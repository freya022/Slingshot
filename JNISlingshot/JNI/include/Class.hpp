#pragma clang diagnostic push
#pragma ide diagnostic ignored "google-explicit-constructor"

#ifndef JNIFSHOTS_CLASS_HPP
#define JNIFSHOTS_CLASS_HPP

#include <fmt/core.h>
#include "Helper.hpp"

namespace JNI {
	class Method;

	class Class {
	private:
		void checkMethodID(std::string_view fullSignature, jmethodID methodId, std::string_view funcType) const;

	public:
		jclass clazz;

		explicit Class(std::string_view view);

		[[maybe_unused]] Class(jclass clazz) : clazz(clazz) { }

		[[nodiscard]] std::string getName() const;

		operator jclass() const { return clazz; }

		[[nodiscard]] Method getStaticMethod(std::string_view fullSignature) const;

		[[nodiscard]] Method getInstanceMethod(jobject instance, std::string_view fullSignature) const;

		template<typename... U>
		[[nodiscard]] jobject NewInstance(std::string_view fullSignature, U... u) const;
	};
}


#endif //JNIFSHOTS_CLASS_HPP

#pragma clang diagnostic pop