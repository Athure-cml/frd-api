package com.furuiduo.quote.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "系统", description = "健康检查与连通性测试")
@RestController
public class HelloController {

  @Operation(summary = "健康检查", description = "验证后端服务是否已启动，无需登录。")
  @GetMapping("/hello")
  public Map<String, String> hello() {
    return Map.of("message", "福瑞多报价系统后端已启动");
  }
}
