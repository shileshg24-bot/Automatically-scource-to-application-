#include <jni.h>
#include <string>

// External declaration from qrcodegen.cpp
extern jintArray generateQRMatrixCpp(JNIEnv* env, jstring text, jint size, jint eccLevel, jint fgColor, jint bgColor);

extern "C" JNIEXPORT jintArray JNICALL
Java_com_app_qr_QRGenerator_generateQR(JNIEnv* env, jobject thiz,
                                       jstring text, jint size, jint eccLevel,
                                       jint fgColor, jint bgColor) {
    return generateQRMatrixCpp(env, text, size, eccLevel, fgColor, bgColor);
}
