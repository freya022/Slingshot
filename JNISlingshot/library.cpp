#include "library.h"

size_t writeFunction(char* ptr, size_t size, size_t nmemb, JNIData* data) {
	for (int i = 0; i < size * nmemb; ++i) {
		*(data->localStream) << ptr[i];
	}

	//Only init one time, the method is the same everywhere
	static jmethodID method = data->env->GetMethodID(data->clazz, "onWrite", "(I)V"); //Param int return void

	data->env->CallVoidMethod(data->jThis, method, size * nmemb);

	return size * nmemb;
}

[[maybe_unused]] void downloadFile0(JNIEnv* env, jclass clazz, jstring dropboxJString, jstring osJString, jobject jThis) {
	jni_str dropboxJniStr(env, dropboxJString);
	jni_str osPathJniStr(env, osJString);

	const char *dropboxPath = dropboxJniStr.data();
	const char *osPath = osPathJniStr.data();

	auto curl = curl_easy_init();
	if (curl) {
		curl_easy_setopt(curl, CURLOPT_URL, "https://content.dropboxapi.com/2/files/download");
		curl_easy_setopt(curl, CURLOPT_MAXREDIRS, 0L);
		curl_easy_setopt(curl, CURLOPT_TCP_KEEPALIVE, 1L);
		curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, writeFunction);
		curl_slist *headers = curl_slist_append(nullptr, getAuthorizationHeader());
		std::stringstream ss;
		ss << R"(Dropbox-API-Arg: {"path": ")" << dropboxPath << R"("})";
		headers = curl_slist_append(headers, ss.str().c_str());
		curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);

		//Allows download of text & binary files. Lol ?
		std::ofstream t(osPath, std::ios::binary);

		JNIData jniData{env, clazz, jThis, &t};
		curl_easy_setopt(curl, CURLOPT_WRITEDATA, &jniData);

		curl_easy_perform(curl);

		curl_slist_free_all(headers);
		curl_easy_cleanup(curl); //Do this at last, "url" is deleted after this
	}
}

[[maybe_unused]] jlong getDownloadSize0(JNIEnv* env, jclass, jstring dropboxJString) {
	jni_str dropboxJniStr(env, dropboxJString);

	const char *dropboxPath = dropboxJniStr.data();

	auto curl = curl_easy_init();
	if (curl) {
		//Standard connection
		curl_easy_setopt(curl, CURLOPT_URL, "https://content.dropboxapi.com/2/files/download");
		curl_easy_setopt(curl, CURLOPT_MAXREDIRS, 0L);
		curl_easy_setopt(curl, CURLOPT_TCP_KEEPALIVE, 1L);
		curl_easy_setopt(curl, CURLOPT_NOBODY, 1L);

		//Set the headers
		curl_slist *headers = curl_slist_append(nullptr, getAuthorizationHeader());
		std::stringstream ss;
		ss << R"(Dropbox-API-Arg: {"path": ")" << dropboxPath << R"("})";
		headers = curl_slist_append(headers, ss.str().c_str());
		curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);

		curl_easy_perform(curl);

		curl_off_t size;
		curl_easy_getinfo(curl, CURLINFO_CONTENT_LENGTH_DOWNLOAD_T, &size);

		curl_slist_free_all(headers);
		curl_easy_cleanup(curl); //Do this at last, "url" is deleted after this

		return size;
	}

	return -1;
}


size_t writeString(char* ptr, size_t size, size_t nmemb, std::string* data) {
	data->append(ptr, size * nmemb);

	return size * nmemb;
}

[[maybe_unused]] jobjectArray listFolder0(JNIEnv* env, jclass, jstring dropboxJString) {
	jni_str dropboxJniStr(env, dropboxJString);

	const char *dropboxPath = dropboxJniStr.data();

	std::stringstream dataStream;
	dataStream << R"({"path": ")";
	dataStream << dropboxPath;
	dataStream << R"(","recursive": false,"include_media_info": false,"include_deleted": false,"include_has_explicit_shared_members": false,"include_mounted_folders": true,"include_non_downloadable_files": true})";

	auto curl = curl_easy_init();
	if (curl) {
		//Standard connection
		curl_easy_setopt(curl, CURLOPT_URL, "https://api.dropboxapi.com/2/files/list_folder");
		curl_easy_setopt(curl, CURLOPT_MAXREDIRS, 0L);
		curl_easy_setopt(curl, CURLOPT_TCP_KEEPALIVE, 1L);
		curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, writeString);
		std::string writeData;
		curl_easy_setopt(curl, CURLOPT_WRITEDATA, &writeData);

		//Set the headers
		curl_slist *headers = curl_slist_append(nullptr, getAuthorizationHeader());
		headers = curl_slist_append(headers, "Content-Type: application/json");
		curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);

		const std::string &basicString = dataStream.str();
		const char *string = basicString.c_str();
		//Set the data
		curl_easy_setopt(curl, CURLOPT_POSTFIELDS, string);

		curl_easy_perform(curl);

		nlohmann::json j = nlohmann::json::parse(writeData);
		auto arr = env->NewObjectArray(j["entries"].size(), env->FindClass("java/lang/String"), nullptr);
		int i = 0;
		for (const auto& entry : j["entries"]) {
			auto entryName = entry["name"].get<std::string>();

			env->SetObjectArrayElement(arr, i, env->NewStringUTF(entryName.c_str()));
			i++;
		}

		curl_slist_free_all(headers);
		curl_easy_cleanup(curl); //Do this at last, "url" is deleted after this

		return arr;
	}

	return env->NewObjectArray(0, env->FindClass("java/lang/String"), nullptr);
}

[[maybe_unused]] jstring getUuid0(JNIEnv* env, jclass, jstring usernameJString) {
	jni_str usernameJniStr(env, usernameJString);

	const char *username = usernameJniStr.data();

	auto curl = curl_easy_init();
	if (curl) {
		//Standard connection
		const auto &string = std::string("https://api.mojang.com/users/profiles/minecraft/") + username;
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

		fmt::print("Received UUID data:\n{}\n", writeData);

		std::string uuid;

		if (!writeData.empty()) {
			nlohmann::json j = nlohmann::json::parse(writeData);

			uuid = j["id"].get<std::string>();
		}

		curl_slist_free_all(headers);
		curl_easy_cleanup(curl); //Do this at last, "url" is deleted after this

		return env->NewStringUTF(uuid.c_str());
	}

	return env->NewStringUTF("0");
}

[[maybe_unused]] jstring authenticate0(JNIEnv* env, jclass, jstring identifierJString, jstring passwordJString, jstring clientTokenJString) {
	jni_str identifierJniStr(env, identifierJString);
	jni_str passwordJniStr(env, passwordJString);
	jni_str clientTokenJniStr(env, clientTokenJString);

	const char *identifier = identifierJniStr.data();
	const char *password = passwordJniStr.data();
	const char *clientToken = clientTokenJniStr.data();

	auto curl = curl_easy_init();
	if (curl) {
		//Standard connection
		curl_easy_setopt(curl, CURLOPT_URL, "https://authserver.mojang.com/authenticate");
		curl_easy_setopt(curl, CURLOPT_MAXREDIRS, 0L);
		curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, writeString);
		std::string writeData;
		curl_easy_setopt(curl, CURLOPT_WRITEDATA, &writeData);

		//Set the headers
		curl_slist *headers = curl_slist_append(nullptr, "Content-Type: application/json");
		curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);

		std::string payload = "{\n"
						"            \"agent\": {\n"
						"                \"name\": \"Minecraft\",\n"
						"                \"version\": 1\n"
						"            },\n"
						"            \"username\": \"" + std::string(identifier) + "\",\n"
						"            \"password\": \"" + std::string(password) + "\",\n"
						"            \"clientToken\": \"" + std::string(clientToken) + "\",\n"
						"            \"requestUser\": true\n"
						"        }";

		curl_easy_setopt(curl, CURLOPT_POSTFIELDS, payload.c_str());

		curl_easy_perform(curl);

		fmt::print("Received authentication data:\n{}\n", writeData);

		std::stringstream ss;

		if (!writeData.empty()) {
			nlohmann::json j = nlohmann::json::parse(writeData);

			if (j.contains("selectedProfile")) {
				ss << j["selectedProfile"]["name"].get<std::string>() << ';' << j["selectedProfile"]["id"].get<std::string>() << ';' << j["clientToken"].get<std::string>() << ';' << j["accessToken"].get<std::string>();
			} else {
				throwIOException(env, "Invalid credentials : " + writeData);
				return nullptr;
			}
		}

		curl_slist_free_all(headers);
		curl_easy_cleanup(curl); //Do this at last, "url" is deleted after this

		return env->NewStringUTF(ss.str().c_str());
	}

	return env->NewStringUTF({});
}

[[maybe_unused]] jbyteArray getSkinImage0(JNIEnv* env, jclass, jstring uuidJString) {
	jni_str uuidJniStr(env, uuidJString);

	const char *uuid = uuidJniStr.data();

	auto curl = curl_easy_init();
	if (curl) {
		//Standard connection
		const auto &string = std::string("https://sessionserver.mojang.com/session/minecraft/profile/") + uuid;
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

		fmt::print("Received skin data:\n{}\n", writeData);

		std::vector<char> bytes;

		if (!writeData.empty()) {
			nlohmann::json j = nlohmann::json::parse(writeData);

			if (j.contains("properties")) {
				for (char c : j["name"].get<std::string>()) {
					bytes.emplace_back(c);
				}
				bytes.emplace_back(';');

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
		
		jbyteArray pArray = env->NewByteArray(bytes.size());
		env->SetByteArrayRegion(pArray, 0, bytes.size(), reinterpret_cast<const jbyte *>(bytes.data()));
		return pArray;
	}

	return env->NewByteArray(0);
}

void downloadSkin(const std::string &url, std::vector<char> &bytes) {
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

		fmt::print("Received skin download data:\n{}\n", writeData);

		bytes.insert(bytes.end(), writeData.begin(), writeData.end());

		curl_easy_cleanup(curl); //Do this at last, "url" is deleted after this
	}
}

[[maybe_unused]] void launchGame0(JNIEnv* env, jclass, jstring javawJString, jstring workingDirJString, jstring commandlineJString) {
	jni_str javawJniStr(env, javawJString);
	jni_str workingDirJniStr(env, workingDirJString);
	jni_str commandlineJniStr(env, commandlineJString);

	auto cmd = std::string(commandlineJniStr.data());
	CreateProcessA(
			javawJniStr.data(),
			cmd.data(),
			nullptr,
			nullptr,
			FALSE,
			NORMAL_PRIORITY_CLASS,
			nullptr,
			workingDirJniStr.data(),
			new STARTUPINFO {},
			new PROCESS_INFORMATION {}
	);
}

[[maybe_unused]] void openFolder0(JNIEnv* env, jclass, jstring folderPathJString) {
	jni_str folderPathJniStr(env,  folderPathJString);

	std::string folderPathA = std::string(folderPathJniStr.data());

	size_t charsNeeded = ::MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, folderPathA.data(), (int) folderPathA.size(), nullptr, 0);
	auto* folderPath = new wchar_t[charsNeeded];
	::MultiByteToWideChar(CP_UTF8, MB_ERR_INVALID_CHARS, folderPathA.data(), (int) folderPathA.size(), folderPath, charsNeeded);

	PIDLIST_ABSOLUTE pidl;
	if (SUCCEEDED(SHParseDisplayName(folderPath, nullptr, &pidl, 0, nullptr))) {
		ITEMIDLIST idNull = { 0 };
		LPCITEMIDLIST pidlNull[1] = { &idNull };
		SHOpenFolderAndSelectItems(pidl, 1, pidlNull, 0);
		ILFree(pidl);
	}
}

[[maybe_unused]] void saveImage0(JNIEnv* env, jclass, jstring pathJString, jintArray jpixels, jint width, jint height) {
	jni_str pathJniStr(env, pathJString);

	jboolean isCopy;
	auto* fpixels = reinterpret_cast<unsigned long *>(env->GetIntArrayElements(jpixels, &isCopy));

	auto* pixels = new unsigned long[width * height];
	for (int i = 0; i < width * height; ++i) {
		pixels[i] = (fpixels[i] & 0xFF00FF00) | ((fpixels[i] & 0x00FF0000) >> 16) | ((fpixels[i] & 0x000000FF) << 16);
	}

	stbi_write_png(pathJniStr.data(), width, height, 4, pixels, width * 4);

	if (isCopy == JNI_TRUE) {
		env->ReleaseIntArrayElements(jpixels, reinterpret_cast<jint *>(fpixels), JNI_ABORT);
	}

	delete[] pixels;
}