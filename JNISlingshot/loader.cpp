#include "loader.h"

[[maybe_unused]] void addDllPath(JNIEnv* env, jclass, jstring jDllFolderPath) {
	const char* dllFolderPath = env->GetStringUTFChars(jDllFolderPath, nullptr);

	auto pathBuffer = new char[65535];
	int writtenBytes = GetEnvironmentVariableA("PATH", pathBuffer, 65535);

	pathBuffer[writtenBytes] = ';';
	
	jsize length = env->GetStringLength(jDllFolderPath);
	for (int i = 0; i < length; ++i) {
		pathBuffer[writtenBytes + i + 1] = dllFolderPath[i];
	}
	pathBuffer[writtenBytes + length + 1] = '\0';
	SetEnvironmentVariableA("PATH", pathBuffer);
}