package com.educational.platform.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Represents centralized JWT token settings shared across modules.
 * Supports an optional previous secret key so tokens signed with the old key
 * remain valid during a rotation window.
 *
 * Values are read once at startup, so key rotation requires a rolling restart:
 * deploy with the new secret key and the old one as previous secret key, then
 * remove the previous key after all old tokens have expired.
 */
@Component
@ConfigurationProperties(prefix = "security.jwt.token")
public class JwtTokenProperties {

	/**
	 * Token validity duration in milliseconds.
	 */
	private long expireLength = 3600000;

	/**
	 * Secret key used to sign new tokens and validate incoming tokens.
	 */
	private String secretKey = "secret-key";

	/**
	 * Optional previous secret key accepted for validation during a rotation window.
	 */
	private String previousSecretKey;

	public long getExpireLength() {
		return expireLength;
	}

	public void setExpireLength(long expireLength) {
		this.expireLength = expireLength;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getPreviousSecretKey() {
		return previousSecretKey;
	}

	public void setPreviousSecretKey(String previousSecretKey) {
		this.previousSecretKey = previousSecretKey;
	}
}
