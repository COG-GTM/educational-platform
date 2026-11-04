package com.educational.platform.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * Guards against CVE-2022-45868 (SNYK-JAVA-COMH2DATABASE-3146851): H2 before 2.2.220 accepts the
 * H2 Console admin password in clear text, and the console must not be exposed by default.
 */
class H2DatabaseTest {

	private static final int[] MINIMUM_VERSION = {2, 2, 220};

	@Test
	void h2OnRuntimeClasspath_isAtLeastMinimumVersion() throws Exception {
		Class<?> constants = Class.forName("org.h2.engine.Constants");
		int[] actual = {
				constants.getField("VERSION_MAJOR").getInt(null),
				constants.getField("VERSION_MINOR").getInt(null),
				constants.getField("BUILD_ID").getInt(null)
		};
		String version = actual[0] + "." + actual[1] + "." + actual[2];

		assertTrue(compare(actual, MINIMUM_VERSION) >= 0,
				"H2 " + version + " is vulnerable to CVE-2022-45868; expected >= 2.2.220");
	}

	@Test
	void h2Console_isDisabledByDefaultAndLocalOnly() throws Exception {
		Properties properties = new Properties();
		try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.properties")) {
			assertNotNull(in, "application.properties not found");
			properties.load(in);
		}

		assertEquals("${SPRING_H2_CONSOLE_ENABLED:false}", properties.getProperty("spring.h2.console.enabled"));
		assertEquals("false", properties.getProperty("spring.h2.console.settings.web-allow-others"));
	}

	private static int compare(int[] left, int[] right) {
		for (int i = 0; i < left.length; i++) {
			if (left[i] != right[i]) {
				return Integer.compare(left[i], right[i]);
			}
		}
		return 0;
	}

}
