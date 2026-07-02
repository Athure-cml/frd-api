package com.furuiduo.quote.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Render 无 SQL 控制台时：在 frd-api 环境变量设置 {@code QUOTE_DB_RESET=true}，部署一次即可清库并
 * 由 Flyway 重建表结构。成功后务必删除该变量并再次部署，避免每次启动都清库。
 */
@Configuration
@Profile("render")
@ConditionalOnProperty(name = "quote.db.reset", havingValue = "true")
public class DatabaseResetFlywayConfig {

  private static final Logger log = LoggerFactory.getLogger(DatabaseResetFlywayConfig.class);

  @Bean
  public FlywayMigrationStrategy resetAndMigrateStrategy(DataSource dataSource) {
    return flyway -> {
      log.warn("QUOTE_DB_RESET=true：正在清空 public schema 并重新执行 Flyway 迁移");
      resetSchema(dataSource);
      flyway.migrate();
      log.warn("数据库重建完成。请立即在 Render 删除环境变量 QUOTE_DB_RESET 并重新部署");
    };
  }

  private void resetSchema(DataSource dataSource) {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute("DROP SCHEMA IF EXISTS public CASCADE");
      statement.execute("CREATE SCHEMA public");
      statement.execute("GRANT ALL ON SCHEMA public TO PUBLIC");
      statement.execute("GRANT ALL ON SCHEMA public TO CURRENT_USER");
    } catch (SQLException ex) {
      throw new IllegalStateException("清空数据库失败，请检查 Postgres 连接与权限", ex);
    }
  }
}
