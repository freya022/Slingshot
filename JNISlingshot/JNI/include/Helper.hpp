
#ifndef JNIFSHOTS_JNI_HELPER_HPP
#define JNIFSHOTS_JNI_HELPER_HPP

#include <string>
#include <string_view>
#include <sstream>

#include "../str_utils.hpp"
#include "jni.h"

namespace JNI {
	void SetEnv(JavaVM* e);

	JNIEnv* GetEnv();

	std::string getClassPath(std::string_view classPath);

	std::string GetTypeSignature(std::string_view type);
	std::string GetMethodSignature(std::string_view rtype, std::initializer_list<std::string_view> parameters);

	std::string getMethodSig(std::string_view rtype, std::string_view params);
	std::string getConstructorSig(std::string_view params);
	std::string getTypeFromParameter(std::string_view params, size_t start, size_t end);
}

#endif //JNIFSHOTS_JNI_HELPER_HPP
