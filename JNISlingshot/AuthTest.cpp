#include <iostream>
#include <fmt/core.h>
#include <cpr/cpr.h>
#include "key.h"
#include "json.h"

void split(std::vector<std::string_view> vec, std::string_view s, std::string_view delim) {
	auto start = 0U;
	auto end = s.find(delim);
	while (end != std::string::npos) {
		vec.push_back(s.substr(start, end - start));
		start = end + delim.length();
		end = s.find(delim, start);
	}

	vec.push_back(s.substr(start, end));
}

std::string getLoginUrl(std::string_view clientId, std::string_view redirectUri) {
	return fmt::format(
			"https://login.live.com/oauth20_authorize.srf?client_id={}&response_type=code&redirect_uri={}&scope=XboxLive.signin%20offline_access",
			clientId, redirectUri);
}

bool containsAuthCode(std::string_view url) {
	return url.find("code=") != std::string::npos;
}

std::string_view getAuthCodeFromUrl(std::string_view url) {
	auto interroIndex = url.find('?');

	return url
			.substr(interroIndex)
			.substr(6);
}

auto getAuthorizationToken(const std::string &clientId, const std::string &clientSecret, const std::string &redirectUri,
						   const std::string &authCode) -> decltype(auto) {
	return cpr::Post(
			cpr::Url("https://login.live.com/oauth20_token.srf"),
			cpr::Header{
					{"Content-Type", "application/x-www-form-urlencoded"},
					{"user-agent", USER_AGENT}
			},
			cpr::Payload({
								 {"client_id",     clientId},
								 {"client_secret", clientSecret},
								 {"redirect_uri",  redirectUri},
								 {"code",          authCode},
								 {"grant_type",    "authorization_code"}
						 })
	);
}

auto authenticateWithXbl(const std::string &accessToken) -> decltype(auto) {
	nlohmann::json j;

	j["Properties"]["AuthMethod"] = "RPS";
	j["Properties"]["SiteName"] = "user.auth.xboxlive.com";
	j["Properties"]["RpsTicket"] = fmt::format("d={}", accessToken);
	j["RelyingParty"] = "http://auth.xboxlive.com";
	j["TokenType"] = "JWT";

	return cpr::Post(
			cpr::Url("https://user.auth.xboxlive.com/user/authenticate"),
			cpr::Header{
					{"Content-Type", "application/json"},
					{"user-agent", USER_AGENT},
					{"Accept",       "application/json"}
			},
			cpr::Body(j.dump())
	);
}

int main() {
	std::cout << "Please login in " << getLoginUrl(CLIENT_ID, REDIRECT_URI) << std::endl;

	std::string login;
	std::getline(std::cin, login);

	std::cout << containsAuthCode(login) << std::endl;
	const auto &authCode = getAuthCodeFromUrl(login);
	std::cout << authCode << std::endl;

	const auto &mutCode = std::string(authCode);
	const auto &tokenRequest = getAuthorizationToken(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, mutCode);

	auto tokenJson = nlohmann::json::parse(tokenRequest.text);
	auto token = tokenJson["access_token"].get<std::string>();

	const auto &xblRequest = authenticateWithXbl(token);

	std::cout << xblRequest.text << std::endl;

	return 0;
}