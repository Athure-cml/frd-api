package com.furuiduo.quote.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

  public static final String BEARER_SCHEME = "bearerAuth";

  @Bean
  public OpenAPI quoteOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("福瑞多报价系统 API")
                .description(
                    """
                    福瑞多报价系统后端接口文档。

                    ## 统一响应格式
                    所有业务接口返回 `ApiResponse`：
                    - `code`: `0` 表示成功，`-1` 表示失败
                    - `data`: 业务数据
                    - `message`: 提示信息
                    - `error`: 错误详情

                    ## 认证方式
                    1. 调用 `POST /auth/login` 获取 `accessToken`
                    2. 在 Swagger 右上角 **Authorize** 填入：`Bearer {accessToken}`
                    3. 或在请求头添加：`Authorization: Bearer {accessToken}`
                    """)
                .version("v0.1.0")
                .contact(new Contact().name("福瑞多报价系统").email("")))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_SCHEME,
                    new SecurityScheme()
                        .name(BEARER_SCHEME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("Token")
                        .description("登录接口返回的 accessToken，格式：Bearer {token}")));
  }
}
