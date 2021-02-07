#ifndef JNISLINGSHOT_LOADER_H
#define JNISLINGSHOT_LOADER_H

#include <windows.h>
#include "jni.h"
#include <string>

#define addDllPath Java_com_freya02_slingshot_AOT_addDllPath

[[maybe_unused]] BOOL APIENTRY DllMain(HMODULE, DWORD, LPVOID) { return TRUE; };

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL addDllPath(JNIEnv *env, jclass, jstring);

#endif //JNISLINGSHOT_LOADER_H
