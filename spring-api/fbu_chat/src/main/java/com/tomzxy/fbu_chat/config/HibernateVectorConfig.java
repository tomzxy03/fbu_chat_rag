package com.tomzxy.fbu_chat.config;

import com.pgvector.PGvector;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Cấu hình DataSource với pgvector type registration.
 *
 * PGvector.addVectorType(connection) phải được gọi trên mỗi Connection
 * trước khi Hibernate dùng nó để đọc/ghi cột VECTOR.
 * Hikari hỗ trợ điều này qua connectionInitSql, nhưng addVectorType()
 * là Java-side call nên cần wrap DataSource.
 */
@Configuration
public class HibernateVectorConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.hikari.maximum-pool-size:10}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:2}")
    private int minIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connectionTimeout);

        // Đăng ký pgvector Java type với mỗi connection mới từ pool
        config.setConnectionInitSql("SELECT 1"); // warm-up
        config.addDataSourceProperty("ApplicationName", "fbu_chat");

        return new PGvectorAwareHikariDataSource(config);
    }

    /**
     * Wrap HikariDataSource để gọi PGvector.addVectorType() trên mỗi connection.
     */
    static class PGvectorAwareHikariDataSource extends HikariDataSource {

        PGvectorAwareHikariDataSource(HikariConfig config) {
            super(config);
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection conn = super.getConnection();
            PGvector.addVectorType(conn);
            return conn;
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            Connection conn = super.getConnection(username, password);
            PGvector.addVectorType(conn);
            return conn;
        }
    }
}
