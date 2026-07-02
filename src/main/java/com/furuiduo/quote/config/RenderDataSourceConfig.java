package com.furuiduo.quote.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import com.zaxxer.hikari.HikariDataSource;

/**
 * Render 关联 PostgreSQL 后会注入 {@code DATABASE_URL}，或分别注入 {@code PGHOST} 等变量。
 */
@Configuration
@Profile("render")
public class RenderDataSourceConfig {

  @Bean
  @Primary
  public DataSource renderDataSource(Environment env) {
    String databaseUrl = env.getProperty("DATABASE_URL");
    if (databaseUrl != null && !databaseUrl.isBlank()) {
      return buildFromDatabaseUrl(databaseUrl.trim());
    }
    return buildFromPgEnv(env);
  }

  private DataSource buildFromDatabaseUrl(String databaseUrl) {
    try {
      URI dbUri = new URI(databaseUrl.replaceFirst("^postgresql:", "postgres:"));

      String username = null;
      String password = "";
      if (dbUri.getUserInfo() != null) {
        String[] userParts = dbUri.getUserInfo().split(":", 2);
        username = URLDecoder.decode(userParts[0], StandardCharsets.UTF_8);
        if (userParts.length > 1) {
          password = URLDecoder.decode(userParts[1], StandardCharsets.UTF_8);
        }
      }

      int port = dbUri.getPort() > 0 ? dbUri.getPort() : 5432;
      String jdbcUrl =
          String.format(
              "jdbc:postgresql://%s:%d%s?sslmode=require",
              dbUri.getHost(), port, dbUri.getPath());

      HikariDataSource dataSource = new HikariDataSource();
      dataSource.setJdbcUrl(jdbcUrl);
      dataSource.setUsername(username);
      dataSource.setPassword(password);
      return dataSource;
    } catch (Exception ex) {
      throw new IllegalStateException("无法解析 DATABASE_URL，请检查 Render 数据库关联配置", ex);
    }
  }

  private DataSource buildFromPgEnv(Environment env) {
    String host = env.getProperty("PGHOST");
    String database = env.getProperty("PGDATABASE");
    if (host == null || host.isBlank() || database == null || database.isBlank()) {
      throw new IllegalStateException(
          "未配置数据库：请在 Render 创建 PostgreSQL，并在 frd-api 服务中 Connect 该数据库");
    }

    String port = env.getProperty("PGPORT", "5432");
    String jdbcUrl =
        String.format("jdbc:postgresql://%s:%s/%s?sslmode=require", host, port, database);

    HikariDataSource dataSource = new HikariDataSource();
    dataSource.setJdbcUrl(jdbcUrl);
    dataSource.setUsername(env.getProperty("PGUSER"));
    dataSource.setPassword(env.getProperty("PGPASSWORD", ""));
    return dataSource;
  }
}
