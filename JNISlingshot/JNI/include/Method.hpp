
#ifndef JNIFSHOTS_METHOD_HPP
#define JNIFSHOTS_METHOD_HPP

#include "Helper.hpp"
#include "Exception.hpp"

namespace JNI {
	class Class;

	class Method {
	private:
		jmethodID jMethodId;
		jobject instance;
		const bool isConstructor;
		const Class& clazz;

	public:
		explicit Method(const Class& clazz, jmethodID jMethodId, bool isConstructor, jobject instance = nullptr)
				: jMethodId(jMethodId), instance(instance), clazz(clazz), isConstructor(isConstructor) {};

		template<typename T, typename... U>
		T call(U... u) {
			static_assert(
					std::is_base_of_v<_jobject, std::remove_pointer_t<T>>
					|| std::is_void_v<T>
					|| std::is_arithmetic_v<T>,
					"Return type does not derive from _jobject or is not void or is not an integer/floating point number");

			JNIEnv* env = GetEnv();

			if constexpr (std::is_base_of_v<_jobject, std::remove_pointer_t<T>>) {
				if (isConstructor) { // ctor
					return (T) env->NewObject(clazz, jMethodId, u...);
				} else if (instance == nullptr) { //static
					return (T) env->CallStaticObjectMethod(clazz, jMethodId, u...);
				} else { //instance
					return (T) env->CallObjectMethod(instance, jMethodId, u...);
				}
			} else if constexpr (std::is_same_v<jboolean, T>) {
				if (instance == nullptr) { //static
					return env->CallStaticBooleanMethod(clazz, jMethodId, u...);
				} else { //instance
					return env->CallBooleanMethod(instance, jMethodId, u...);
				}
			} else if constexpr (std::is_same_v<jbyte, T>) {
				if (instance == nullptr) { //static
					return env->CallStaticByteMethod(clazz, jMethodId, u...);
				} else { //instance
					return env->CallByteMethod(instance, jMethodId, u...);
				}
			} else if constexpr (std::is_same_v<jchar, T>) {
				if (instance == nullptr) { //static
					return env->CallStaticCharMethod(clazz, jMethodId, u...);
				} else { //instance
					return env->CallCharMethod(instance, jMethodId, u...);
				}
			} else if constexpr (std::is_same_v<jshort, T>) {
				if (instance == nullptr) { //static
					return env->CallStaticShortMethod(clazz, jMethodId, u...);
				} else { //instance
					return env->CallShortMethod(instance, jMethodId, u...);
				}
			} else if constexpr (std::is_same_v<jint, T>) {
				if (instance == nullptr) { //static
					return env->CallStaticIntMethod(clazz, jMethodId, u...);
				} else { //instance
					return env->CallIntMethod(instance, jMethodId, u...);
				}
			} else if constexpr (std::is_same_v<jlong, T>) {
				if (instance == nullptr) { //static
					return env->CallStaticLongMethod(clazz, jMethodId, u...);
				} else { //instance
					return env->CallLongMethod(instance, jMethodId, u...);
				}
			} else if constexpr (std::is_same_v<jfloat, T>) {
				if (instance == nullptr) { //static
					return env->CallStaticFloatMethod(clazz, jMethodId, u...);
				} else { //instance
					return env->CallFloatMethod(instance, jMethodId, u...);
				}
			} else if constexpr (std::is_same_v<jdouble, T>) {
				if (instance == nullptr) { //static
					return env->CallStaticDoubleMethod(clazz, jMethodId, u...);
				} else { //instance
					return env->CallDoubleMethod(instance, jMethodId, u...);
				}
			} else if constexpr (std::is_void_v<T>) {
				if (instance == nullptr) { //static
					return env->CallStaticVoidMethod(clazz, jMethodId, u...);
				} else { //instance
					return env->CallVoidMethod(instance, jMethodId, u...);
				}
			}

			throw Exception("java.lang.ClassCastException", std::string("Unable to cast to return type: ") + typeid(T).name());
		}
	};
}

#endif //JNIFSHOTS_METHOD_HPP
