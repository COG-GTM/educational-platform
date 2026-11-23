package com.educational.platform.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.PropertiesPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;

class H2ConsolePropertiesTest {

	@Test
	void h2Console_disabledWhenEnvVarAbsent() throws IOException {
		// given
		StandardEnvironment environment = environmentWith(Map.of());

		// when
		Boolean enabled = environment.getProperty("spring.h2.console.enabled", Boolean.class);

		// then
		assertEquals(Boolean.FALSE, enabled);
	}

	@Test
	void h2Console_enabledWhenEnvVarTrue() throws IOException {
		// given
		StandardEnvironment environment = environmentWith(Map.of("SPRING_H2_CONSOLE_ENABLED", "true"));

		// when
		Boolean enabled = environment.getProperty("spring.h2.console.enabled", Boolean.class);

		// then
		assertEquals(Boolean.TRUE, enabled);
	}

	@Test
	void h2Console_disabledWhenEnvVarFalse() throws IOException {
		// given
		StandardEnvironment environment = environmentWith(Map.of("SPRING_H2_CONSOLE_ENABLED", "false"));

		// when
		Boolean enabled = environment.getProperty("spring.h2.console.enabled", Boolean.class);

		// then
		assertEquals(Boolean.FALSE, enabled);
	}

	@Test
	void h2Console_remoteAccessDisabledEvenWhenConsoleEnabled() throws IOException {
		// given
		StandardEnvironment environment = environmentWith(Map.of("SPRING_H2_CONSOLE_ENABLED", "true"));

		// when
		Boolean remoteAccessAllowed = environment.getProperty(
				"spring.h2.console.settings.web-allow-others", Boolean.class);

		// then
		assertEquals(Boolean.FALSE, remoteAccessAllowed);
	}

	private StandardEnvironment environmentWith(Map<String, Object> environmentVariables) throws IOException {
		StandardEnvironment environment = new StandardEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		propertySources.addFirst(new SystemEnvironmentPropertySource("testSystemEnvironment", environmentVariables));

		List<PropertySource<?>> applicationProperties = new PropertiesPropertySourceLoader()
				.load("applicationConfig", new ClassPathResource("application.properties"));
		applicationProperties.forEach(propertySources::addLast);

		return environment;
	}
}
