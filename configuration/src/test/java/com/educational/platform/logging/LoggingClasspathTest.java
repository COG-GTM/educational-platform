package com.educational.platform.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoggingClasspathTest {

    @Test
    void log4jApi_isNotOnClasspath() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("org.apache.logging.log4j.LogManager"));
        assertThrows(ClassNotFoundException.class, () -> Class.forName("org.apache.logging.slf4j.SLF4JLoggerContextFactory"));
    }

    @Test
    void slf4j_isBoundToLogback() {
        assertEquals("ch.qos.logback.classic.LoggerContext", LoggerFactory.getILoggerFactory().getClass().getName());
    }

}
