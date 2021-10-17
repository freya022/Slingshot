package com.freya02.slingshot.auth;

import com.freya02.io.IOOperation;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.freya02.slingshot.auth.AuthKeys.*;

public class MSAuth extends IOOperation {
	private static final OkHttpClient CLIENT = new OkHttpClient.Builder().build();

	private final ExecutorService serverExecutor = Executors.newSingleThreadExecutor();
	private final HttpServer server;

	public MSAuth() throws IOException {
		server = HttpServer.create(new InetSocketAddress(25566), 0);
	}

	public void doFullLogin() throws IOException, URISyntaxException {
		server.createContext("/auth-response", exchange -> {
			final String response;
			boolean canClose = false;

			final String query = exchange.getRequestURI().getQuery();
			if (query.startsWith("code=")) {
				canClose = true;
				response = "<h1>You can close this page</h1><script>window.close()</script>";

				final String code = query.substring(5);

				exchange.sendResponseHeaders(200, response.length());

				new Thread(() -> {
					AuthenticationData authenticationData = null;

					try {
						authenticationData = doFullLogin(code);
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						AuthenticationData finalAuthenticationData = authenticationData;

						Platform.runLater(() -> Platform.exitNestedEventLoop(this, finalAuthenticationData));
					}
				}).start();
			} else {
				response = "<h1>Invalid query</h1>";

				exchange.sendResponseHeaders(400, response.length());
			}

			final OutputStream outputStream = exchange.getResponseBody();
			outputStream.write(response.getBytes(StandardCharsets.UTF_8));
			outputStream.flush();
			outputStream.close();

			if (canClose) {
				server.stop(0);
				serverExecutor.shutdown();
			}
		});

		server.setExecutor(serverExecutor);
		server.start();

		Desktop.getDesktop().browse(new URL(getLoginUrl()).toURI());

		setState("Waiting for authentication");
	}

	private AuthenticationData doFullLogin(String authCode) throws IOException {
		setState("Getting MS token");

		final HttpResponse tokenRequest = getAuthorizationToken(authCode);
		final String token = tokenRequest.getValue("access_token");

		return doCommonLogin(tokenRequest, token);
	}

	public AuthenticationData doRefreshLogin(String refreshToken) throws IOException {
		setState("Refreshing MS token");

		final HttpResponse tokenRequest = refreshAuthorizationToken(refreshToken);
		final String token = tokenRequest.getValue("access_token");

		return doCommonLogin(tokenRequest, token);
	}

	@NotNull
	private AuthenticationData doCommonLogin(HttpResponse tokenRequest, String token) throws IOException {
		setState("Getting Xbox Live token");

		final HttpResponse xblRequest = authenticateWithXbl(token);
		final String xblToken = xblRequest.getValue("Token");
		final String userHash = xblRequest.getValue("DisplayClaims", "xui", 0, "uhs");

		setState("Getting XSTS token");

		final HttpResponse xstsRequest = authenticateWithXsts(xblToken);
		final String xstsToken = xstsRequest.getValue("Token");

		setState("Getting Minecraft account");
		final HttpResponse accountRequest = authenticateWithMinecraft(userHash, xstsToken);
		final String accessToken = accountRequest.getValue("access_token");

		setState("Getting Minecraft profile");
		final String mcAccessToken = accountRequest.getValue("access_token");
		final String mcRefreshToken = tokenRequest.getValue("refresh_token");
		final String mcUsername = getProfile(accessToken).getValue("name");
		final String mcUuid = getProfile(accessToken).getValue("id");

		return new AuthenticationData(mcUsername, mcUuid, mcAccessToken, mcRefreshToken);
	}

	private String getLoginUrl() {
		return "https://login.live.com/oauth20_authorize.srf?client_id=" + CLIENT_ID + "&response_type=code&redirect_uri=" + REDIRECT_URI + "&scope=XboxLive.signin%20offline_access";
	}

	private HttpResponse getAuthorizationToken(String code) throws IOException {
		return new HttpResponse(CLIENT.newCall(
				new Request.Builder()
						.url("https://login.live.com/oauth20_token.srf")
						.post(
								new FormBody.Builder()
										.add("client_id", CLIENT_ID)
										.add("client_secret", CLIENT_SECRET)
										.add("redirect_uri", REDIRECT_URI)
										.add("code", code)
										.add("grant_type", "authorization_code")
										.build()
						)
						.header("Content-Type", "application/x-www-form-urlencoded")
						.header("user-agent", USER_AGENT)
						.build()
		).execute());
	}

	private HttpResponse authenticateWithXbl(String token) throws IOException {
		final Map<String, ?> map = Map.of(
				"Properties", Map.of(
						"AuthMethod", "RPS",
						"SiteName", "user.auth.xboxlive.com",
						"RpsTicket", "d=" + token
				),
				"RelyingParty", "http://auth.xboxlive.com",
				"TokenType", "JWT"
		);

		final String json = new Gson().toJson(map);

		return new HttpResponse(CLIENT.newCall(
				new Request.Builder()
						.url("https://user.auth.xboxlive.com/user/authenticate")
						.post(
								RequestBody.create(MediaType.get("application/json"), json)
						)
						.header("Content-Type", "application/json")
						.header("Accept", "application/json")
						.header("user-agent", USER_AGENT)
						.build()
		).execute());
	}

	private HttpResponse authenticateWithXsts(String xblToken) throws IOException {
		final Map<String, ?> map = Map.of(
				"Properties", Map.of(
						"SandboxId", "RETAIL",
						"UserTokens", List.of(xblToken)
				),
				"RelyingParty", "rp://api.minecraftservices.com/",
				"TokenType", "JWT"
		);

		final String json = new Gson().toJson(map);

		return new HttpResponse(CLIENT.newCall(
				new Request.Builder()
						.url("https://xsts.auth.xboxlive.com/xsts/authorize")
						.post(
								RequestBody.create(MediaType.get("application/json"), json)
						)
						.header("Content-Type", "application/json")
						.header("Accept", "application/json")
						.header("user-agent", USER_AGENT)
						.build()
		).execute());
	}

	private HttpResponse authenticateWithMinecraft(String userHash, String xstsToken) throws IOException {
		final Map<String, ?> map = Map.of(
				"identityToken", String.format("XBL3.0 x=%s;%s", userHash, xstsToken)
		);

		final String json = new Gson().toJson(map);

		return new HttpResponse(CLIENT.newCall(
				new Request.Builder()
						.url("https://api.minecraftservices.com/authentication/login_with_xbox")
						.post(
								RequestBody.create(MediaType.get("application/json"), json)
						)
						.header("Content-Type", "application/json")
						.header("Accept", "application/json")
						.header("user-agent", USER_AGENT)
						.build()
		).execute());
	}

	private HttpResponse getProfile(String accessToken) throws IOException {
		return new HttpResponse(CLIENT.newCall(
				new Request.Builder()
						.url("https://api.minecraftservices.com/minecraft/profile")
						.get()
						.header("Authorization", "Bearer " + accessToken)
						.header("user-agent", USER_AGENT)
						.build()
		).execute());
	}

	private HttpResponse refreshAuthorizationToken(String refreshToken) throws IOException {
		return new HttpResponse(CLIENT.newCall(
				new Request.Builder()
						.url("https://login.live.com/oauth20_token.srf")
						.post(
								new FormBody.Builder()
										.add("client_id", CLIENT_ID)
										.add("client_secret", CLIENT_SECRET)
										.add("redirect_uri", REDIRECT_URI)
										.add("refresh_token", refreshToken)
										.add("grant_type", "refresh_token")
										.build()
						)
						.header("user-agent", USER_AGENT)
						.build()
		).execute());
	}

	public void cancel() {
		server.stop(0);
		serverExecutor.shutdown();
	}

	private static class HttpResponse {
		private static final Gson GSON = new Gson();

		private final Map<String, ?> map;

		@SuppressWarnings("unchecked")
		private HttpResponse(Response response) throws IOException {
			try (Response ignored = response) {
				final ResponseBody body = response.body();
				if (body != null) {
					this.map = (Map<String, ?>) GSON.fromJson(body.string(), Map.class);
				} else {
					this.map = null;
				}
			}
		}

		@SuppressWarnings("unchecked")
		public String getValue(Object... keys) {
			Object value = map;

			for (Object key : keys) {
				if (key instanceof Number) {
					final List<?> list = (List<?>) value;
					final int index = ((Number) key).intValue();

					if (index >= list.size()) throw new NoSuchElementException("No element at index " + index);

					value = list.get(index);
				} else {
					value = ((Map<String, ?>) value).get(key.toString());

					if (value == null) throw new NoSuchElementException("No element at key " + key);
				}
			}

			return (String) value;
		}
	}
}