package com.furuiduo.quote.auth;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class TokenStore {

  private final Map<String, String> tokenToUsername = new ConcurrentHashMap<>();

  public String issueToken(String username) {
    String token = UUID.randomUUID().toString().replace("-", "");
    tokenToUsername.put(token, username);
    return token;
  }

  public Optional<String> resolveUsername(String token) {
    return Optional.ofNullable(tokenToUsername.get(token));
  }

  public void revoke(String token) {
    tokenToUsername.remove(token);
  }
}
