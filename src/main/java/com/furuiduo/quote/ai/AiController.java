package com.furuiduo.quote.ai;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.furuiduo.quote.ai.dto.AiChatRequest;
import com.furuiduo.quote.ai.dto.AiChatResponse;
import com.furuiduo.quote.ai.dto.AiParseRequest;
import com.furuiduo.quote.ai.dto.AiParseResponse;
import com.furuiduo.quote.auth.AuthService;
import com.furuiduo.quote.common.ApiResponse;
import com.furuiduo.quote.config.OpenApiConfig;
import com.furuiduo.quote.sys.PermissionCodes;
import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.service.PermissionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "AI 助手", description = "通义千问代理：系统问答、成本查询、文档解析")
@RestController
@RequestMapping("/ai")
public class AiController {

  private final AuthService authService;
  private final PermissionService permissionService;
  private final AiChatService aiChatService;
  private final AiParseService aiParseService;
  private final AiRateLimiter aiRateLimiter;

  public AiController(
      AuthService authService,
      PermissionService permissionService,
      AiChatService aiChatService,
      AiParseService aiParseService,
      AiRateLimiter aiRateLimiter) {
    this.authService = authService;
    this.permissionService = permissionService;
    this.aiChatService = aiChatService;
    this.aiParseService = aiParseService;
    this.aiRateLimiter = aiRateLimiter;
  }

  @Operation(
      summary = "AI 对话（非流式）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/chat")
  public ApiResponse<AiChatResponse> chat(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody AiChatRequest request) {
    SysUser user = requireAiUser(authorization);
    return ApiResponse.ok(aiChatService.chat(user, request));
  }

  @Operation(
      summary = "AI 对话（SSE，完成后推送完整回复）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter chatStream(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody AiChatRequest request) {
    SysUser user = requireAiUser(authorization);
    SseEmitter emitter = new SseEmitter(120_000L);
    Thread.startVirtualThread(
        () -> {
          try {
            AiChatResponse response = aiChatService.chat(user, request);
            emitter.send(SseEmitter.event().name("message").data(response));
            emitter.send(SseEmitter.event().name("done").data(Map.of("ok", true)));
            emitter.complete();
          } catch (ResponseStatusException ex) {
            try {
              emitter.send(
                  SseEmitter.event()
                      .name("error")
                      .data(Map.of("message", ex.getReason() == null ? "error" : ex.getReason())));
            } catch (IOException ignored) {
              // ignore
            }
            emitter.completeWithError(ex);
          } catch (Exception ex) {
            try {
              emitter.send(
                  SseEmitter.event()
                      .name("error")
                      .data(Map.of("message", ex.getMessage() == null ? "error" : ex.getMessage())));
            } catch (IOException ignored) {
              // ignore
            }
            emitter.completeWithError(ex);
          }
        });
    return emitter;
  }

  @Operation(
      summary = "解析粘贴文本/邮件",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping("/parse")
  public ApiResponse<AiParseResponse> parseText(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody AiParseRequest request) {
    SysUser user = requireAiUser(authorization);
    return ApiResponse.ok(aiParseService.parseText(request));
  }

  @Operation(
      summary = "解析上传文件（Excel / PDF / 文本）",
      security = @SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME))
  @PostMapping(value = "/parse/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<AiParseResponse> parseFile(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam("file") MultipartFile file,
      @RequestParam(value = "hint", required = false) String hint) {
    SysUser user = requireAiUser(authorization);
    return ApiResponse.ok(aiParseService.parseFile(file, hint));
  }

  private SysUser requireAiUser(String authorization) {
    SysUser user = authService.requireUser(authorization);
    if (!permissionService.hasPermission(user, PermissionCodes.AI_USE)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden");
    }
    aiRateLimiter.check(user.getId());
    return user;
  }
}
