//
// Created by L5O on 23/07/2020.
//

#ifndef JNISLINGSHOT_LOADER_H
#define JNISLINGSHOT_LOADER_H

#include <windows.h>
#include "JNI/jni.h"
#include <string>

[[maybe_unused]] BOOL APIENTRY DllMain(HMODULE, DWORD, LPVOID) { return TRUE; };

extern "C" [[maybe_unused]] JNIEXPORT void JNICALL Java_com_freya02_slingshot_MinecraftTask_addDllPath(JNIEnv *env, jclass, jstring);

#endif //JNISLINGSHOT_LOADER_H
