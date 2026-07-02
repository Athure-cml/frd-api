package com.furuiduo.quote.quote.seed;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 为已有库补充 quote_order 币种快照列（实体新增 base_currency / exchange_rate 后，H2 ddl-auto
 * 有时不会自动变更已存在的表）。
 */
@Component
@Order(5)
public class QuoteOrderSchemaSeeder implements ApplicationRunner {

  private final JdbcTemplate jdbcTemplate;

  public QuoteOrderSchemaSeeder(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void run(ApplicationArguments args) {
    ensureColumn("quote_order", "base_currency", "VARCHAR(8) NOT NULL DEFAULT 'CNY'");
    ensureColumn("quote_order", "exchange_rate", "DECIMAL(18,8)");
    jdbcTemplate.update(
        "UPDATE quote_order SET base_currency = currency WHERE base_currency IS NULL");
  }

  private void ensureColumn(String table, String column, String definition) {
    if (columnExists(table, column)) {
      return;
    }
    jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
  }

  private boolean columnExists(String table, String column) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
            WHERE UPPER(TABLE_NAME) = UPPER(?)
              AND UPPER(COLUMN_NAME) = UPPER(?)
            """,
            Integer.class,
            table,
            column);
    return count != null && count > 0;
  }
}
