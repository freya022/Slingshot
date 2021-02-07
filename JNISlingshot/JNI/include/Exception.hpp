
#ifndef JNIFSHOTS_JNI_EXCEPTION_HPP
#define JNIFSHOTS_JNI_EXCEPTION_HPP

#include "Helper.hpp"
#include "Class.hpp"

namespace JNI {
	class Exception : public std::runtime_error {
	public:
		explicit Exception(std::string_view exClass, const std::string& message) : runtime_error(message) {
			GetEnv()->ExceptionClear();
			GetEnv()->ThrowNew(JNI::Class(exClass), message.data());
		}
	};
}

#endif //JNIFSHOTS_JNI_EXCEPTION_HPP
