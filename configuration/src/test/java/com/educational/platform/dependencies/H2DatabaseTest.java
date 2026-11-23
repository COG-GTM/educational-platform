package com.educational.platform.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Guards against CVE-2022-45868 (SNYK-JAVA-COMH2DATABASE-3146851): H2 before 2.2.220 accepts the
 * H2 Console admin password in clear text, and the console must not be exposed by default.
 */
class H2DatabaseTest {

	private static final int[] MINIMUM_VERSION = {2, 2, 220};
	private static final String ADMIN_PASSWORD = "plaintext-admin-pw";

	@Test
	void h2WebServer_rejectsPlainTextAdminPassword() throws Exception {
		// given
		Method createWebServer = Class.forName("org.h2.tools.Server")
				.getMethod("createWebServer", String[].class);

		// when
		InvocationTargetException exception = assertThrows(InvocationTargetException.class,
				() -> createWebServer.invoke(null, (Object) new String[] {
						"-webAdminPassword", ADMIN_PASSWORD, "-webPort", "0"
				}));

		// then
		assertTrue(exception.getCause() instanceof SQLException,
				"Expected SQLException cause but got " + exception.getCause());
		SQLException cause = (SQLException) exception.getCause();
		assertTrue(cause.getMessage().contains("encodeAdminPassword"),
				"Unexpected H2 rejection message: " + cause.getMessage());
	}

	@Test
	void h2WebServer_acceptsHashedAdminPassword() throws Exception {
		// given
		String hash = (String) Class.forName("org.h2.server.web.WebServer")
				.getMethod("encodeAdminPassword", String.class).invoke(null, ADMIN_PASSWORD);
		Object server = null;

		try {
			// when
			server = Class.forName("org.h2.tools.Server").getMethod("createWebServer", String[].class)
					.invoke(null, (Object) new String[] {"-webPort", "0", "-webAdminPassword", hash});

			// then
			assertNotNull(server);
		} finally {
			if (server != null) {
				server.getClass().getMethod("stop").invoke(server);
			}
		}
	}

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
	void h2OnRuntimeClasspath_matchesVersionPinnedInVersionCatalog() throws Exception {
		// given
		Path versionCatalog = Path.of("..", "gradle", "libs.versions.toml");
		assertTrue(Files.exists(versionCatalog), "Version catalog not found at " + versionCatalog.toAbsolutePath());
		Matcher matcher = Pattern.compile("^h2\\s*=\\s*\"([^\"]+)\"", Pattern.MULTILINE)
				.matcher(Files.readString(versionCatalog));
		assertTrue(matcher.find(), "H2 version entry not found in " + versionCatalog.toAbsolutePath());
		String expectedVersion = matcher.group(1);

		// when
		try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:h2VersionCheck");
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT H2VERSION()")) {
			// then
			assertTrue(resultSet.next(), "H2VERSION() did not return a row");
			assertEquals(expectedVersion, resultSet.getString(1));
		}
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
