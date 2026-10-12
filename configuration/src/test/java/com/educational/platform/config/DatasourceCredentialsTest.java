package com.educational.platform.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class DatasourceCredentialsTest {

    @Test
    void applicationProperties_mustNotContainLiteralDatasourcePassword() throws IOException {
        final Properties properties = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            properties.load(in);
        }

        assertThat(properties.getProperty("spring.datasource.password"))
                .isEqualTo("${SPRING_DATASOURCE_PASSWORD:}");
    }

    @Nested
    @SpringBootTest(properties = "SPRING_DATASOURCE_PASSWORD=from-environment")
    class WhenPasswordIsProvidedExternally {

        @Autowired
        private Environment environment;

        @Test
        void datasourcePassword_isResolvedFromEnvironment() {
            assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("from-environment");
        }
    }
}
