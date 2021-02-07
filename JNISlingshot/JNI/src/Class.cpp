#include "Class.hpp"
#include "Method.hpp"
#include "Exception.hpp"

void JNI::Class::checkMethodID(std::string_view fullSignature, jmethodID methodId, std::string_view funcType) const {
	if (methodId == nullptr) {
		const std::string &string = fmt::format("{} '{}' in '{}' is not recognized", funcType, fullSignature, getName());

		throw Exception("java.lang.NoSuchMethodException", string); //Short circuit, hope that the JNI function has a try catch
	}
}

JNI::Method JNI::Class::getStaticMethod(std::string_view fullSignature) const {
	auto spaceAfterRType = fullSignature.find(' ');
	auto parenthesis = fullSignature.find('(', spaceAfterRType);
	auto endParenthesis = fullSignature.rfind(')');

	const auto& returnType = fullSignature.substr(0, spaceAfterRType);
	const auto& methodName = trim(fullSignature.substr(spaceAfterRType + 1,
													   (parenthesis - spaceAfterRType - 1)));
	const auto& params = fullSignature.substr(parenthesis + 1, (endParenthesis - parenthesis - 1));

	std::string methodSig = getMethodSig(returnType, params);

	jmethodID methodId = GetEnv()->GetStaticMethodID(clazz, methodName.data(), methodSig.c_str());
	checkMethodID(fullSignature, methodId, "Static method");

	return Method(*this, methodId, false);
}

JNI::Method JNI::Class::getInstanceMethod(jobject instance, std::string_view fullSignature) const {
	auto spaceAfterRType = fullSignature.find(' ');
	auto parenthesis = fullSignature.find('(', spaceAfterRType);
	auto endParenthesis = fullSignature.rfind(')');

	const auto& returnType = fullSignature.substr(0, spaceAfterRType);
	const auto& methodName = trim(fullSignature.substr(spaceAfterRType + 1,
													   (parenthesis - spaceAfterRType - 1)));
	const auto& params = fullSignature.substr(parenthesis + 1, (endParenthesis - parenthesis - 1));

	std::string methodSig = getMethodSig(returnType, params);

	jmethodID methodId = GetEnv()->GetMethodID(clazz, methodName.data(), methodSig.c_str());
	checkMethodID(fullSignature, methodId, "Member method");

	return Method(*this, methodId, false, instance);
}

template<typename... U>
jobject JNI::Class::NewInstance(std::string_view fullSignature, U... u) const {
	auto parenthesis = fullSignature.find('(');
	auto endParenthesis = fullSignature.rfind(')');

	const auto& params = fullSignature.substr(parenthesis + 1, (endParenthesis - parenthesis - 1));

	std::string methodSig = getConstructorSig(params);

	jmethodID methodId = GetEnv()->GetMethodID(clazz, "<init>", methodSig.c_str());
	checkMethodID(fullSignature, methodId, "Constructor");

	return GetEnv()->NewObject(clazz, methodId, u...);
}

std::string JNI::Class::getName() const {
	JNI::Class classClazz("java.lang.Class");
	JNI::Method getName = classClazz.getInstanceMethod(clazz, "String getName()");
	auto str = getName.call<jstring>();

	jboolean copy;
	return std::string(GetEnv()->GetStringUTFChars(str, &copy));
}

JNI::Class::Class(std::string_view view) : clazz() {
	const std::string& classPath = getClassPath(view);
	clazz = GetEnv()->FindClass(classPath.c_str());

	if (clazz == nullptr) {
		throw Exception("java.lang.NoClassDefFoundError", "Could not find class for " + classPath);
	}
}
