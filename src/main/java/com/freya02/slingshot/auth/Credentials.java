package com.freya02.slingshot.auth;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import static com.freya02.slingshot.Main.CREDENTIALS_PATH;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;


/**
 * <p>The clientToken should be a randomly generated identifier and must be identical for each request.<br>
 * The vanilla launcher generates a random (version 4) UUID on first run and saves it, reusing it for every subsequent request.<br>
 * In case it is omitted the server will generate a random token based on Java's UUID.toString() which should then be stored by the client.<br></p>
 * <h2><b>This will however also invalidate all previously acquired accessTokens for this user across all clients.</b></h2>
 * <i><b>Perhaps this mean that different clients can have different access tokens as long as they have the same client token<br>
 * Let's first assume that it does not require client token to be shared</b></i>
 */
public class Credentials {
	private static Credentials instance;

	private String username = "";
	private String uuid = "0";
	private String clientToken = UUID.randomUUID().toString();
	private String accessToken = "0";

	private Credentials() throws IOException {
		if (Files.notExists(CREDENTIALS_PATH)) return;

		final List<String> strings = Files.readAllLines(CREDENTIALS_PATH);
		if (strings.size() == 4) {
			load(strings);
		} else {
			Files.deleteIfExists(CREDENTIALS_PATH);
			System.err.println("WARN : Invalid credentials file, deleting file");
		}
	}

	public static Credentials getInstance() {
		if (instance == null) {
			try {
				instance = new Credentials();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		return instance;
	}

	public boolean isValid() {
		if (!username.isBlank() && !uuid.equals("0") && clientToken.equals("0") && accessToken.equals("0")) {
			return true;
		}

		return !username.isBlank() && !uuid.equals("0") && !clientToken.equals("0") && !accessToken.equals("0");
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getClientToken() {
		return clientToken;
	}

	public void setClientToken(String clientToken) {
		this.clientToken = clientToken;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	private void load(List<String> strings) {
		username = strings.get(0);
		uuid = strings.get(1);
		clientToken = strings.get(2);
		accessToken = strings.get(3);
	}

	public void save() throws IOException {
		Files.writeString(CREDENTIALS_PATH, String.join("\n", username, uuid, clientToken, accessToken), CREATE, TRUNCATE_EXISTING);
	}

	public void reset() throws IOException {
		username = "";
		uuid = "0";
		accessToken = "0";
		//Not resetting client token, maybe that would be useful, maybe

		save();
	}
}
