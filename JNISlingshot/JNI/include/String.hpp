#pragma clang diagnostic push
#pragma ide diagnostic ignored "google-explicit-constructor"

#ifndef JNI_STRING_HPP
#define JNI_STRING_HPP

#include "Helper.hpp"
#include <string>

namespace JNI {
	class String {
	private:
		jstring jstr;
		const char* str;
		jboolean isCopy;
		jsize strLength;

	public:
		[[nodiscard]] explicit String(jstring jstr) : jstr(jstr), strLength(GetEnv()->GetStringUTFLength(jstr)), str(GetEnv()->GetStringUTFChars(jstr, &isCopy)), isCopy(false) { }

		~String() {
			if (isCopy == JNI_TRUE) {
				GetEnv()->ReleaseStringUTFChars(jstr, str);
			}
		}

		//Do not use that after this object is destroyed, the string may be freed !
		operator const char*() {
			return str;
		}

		size_t length() const {
			return strLength;
		}

		const char* data() const {
			return str;
		}
	};
}


#endif //JNI_STRING_HPP

#pragma clang diagnostic pop