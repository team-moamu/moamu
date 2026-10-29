package com.moamu;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
@Import(BackendApplicationTests.ContainerConfiguration.class)
public class BackendApplicationTests {

    @Test
    void contextLoads() {
    }

    // 도전 도메인 테스트에서도 가져올 수 있도록 설정 클래스와 바깥 클래스를 공개합니다.
    @TestConfiguration(proxyBeanMethods = false)
    public static class ContainerConfiguration {

        @Bean
        @ServiceConnection
        MySQLContainer<?> mysqlContainer(@Value("${test.mysql.image}") String image) {
            if (image.isBlank()) {
                throw new IllegalArgumentException("TEST_MYSQL_IMAGE must specify a MySQL image with a tag.");
            }
            return new MySQLContainer<>(DockerImageName.parse(image));
        }
    }

}
