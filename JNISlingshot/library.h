#ifndef JNISLINGSHOT_LIBRARY_H
#define JNISLINGSHOT_LIBRARY_H

#include <windows.h>
#include "Shlobj_core.h"
#include <fstream>
#include <string>
#include <sstream>
#include <filesystem>

#include "json.h"
#include "base64/base64.h"
#include <fmt/core.h>
#include <curl/curl.h>
#include <cpr/cpr.h>

#include "jni.h"
#include "Method.hpp"
#include "String.hpp"
#include "Helper.hpp"
#include "bswapintrin.h"
#include "key.h"

#define downloadFile0 Java_com_freya02_slingshot_SlingshotTask_downloadFile0
#define getDownloadSize0 Java_com_freya02_slingshot_SlingshotTask_getDownloadSize0
#define launchGame0 Java_com_freya02_slingshot_SlingshotTask_launchGame0

#define getUuid0 Java_com_freya02_slingshot_auth_AuthController_getUuid0
#define authenticate0 Java_com_freya02_slingshot_auth_AuthController_authenticate0

#define searchModpacks0 Java_com_freya02_slingshot_SlingshotController_searchModpacks0
#define getSkinImage0 Java_com_freya02_slingshot_SlingshotController_getSkinImage0
#define openFolder0 Java_com_freya02_slingshot_SlingshotController_openFolder0

[[maybe_unused]] BOOL APIENTRY DllMain(HMODULE, DWORD, LPVOID) { return TRUE; }

extern "C" {
	[[maybe_unused]] JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
		JNI::SetEnv(vm);

		return JNI_VERSION_1_8;
	}

	[[maybe_unused]] JNIEXPORT void JNICALL downloadFile0(JNIEnv* env, jclass clazz, jstring dropboxJString, jstring osJString, jobject jThis);
	[[maybe_unused]] JNIEXPORT jlong JNICALL getDownloadSize0(JNIEnv* env, jclass clazz, jstring dropboxJString);
	[[maybe_unused]] JNIEXPORT jobjectArray JNICALL searchModpacks0(JNIEnv* env, jclass clazz);
	[[maybe_unused]] JNIEXPORT void JNICALL launchGame0(JNIEnv* env, jclass, jstring javawJString, jstring workingDirJString, jstring commandlineJString);

	[[maybe_unused]] JNIEXPORT jstring JNICALL getUuid0(JNIEnv* env, jclass clazz, jstring usernameJString);
	[[maybe_unused]] JNIEXPORT jobjectArray JNICALL authenticate0(JNIEnv* env, jclass clazz, jstring identifierJString, jstring passwordJString, jstring clientTokenJString);

	[[maybe_unused]] JNIEXPORT jobjectArray JNICALL getSkinImage0(JNIEnv* env, jclass clazz, jstring uuidJString);
	[[maybe_unused]] JNIEXPORT void JNICALL openFolder0(JNIEnv* env, jclass, jstring folderPathJString);
}

#endif //JNISLINGSHOT_LIBRARY_H
