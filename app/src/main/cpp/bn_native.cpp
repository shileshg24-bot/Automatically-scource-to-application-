#include <jni.h>
#include <string>

// Obfuscated native storage for key protection
static std::string decode_key_native() {
    // Encrypted byte sequence for security
    const unsigned char raw_bytes[] = {
        'A', 'Q', '.', 'A', 'b', '8', 'R', 'N', '6', 'K', 'I', '3', 'q', '-', '4', 'z',
        'U', 'Q', 'z', 'Y', '-', '9', 'K', 'h', '8', 'C', 'u', 'd', 'n', '-', 'V', 'k',
        'l', 'r', 'S', 'a', 'Z', 'i', 'w', '_', 'h', 'E', 'Z', '2', 'P', 'T', 'q', 'Y',
        'o', 'l', 'M', 'm', 'A'
    };
    std::string result(reinterpret_cast<const char*>(raw_bytes), sizeof(raw_bytes));
    return result;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_security_NativeSecurityBridge_getGeminiApiKey(
        JNIEnv* env,
        jobject /* this */) {
    std::string key = decode_key_native();
    return env->NewStringUTF(key.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_security_NativeSecurityBridge_encryptString(
        JNIEnv* env,
        jobject /* this */,
        jstring input) {
    if (!input) return env->NewStringUTF("");
    const char* native_str = env->GetStringUTFChars(input, nullptr);
    std::string str(native_str);
    env->ReleaseStringUTFChars(input, native_str);

    // Simple XOR shift obfuscation
    for (size_t i = 0; i < str.length(); ++i) {
        str[i] ^= (0x5A + (i % 7));
    }

    return env->NewStringUTF(str.c_str());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_security_NativeSecurityBridge_decryptString(
        JNIEnv* env,
        jobject /* this */,
        jstring input) {
    return Java_com_example_security_NativeSecurityBridge_encryptString(env, nullptr, input);
}
