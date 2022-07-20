#include "library.h"

[[maybe_unused]] void downloadFile0(JNIEnv* env, jclass, jstring dropboxJString, jstring osJString, jobject jThis) {
	JNI::String dropboxPath(dropboxJString);
	JNI::String osPath(osJString);

	try {
		//Only init one time, the method is the same everywhere
		static jmethodID method = env->GetMethodID(env->GetObjectClass(jThis), "onWrite", "(I)V"); //Param int return void

		std::string osPathTmp = std::string(osPath.data()) + ".tmp";
		{ //Close the std::ofstream handle before doing std::filesystem::move
			std::ofstream out(osPathTmp, std::ios::binary);
			size_t downloadOld = 0;
			cpr::Response resp = cpr::Download(
					out,
					cpr::Url("https://content.dropboxapi.com/2/files/download"),
					cpr::Header{{"Dropbox-API-Arg", fmt::format(R"({{"path": "{}"}})", dropboxPath.data())}},
					cpr::Bearer(KEY),
					cpr::ProgressCallback(
							[&downloadOld, &jThis, &env](size_t downloadTotal, size_t downloadNow, size_t uploadTotal,
														 size_t uploadNow) {
								//Don't update Slingshot UI because of CPR spamming these callbacks for no reason
								if (downloadNow == downloadOld) return true;

								size_t progress = downloadNow - downloadOld;
								downloadOld = downloadNow;

								env->CallVoidMethod(jThis, method, progress);

								return true;
							})
			);

			if (resp.status_code != 200) {
				throw std::runtime_error(fmt::format("Dropbox download status code: {}", resp.status_code));
			}
		}

		std::filesystem::rename(osPathTmp, osPath.data());
	} catch (const std::exception& e) {
		fmt::print("Exception: {}", e.what());
		env->ThrowNew(env->FindClass("java/io/IOException"),
					  fmt::format("Exception while downloading dropbox file {}: {}", dropboxPath.data(), e.what()).c_str());
	}
}

[[maybe_unused]] jlong getDownloadSize0(JNIEnv* env, jclass, jstring dropboxJString) {
	JNI::String dropboxPath(dropboxJString);

	try {
		cpr::Response resp = cpr::Post(
				cpr::Url("https://api.dropboxapi.com/2/files/get_metadata"),
				cpr::Header{{"Content-Type", "application/json"}},
				cpr::Body(fmt::format(R"({{
					"include_deleted": false,
					"include_has_explicit_shared_members": false,
					"include_media_info": false,
					"path": "{}"
				}})", dropboxPath.data())),
				cpr::Bearer(KEY)
		);

		if (resp.status_code != 200) {
			throw std::runtime_error(fmt::format("Get download size status code: {}", resp.status_code));
		}

		nlohmann::json json = nlohmann::json::parse(resp.text);
		jlong size = json["size"];

		return size;
	} catch (const std::exception& e) {
		fmt::print("Exception: {}", e.what());
		env->ThrowNew(env->FindClass("java/io/IOException"),
					  fmt::format("Exception while downloading dropbox file {}: {}", dropboxPath.data(), e.what()).c_str());
	}

	return -1;
}

[[maybe_unused]] jobjectArray searchModpacks0(JNIEnv* env, jclass) {
	try {
		constexpr auto payload = R"(
		{
			"query": "Files",
			"options": {
				"path": "/Versions",
				"filename_only": true
			}
		})";

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
		const auto &matches = j["matches"];
		auto arr = env->NewObjectArray(matches.size(), env->FindClass("java/lang/String"), nullptr);
		for (int i = 0; i < matches.size(); i++) {
			std::string path = matches[i]["metadata"]["metadata"]["path_display"];

			env->SetObjectArrayElement(arr, i, env->NewStringUTF(path.c_str()));
		}

		return arr;
	} catch (const std::exception& e) {
		fmt::print("Exception: {}", e.what());
		env->ThrowNew(env->FindClass("java/io/IOException"),
					  fmt::format("Exception while searching modpacks: {}", e.what()).c_str());
	}

	return nullptr;
}

[[maybe_unused]] jstring getUuid0(JNIEnv* env, jclass, jstring usernameJString) {
	JNI::String username(usernameJString);

	try {
		cpr::Response resp = cpr::Get(
				cpr::Url(std::string("https://api.mojang.com/users/profiles/minecraft/") + username.data()),
				cpr::Header{{"Content-Type", "application/json"}}
		);

		if (resp.status_code == 204) {
			return env->NewStringUTF("0");
		}

		if (resp.status_code != 200) {
			throw std::runtime_error(fmt::format("Status code: {}", resp.status_code));
		}

		nlohmann::json json = nlohmann::json::parse(resp.text);
		std::string uuid = json["id"];

		return env->NewStringUTF(uuid.c_str());
	} catch (const std::exception& e) {
		fmt::print("Exception: {}", e.what());
		env->ThrowNew(env->FindClass("java/io/IOException"),
					  fmt::format("Exception while getting UUID for username '{}': {}", username.data(), e.what()).c_str());
	}

	return nullptr;
}

[[maybe_unused]] jobjectArray authenticate0(JNIEnv* env, jclass, jstring identifierJString, jstring passwordJString, jstring clientTokenJString) {
	JNI::String identifier(identifierJString);
	JNI::String password(passwordJString);
	JNI::String clientToken(clientTokenJString);

	try {
		const std::string payloadFormat = R"(
		{{
			"agent": {{
				"name": "Minecraft",
				"version": 1
			}},
			"username": "{}",
			"password": "{}",
			"clientToken": "{}",
			"requestUser": true
		}})";

		std::string payload = fmt::vformat(payloadFormat, fmt::make_format_args(identifier.data(), password.data(), clientToken.data()));

		cpr::Response resp = cpr::Post(
				cpr::Url("https://authserver.mojang.com/authenticate"),
				cpr::Header{{"Content-Type", "application/json"}},
				cpr::Body(payload)
		);

		if (resp.status_code != 200) {
			throw std::runtime_error(fmt::format("Status code: {}", resp.status_code));
		}

		nlohmann::json j = nlohmann::json::parse(resp.text);
		const std::string &username = j["selectedProfile"]["name"];
		const std::string &uuid = j["selectedProfile"]["id"];
		const std::string &newClientToken = j["clientToken"];
		const std::string &accessToken = j["accessToken"];

		jobjectArray oArray = env->NewObjectArray(4, env->FindClass("java/lang/String"), nullptr);
		env->SetObjectArrayElement(oArray, 0, env->NewStringUTF(username.c_str()));
		env->SetObjectArrayElement(oArray, 1, env->NewStringUTF(uuid.c_str()));
		env->SetObjectArrayElement(oArray, 2, env->NewStringUTF(newClientToken.c_str()));
		env->SetObjectArrayElement(oArray, 3, env->NewStringUTF(accessToken.c_str()));

		return oArray;
	} catch (const std::exception &e) {
		fmt::print("Exception: {}", e.what());
		env->ThrowNew(env->FindClass("java/io/IOException"),
					  fmt::format("Exception while authentication: {}", e.what()).c_str());
	}

	return nullptr;
}

[[maybe_unused]] jobjectArray getSkinImage0(JNIEnv* env, jclass, jstring uuidJString) {
	JNI::String uuid(uuidJString);

	try {
		const std::string sessionserverUrl = std::string("https://sessionserver.mojang.com/session/minecraft/profile/") + uuid.data();

		cpr::Response resp = cpr::Get(
				cpr::Url(sessionserverUrl),
				cpr::Header{{"Content-Type", "application/json"}}
		);

		if (resp.status_code != 200) {
			throw std::runtime_error(fmt::format("Profile GET status code: {}", resp.status_code));
		}

		nlohmann::json j = nlohmann::json::parse(resp.text);
		const std::string& username = j["name"];
		const std::string& skinjson = base64_decode(j["properties"][0]["value"].get<std::string>(), false);

		nlohmann::json j2 = nlohmann::json::parse(skinjson);
		const std::string& url = j2["textures"]["SKIN"]["url"];

		std::vector<char> skinBytes;
		skinBytes.reserve(10240);
		cpr::Response downloadResp = cpr::Download(cpr::WriteCallback([&skinBytes](std::string_view str) {
			skinBytes.insert(skinBytes.end(), str.begin(), str.end());

			return true;
		}), cpr::Url(url));

		if (downloadResp.status_code != 200) {
			throw std::runtime_error(fmt::format("Skin download status code: {}", downloadResp.status_code));
		}

		jstring jname = env->NewStringUTF(username.c_str());
		jobjectArray oArray = env->NewObjectArray(2, env->FindClass("java/lang/Object"), nullptr);

		jbyteArray pArray = env->NewByteArray(skinBytes.size());
		env->SetByteArrayRegion(pArray, 0, skinBytes.size(), reinterpret_cast<const jbyte *>(skinBytes.data()));

		env->SetObjectArrayElement(oArray, 0, jname);
		env->SetObjectArrayElement(oArray, 1, pArray);

		return oArray;
	} catch (const std::exception& e) {
		fmt::print("Exception: {}", e.what());
		env->ThrowNew(env->FindClass("java/io/IOException"),
				fmt::format("Exception while downloading skin of uuid '{}' : {}", uuid.data(), e.what()).c_str());
	}

	return nullptr;
}

[[maybe_unused]] void launchGame0(JNIEnv*, jclass, jstring javawJString, jstring workingDirJString, jstring commandlineJString) {
	JNI::String name(javawJString);
	JNI::String line(commandlineJString);
	JNI::String directory(workingDirJString);

	CreateProcessA(
			name.data(),
			const_cast<char*>(line.data()),
			nullptr,
			nullptr,
			FALSE,
			NORMAL_PRIORITY_CLASS,
			nullptr,
			directory.data(),
			new STARTUPINFO {},
			new PROCESS_INFORMATION {}
	);
}

[[maybe_unused]] void openFolder0(JNIEnv*, jclass, jstring folderPathJString) {
	JNI::String folderPathStr(folderPathJString);

	size_t charsNeeded = ::MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, folderPathStr, folderPathStr.length(), nullptr, 0);
	auto* folderPath = new wchar_t[charsNeeded + 1];
	int written = ::MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, folderPathStr, folderPathStr.length(), folderPath, charsNeeded);
	folderPath[written] = '\0';

	PIDLIST_ABSOLUTE pidl;
	if (SUCCEEDED(SHParseDisplayName(folderPath, nullptr, &pidl, 0, nullptr))) {
		ITEMIDLIST idNull = { 0 };
		LPCITEMIDLIST pidlNull[1] = { &idNull };
		SHOpenFolderAndSelectItems(pidl, 1, pidlNull, 0);
		ILFree(pidl);
	}
}