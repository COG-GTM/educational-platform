package com.educational.platform.dependency;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.data.util.Version;

/**
 * spring-data-commons below 4.0.6 is affected by CVE-2026-41711 (DoS).
 */
public class SpringDataCommonsVersionTest {

	private static final Version MINIMUM_PATCHED = Version.parse("4.0.6");

	@Test
	void springDataCommons_shouldBe_patchedForCve202641711() {
		final String resolved = Version.class.getPackage().getImplementationVersion();

		assertNotNull(resolved, "spring-data-commons jar has no Implementation-Version");
		assertTrue(Version.parse(resolved).isGreaterThanOrEqualTo(MINIMUM_PATCHED),
				"spring-data-commons " + resolved + " is vulnerable to CVE-2026-41711; expected >= " + MINIMUM_PATCHED);
	}

}
