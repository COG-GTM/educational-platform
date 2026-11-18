package com.educational.platform.users.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards against javax.xml.bind:jaxb-api (SNYK-JAVA-JAVAXXMLBIND-3038042, no fixed version) returning to the classpath.
 */
public class JaxbApiAbsentTest {

    @Test
    void jaxbApi_notOnClasspath() {
        assertThat(getClass().getClassLoader().getResource("javax/xml/bind/DatatypeConverter.class")).isNull();
        assertThat(getClass().getClassLoader().getResource("META-INF/maven/javax.xml.bind/jaxb-api/pom.properties")).isNull();
    }
}
