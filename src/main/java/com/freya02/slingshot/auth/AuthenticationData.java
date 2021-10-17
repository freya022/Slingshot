package com.freya02.slingshot.auth;

import org.jetbrains.annotations.NotNull;

public class AuthenticationData {
	private final String uuid, accessToken, refreshToken;
	private final String username;

	public AuthenticationData(@NotNull String username, @NotNull String uuid, @NotNull String accessToken, @NotNull String refreshToken) {
		this.username = username;
		this.uuid = uuid;
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
	}

	public String getUuid() {
		return uuid;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public String getUsername() {
		return username;
	}
}
