package org.example.finishedbackend.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseBootstrapConfiguration {

    @Bean
    public ApplicationRunner ensureRuntimeTables(JdbcTemplate jdbcTemplate) {
        return args -> {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS db_sensitive_word (
                        id INT PRIMARY KEY AUTO_INCREMENT,
                        word VARCHAR(50) NOT NULL UNIQUE COMMENT '敏感词（唯一）',
                        type VARCHAR(20) NOT NULL DEFAULT 'sensitive' COMMENT '词类型: sensitive/forbidden',
                        INDEX idx_word (word)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);

            ensureColumn(jdbcTemplate, "db_sensitive_word", "type",
                    "ALTER TABLE db_sensitive_word ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'sensitive' COMMENT '词类型: sensitive/forbidden'");
            ensureColumn(jdbcTemplate, "db_topic_comment", "like_count",
                    "ALTER TABLE db_topic_comment ADD COLUMN like_count INT NOT NULL DEFAULT 0");
            ensureColumn(jdbcTemplate, "db_topic_comment", "hot_score",
                    "ALTER TABLE db_topic_comment ADD COLUMN hot_score DOUBLE NOT NULL DEFAULT 0");
            ensureColumn(jdbcTemplate, "db_account_details", "cover",
                    "ALTER TABLE db_account_details ADD COLUMN cover VARCHAR(255) DEFAULT NULL");
        };
    }

    private void ensureColumn(JdbcTemplate jdbcTemplate, String tableName, String columnName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, tableName, columnName);
        if (count != null && count == 0) {
            jdbcTemplate.execute(ddl);
        }
    }
}
