package com.furuiduo.quote.auth;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.furuiduo.quote.sys.entity.SysUser;
import com.furuiduo.quote.sys.repository.SysUserRepository;
import com.furuiduo.quote.sys.service.PermissionService;
import com.furuiduo.quote.sys.service.UserAccountService;

@Service
public class AuthService {

  private final TokenStore tokenStore;
  private final SysUserRepository sysUserRepository;
  private final UserAccountService userAccountService;
  private final PermissionService permissionService;

  public AuthService(
      TokenStore tokenStore,
      SysUserRepository sysUserRepository,
      UserAccountService userAccountService,
      PermissionService permissionService) {
    this.tokenStore = tokenStore;
    this.sysUserRepository = sysUserRepository;
    this.userAccountService = userAccountService;
    this.permissionService = permissionService;
  }

  public LoginResponse login(LoginRequest request) {
    if (request.username() == null
        || request.username().isBlank()
        || request.password() == null
        || request.password().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名和密码不能为空");
    }

    SysUser user =
        sysUserRepository
            .findWithDetailsByUsername(request.username())
            .filter(found -> found.getStatus() == 1)
            .filter(found -> userAccountService.matchesPassword(found, request.password()))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "用户名或密码错误"));

    String accessToken = tokenStore.issueToken(user.getUsername());
    return LoginResponse.of(accessToken, user, permissionService);
  }

  public SysUser requireUser(String authorizationHeader) {
    String token = extractBearerToken(authorizationHeader)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录或登录已过期"));

    String username =
        tokenStore
            .resolveUsername(token)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录或登录已过期"));

    return sysUserRepository
        .findWithDetailsByUsername(username)
        .filter(user -> user.getStatus() == 1)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录或登录已过期"));
  }

  public List<String> accessCodes(String authorizationHeader) {
    return permissionService.getPermissionCodes(requireUser(authorizationHeader));
  }

  public void logout(String authorizationHeader) {
    extractBearerToken(authorizationHeader).ifPresent(tokenStore::revoke);
  }

  private java.util.Optional<String> extractBearerToken(String authorizationHeader) {
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      return java.util.Optional.empty();
    }
    String token = authorizationHeader.substring("Bearer ".length()).trim();
    return token.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(token);
  }
}
