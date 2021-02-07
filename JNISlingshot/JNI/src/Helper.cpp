#include <fstream>
#include "Helper.hpp"
#include "Exception.hpp"

static JavaVM* vm = nullptr;
thread_local static JNIEnv* env = nullptr;

void JNI::SetEnv(JavaVM* e) {
	vm = e;
}

JNIEnv* JNI::GetEnv() {
	if (vm == nullptr) {
		fmt::print("FATAL: JavaVM* was not set in JNI::Helper::SetEnv");
		throw std::runtime_error("JavaVM* was not set in JNI::Helper::SetEnv");
	}

	if (env == nullptr) {
		jint res = vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_8);
		if (res != 0) {
			throw std::runtime_error("GetEnv code == " + std::to_string(res));
		}
	}

	return env;
}

std::string JNI::getClassPath(std::string_view classPath) {
	std::string s;
	s.reserve(classPath.length());

	for (const auto& c : classPath) {
		if (c == '.') {
			s += '/';
		} else {
			s += c;
		}
	}

	return s;
}

std::string JNI::GetTypeSignature(std::string_view type) {
	if (type == "boolean") {
		return "Z";
	} if (type == "byte") {
		return "B";
	} else if (type == "char") {
		return "C";
	} else if (type == "short") {
		return "S";
	} else if (type == "int") {
		return "I";
	} else if (type == "long") {
		return "J";
	} else if (type == "float") {
		return "F";
	} else if (type == "double") {
		return "D";
	} else if (type == "String") {
		return "Ljava/lang/String;";
	} else if (type == "void") {
		return "V";
	} else {
		if (type.rfind("[]") != std::string::npos) {
			return "[" + GetTypeSignature(type.substr(0, type.length() - 2));
		} else {
			return "L" + getClassPath(type) + ";";
		}
	}
}

std::string JNI::GetMethodSignature(std::string_view rtype, std::initializer_list<std::string_view> parameters) {
	std::stringstream ss;
	ss << "(";
	for (auto parameter : parameters) {
		ss << GetTypeSignature(parameter);
	}
	ss << ")" << GetTypeSignature(rtype);

	return ss.str();
}

std::string JNI::getTypeFromParameter(std::string_view params, size_t start, size_t end) {
	auto trim1 = trim(params.substr(start, end - start));
	auto type = trim1.substr(0, trim1.find(' '));
	if (trim(type).empty()) return "";

	return JNI::GetTypeSignature(type);
}

std::string JNI::getMethodSig(std::string_view rtype, std::string_view params) {
	std::string methodSig("(");

	//Adding parameters
	auto start = 0U;
	auto end = params.find(',');
	while (end != std::string::npos) {
		methodSig += getTypeFromParameter(params, start, end);
		start = end + 1;
		end = params.find(',', start);
	}

	methodSig += getTypeFromParameter(params, start, end) + ')' + JNI::GetTypeSignature(rtype);

	return methodSig;
}

std::string JNI::getConstructorSig(std::string_view params) {
	std::string methodSig("(");

	//Adding parameters
	auto start = 0U;
	auto end = params.find(',');
	while (end != std::string::npos) {
		methodSig += getTypeFromParameter(params, start, end);
		start = end + 1;
		end = params.find(',', start);
	}

	methodSig += getTypeFromParameter(params, start, end) + ")V";

	return methodSig;
}