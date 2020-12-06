#include <curl/curl.h>
#include <string>
#include <iostream>
#include <sstream>
#include <chrono>
#include "json.h"
#include "base64/base64.h"

size_t writeString(char* ptr, size_t size, size_t nmemb, std::string* data) {
	data->append(ptr, size * nmemb);

	return size * nmemb;
}

/*void downloadSkin(const std::string &url, std::vector<char> &bytes) {
	auto curl = curl_easy_init();
	if (curl) {
		//Standard connection
		const char *urlC = url.c_str();

		curl_easy_setopt(curl, CURLOPT_URL, urlC);
		curl_easy_setopt(curl, CURLOPT_MAXREDIRS, 0L);
		curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, writeString);

		std::string writeData;
		curl_easy_setopt(curl, CURLOPT_WRITEDATA, &writeData);

		curl_easy_perform(curl);

		bytes.insert(bytes.begin(), writeData.begin(), writeData.end());

		//curl_slist_free_all(headers);
		curl_easy_cleanup(curl); //Do this at last, "url" is deleted after this
	}
}*/

int main() {


	/*auto curl = curl_easy_init();
	if (curl) {
		//Standard connection
		const auto &string = std::string("https://sessionserver.mojang.com/session/minecraft/profile/4b9edc26a4a0410f8661c4f6fcab6d12");
		const char *url = string.c_str();

		curl_easy_setopt(curl, CURLOPT_URL, url);
		curl_easy_setopt(curl, CURLOPT_MAXREDIRS, 0L);
		curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, writeString);
		std::string writeData;
		curl_easy_setopt(curl, CURLOPT_WRITEDATA, &writeData);

		//Set the headers
		curl_slist *headers = curl_slist_append(nullptr, "Content-Type: application/json");
		curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);

		curl_easy_perform(curl);

		std::vector<char> bytes;

		if (!writeData.empty()) {
			nlohmann::json j = nlohmann::json::parse(writeData);

			if (j.contains("properties")) {
				for (const auto& item : j["properties"]) {
					if (item.contains("name") && item["name"] == "textures") {
						std::string base64json = item["value"];

						const std::string &decodedJson = base64_decode(base64json, false);

						nlohmann::json j2 = nlohmann::json::parse(decodedJson);
						if (j2.contains("textures") && j2["textures"].contains("SKIN")) {
							if (j2["textures"]["SKIN"].contains("url")) {
								downloadSkin(j2["textures"]["SKIN"]["url"], bytes);
							}
						}

						break;
					}
				}
			}
		}

		curl_slist_free_all(headers);
		curl_easy_cleanup(curl); //Do this at last, "url" is deleted after this

		std::cout << "finished";
	}*/

	return 0;
}
