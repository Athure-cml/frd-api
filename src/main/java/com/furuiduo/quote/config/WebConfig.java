package com.furuiduo.quote.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final String uploadDir;
  private final String[] allowedOriginPatterns;

  public WebConfig(
      @Value("${quote.upload-dir:./uploads}") String uploadDir,
      @Value("${quote.cors.allowed-origin-patterns:http://localhost:*}") String allowedOriginPatterns) {
    this.uploadDir = uploadDir;
    this.allowedOriginPatterns =
        Arrays.stream(allowedOriginPatterns.split(","))
            .map(String::trim)
            .filter(pattern -> !pattern.isEmpty())
            .toArray(String[]::new);
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String location =
        Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
    registry.addResourceHandler("/uploads/**").addResourceLocations(location + "/");
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/**")
        .allowedOriginPatterns(allowedOriginPatterns)
        .allowedMethods("*")
        .allowedHeaders("*")
        .allowCredentials(true);
  }
}
