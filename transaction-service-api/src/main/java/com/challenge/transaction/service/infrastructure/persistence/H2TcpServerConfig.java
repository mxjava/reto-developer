package com.challenge.transaction.service.infrastructure.persistence;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.h2.tcp.enabled", havingValue = "true")
public class H2TcpServerConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    Server h2TcpServer(@Value("${app.h2.tcp.port:9092}") int port) throws SQLException {
        return Server.createTcpServer(
                "-tcp", "-tcpPort", Integer.toString(port), "-tcpDaemon", "-ifNotExists");
    }

    @Bean
    DataSource h2TcpDataSource(
            Server h2TcpServer,
            @Value("${app.h2.tcp.port:9092}") int port,
            @Value("${spring.datasource.username:sa}") String username,
            @Value("${spring.datasource.password}") String password) {

        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL(
                "jdbc:h2:tcp://localhost:"
                        + port
                        + "/mem:transactionsdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUser(username);
        dataSource.setPassword(password);
        return dataSource;
    }
}
