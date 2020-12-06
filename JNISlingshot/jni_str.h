
#ifndef JNISLINGSHOT_JNI_STR_H
#define JNISLINGSHOT_JNI_STR_H


class jni_str {
private:
	JNIEnv* env;
	const char* str;
	jstring _jstring;
	jboolean isCopy;
public:
	jni_str(JNIEnv* env, jstring jstring) : env(env), _jstring(jstring) {
		isCopy = false; //clang-tidy dum thinks isCopy is not initialized but it is by the line below
		str = env->GetStringUTFChars(jstring, &isCopy);
	}

	~jni_str() {
		if (isCopy == JNI_TRUE) env->ReleaseStringUTFChars(_jstring, str);
	}

	const char* data() {
		return str;
	}
};


#endif //JNISLINGSHOT_JNI_STR_H
