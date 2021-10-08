#include <string>
#include <iostream>
#include <sstream>
#include <chrono>
#include "json.h"
#include "base64/base64.h"
#include "library.h"
#include <cpr/cpr.h>
#include <fmt/core.h>
#include "bswapintrin.h"

void skinTest() {
	try {
		cpr::Response resp = cpr::Get(
				cpr::Url("https://sessionserver.mojang.com/session/minecraft/profile/4b9edc26a4a0410f8661c4f6fcab6d12"),
				cpr::Header{{"Content-Type", "application/json"}}
		);

		if (resp.status_code != 200) {
			throw std::runtime_error(fmt::format("Status code: {}", resp.status_code));
		}

		nlohmann::json j = nlohmann::json::parse(resp.text);
		fmt::print("name: {}\n", j["name"]);
		const std::string& skinjson = base64_decode(j["properties"][0]["value"].get<std::string>(), false);

		nlohmann::json j2 = nlohmann::json::parse(skinjson);
		const std::string& url = j2["textures"]["SKIN"]["url"];
		fmt::print("skin url: {}\n", url);

		std::vector<char> skinBytes;
		skinBytes.reserve(10240);
		auto skinResp = cpr::Download(cpr::WriteCallback([&skinBytes](std::string_view str) {
			skinBytes.insert(skinBytes.end(), str.begin(), str.end());

			return true;
		}), cpr::Url(url));

		if (skinResp.status_code != 200) {
			throw std::runtime_error(fmt::format("Mojang session server status code: {}", skinResp.status_code));
		}

		fmt::print("");
	} catch (const std::exception& e) {
		fmt::print("Exception: {}", e.what());
	}
}

void downloadTest() {
	try {
		std::string path = "/JreCheckList.fchecks";
		std::ofstream output("lmao.txt");
		cpr::Response resp = cpr::Get(
				cpr::Url("https://content.dropboxapi.com/2/files/download"),
				cpr::Header{{"Dropbox-API-Arg", fmt::format(R"({{"path": "{}"}})", path)}},
				cpr::Bearer(KEY),
				cpr::WriteCallback([&output](const std::string& str) {
					output << str;

					fmt::print("got {} bytes", str.length());

					return true;
				})
		);

		if (resp.status_code != 200) {
			throw std::runtime_error(fmt::format("Status code: {}", resp.status_code));
		}
	} catch (const std::exception& e) {
		fmt::print("Exception: {}", e.what());
	}
}

void authenticationTest() {
	try {
		const std::string payloadFormat = R"(
		{{
			"agent": {{
				"name": "Minecraft",
				"version": 1
			}},
			"username": {},
			"password": {},
			"clientToken": {},
			"requestUser": true
		}})";

		std::string payload = fmt::vformat(payloadFormat, fmt::make_format_args("lol", "xd", "lmao"));

		cpr::Response resp = cpr::Get(
				cpr::Url("https://authserver.mojang.com/authenticate"),
				cpr::Header{{"Content-Type", "application/json"}},
				cpr::Body(payload)
		);

		if (resp.status_code != 200) {
			throw std::runtime_error(fmt::format("Status code: {}", resp.status_code));
		}

		fmt::print("");
	} catch (const std::exception &e) {
		fmt::print("Exception: {}", e.what());
	}
}

void getSizeTest() {
	try {
		std::string path = "/JreCheckList.fchecks";
		cpr::Response resp = cpr::Head(
				cpr::Url("https://content.dropboxapi.com/2/files/download"),
				cpr::Header{{"Dropbox-API-Arg", fmt::format(R"({{"path": "{}"}})", path)}},
				cpr::Bearer(KEY)
		);

		if (resp.status_code != 200) {
			throw std::runtime_error(fmt::format("Status code: {}", resp.status_code));
		}

		fmt::print("length: {}", resp.header["Content-Length"]);
	} catch (const std::exception& e) {
		fmt::print("Exception: {}", e.what());
	}
}

void getUUIDTest() {
	try {
		std::string username = "x";
		cpr::Response resp = cpr::Get(
				cpr::Url(std::string("https://api.mojang.com/users/profiles/minecraft/") + username),
				cpr::Header{{"Content-Type", "application/json"}}
		);

		if (resp.status_code != 200) {
			throw std::runtime_error(fmt::format("Status code: {}", resp.status_code));
		}

		nlohmann::json json = nlohmann::json::parse(resp.text);
		std::string uuid = json["id"];

		fmt::print("uuid: {}", uuid);
	} catch (const std::exception& e) {
		fmt::print("Exception: {}", e.what());
	}
}

void modsFolderTest() {
	CoInitialize(nullptr);

	std::string folderPathA = "C:\\msys64";

	size_t charsNeeded = MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, folderPathA.data(), (int) folderPathA.size(), nullptr, 0);
	auto* folderPath = new wchar_t[charsNeeded + 1];
	int written = MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, folderPathA.data(), (int) folderPathA.size(), folderPath, charsNeeded);
	folderPath[written] = '\0';

	PIDLIST_ABSOLUTE pidl;
	auto hresult = SHParseDisplayName(folderPath, nullptr, &pidl, 0, nullptr);
	if (SUCCEEDED(hresult)) {
		ITEMIDLIST idNull = { 0 };
		LPCITEMIDLIST pidlNull[1] = { &idNull };
		HRESULT hresult2 = SHOpenFolderAndSelectItems(pidl, 1, pidlNull, 0);
		ILFree(pidl);
	}
}

void bswapTest() {
	int size = 55 * 1024 + 5;
	unsigned long pixels[size];
	unsigned long pixels2[size];

	FillMemory(pixels, size * sizeof(unsigned long), 255);
	FillMemory(pixels2, size * sizeof(unsigned long), 255);

	{
		auto start = std::chrono::system_clock::now();
		for (int i = 0; i < size; i++) {
			pixels[i] = _byteswap_ulong(pixels[i]);
		}
		auto end = std::chrono::system_clock::now();
		std::cout << std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count() << " ms" << std::endl;
	}

	std::cout << pixels2[size + 1] << std::endl;

	{
		auto start = std::chrono::system_clock::now();
		for (int i = 0; i < size; i += Vec256::size()) {
			Vec256::swap(reinterpret_cast<uint8_t *>(pixels2), Vec256::getMask<unsigned long>());
		}
		auto end = std::chrono::system_clock::now();
		std::cout << std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count() << " ms" << std::endl;
	}

	std::cout << pixels2[size + 1] << std::endl;

	std::cout << memcmp(pixels, pixels2, size * sizeof(unsigned long));
}

void listFolderTest() {
	std::string folder = "/Versions";
	std::string args = std::string(R"({"path": ")") + folder + R"(","recursive": false,"include_media_info": false,"include_deleted": false,"include_has_explicit_shared_members": false,"include_mounted_folders": true,"include_non_downloadable_files": true})";

	try {
		cpr::Response resp = cpr::Post(
				cpr::Url("https://api.dropboxapi.com/2/files/list_folder"),
				cpr::Bearer(KEY),
				cpr::Header{{"Content-Type", "application/json"}},
				cpr::Body{args}
		);

		if (resp.status_code != 200) {
			throw std::runtime_error(fmt::format("Status code: {}", resp.status_code));
		}

		nlohmann::json j = nlohmann::json::parse(resp.text);

		fmt::print("entries: {}\n", j["entries"].size());

		for (const auto& entry : j["entries"]) {
			auto entryName = entry["name"].get<std::string>();

			fmt::print("Entry: {}\n", entryName);
		}
	} catch (const std::exception& e) {
		fmt::print("Exception: {}", e.what());
	}
}

void modpackSearchTest() {
	try {
		const std::string payloadFormat = R"(
		{{
			"query": "Files",
			"options": {{
				"path": "{}",
				"filename_only": true
			}}
		}})";

		std::string payload = fmt::vformat(payloadFormat, fmt::make_format_args("/Versions"));

		cpr::Response resp = cpr::Post(
				cpr::Url("https://api.dropboxapi.com/2/files/search_v2"),
				cpr::Bearer(KEY),
				cpr::Header{{"Content-Type", "application/json"}},
				cpr::Body(payload)
		);

		if (resp.status_code != 200) {
			throw std::runtime_error(fmt::format("Status code: {}", resp.status_code));
		}

		auto j = nlohmann::json::parse(resp.text);
		for (const auto& match : j["matches"]) {
			std::cout << match["metadata"]["metadata"]["path_display"] << std::endl;
		}
	} catch (const std::exception &e) {
		fmt::print("Exception: {}", e.what());
	}
}

int main() {
//	skinTest();

//	authenticationTest();

//	downloadTest();

//	getSizeTest();

//	getUUIDTest();

//	modsFolderTest();

//	bswapTest();

//	listFolderTest();

//	modpackSearchTest();

	return 0;
}