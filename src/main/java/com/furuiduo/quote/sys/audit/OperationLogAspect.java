package com.furuiduo.quote.sys.audit;

import java.lang.reflect.Method;
import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.furuiduo.quote.auth.AuthService;
import com.furuiduo.quote.sys.entity.OperationAction;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.OperationLogService;
import com.furuiduo.quote.sys.support.OperationLogSupport;

import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
public class OperationLogAspect {

  private final AuthService authService;
  private final OperationLogService operationLogService;
  private final OperationLogSupport operationLogSupport;

  public OperationLogAspect(
      AuthService authService,
      OperationLogService operationLogService,
      OperationLogSupport operationLogSupport) {
    this.authService = authService;
    this.operationLogService = operationLogService;
    this.operationLogSupport = operationLogSupport;
  }

  @Around(
      "within(com.furuiduo.quote..controller..*)"
          + " && !within(com.furuiduo.quote.auth..*)"
          + " && !within(com.furuiduo.quote.menu..*)"
          + " && !within(com.furuiduo.quote.controller..*)"
          + " && !within(com.furuiduo.quote.user..*)"
          + " && (@annotation(org.springframework.web.bind.annotation.PostMapping)"
          + " || @annotation(org.springframework.web.bind.annotation.PutMapping)"
          + " || @annotation(org.springframework.web.bind.annotation.PatchMapping)"
          + " || @annotation(org.springframework.web.bind.annotation.DeleteMapping))")
  public Object aroundWriteOperation(ProceedingJoinPoint joinPoint) throws Throwable {
    HttpServletRequest request = currentRequest().orElse(null);
    if (request == null) {
      return joinPoint.proceed();
    }

    String requestUri = request.getRequestURI();
    if (requestUri.startsWith("/sys/operation-logs")) {
      return joinPoint.proceed();
    }

    String authorization = request.getHeader("Authorization");
    SysUser user = resolveUser(authorization);
    if (user == null) {
      return joinPoint.proceed();
    }

    String httpMethod = request.getMethod();
    OperationAction action = operationLogSupport.resolveAction(httpMethod, requestUri);
    String module = operationLogSupport.resolveModule(requestUri);
    String resourceType =
        operationLogSupport.resolveResourceType(joinPoint.getTarget().getClass().getSimpleName());
    String resourceId = operationLogSupport.resolveResourceId(requestUri);
    Object requestBodyObj = findRequestBody(joinPoint);
    String requestBody = operationLogSupport.serializeBody(requestBodyObj);
    String ipAddress = resolveClientIp(request);

    try {
      Object result = joinPoint.proceed();
      String summary =
          operationLogSupport.buildSummary(
              action, module, resourceId, requestBodyObj, result, requestUri);
      operationLogService.recordSuccess(
          user,
          module,
          action,
          resourceType,
          resourceId,
          summary,
          httpMethod,
          requestUri,
          requestBody,
          ipAddress);
      return result;
    } catch (Throwable ex) {
      String summary =
          operationLogSupport.buildSummary(
              action, module, resourceId, requestBodyObj, null, requestUri);
      operationLogService.recordFailure(
          user,
          module,
          action,
          resourceType,
          resourceId,
          summary,
          httpMethod,
          requestUri,
          requestBody,
          ipAddress,
          ex.getMessage());
      throw ex;
    }
  }

  private SysUser resolveUser(String authorization) {
    try {
      return authService.requireUser(authorization);
    } catch (RuntimeException ex) {
      return null;
    }
  }

  private Optional<HttpServletRequest> currentRequest() {
    ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes == null) {
      return Optional.empty();
    }
    return Optional.of(attributes.getRequest());
  }

  private Object findRequestBody(ProceedingJoinPoint joinPoint) {
    MethodSignature signature = (MethodSignature) joinPoint.getSignature();
    Method method = signature.getMethod();
    Object[] args = joinPoint.getArgs();
    var parameters = method.getParameters();
    for (int i = 0; i < parameters.length; i++) {
      if (parameters[i].isAnnotationPresent(RequestBody.class) && i < args.length) {
        return args[i];
      }
      if (parameters[i].isAnnotationPresent(RequestHeader.class) && i < args.length) {
        continue;
      }
    }
    return null;
  }

  private String resolveClientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
