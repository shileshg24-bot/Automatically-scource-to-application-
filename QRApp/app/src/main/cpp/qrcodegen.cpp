#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>

#define LOG_TAG "QR_NATIVE_QR"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// External declaration for watermarking
extern void applyWatermarkToPixels(uint32_t* pixels, int width, int height);

// Function to generate the scaled QR pixel array using the JNI callback to Kotlin (which uses ZXing)
jintArray generateQRMatrixCpp(JNIEnv* env, jstring text, jint size, jint eccLevel, jint fgColor, jint bgColor) {
    LOGI("generateQRMatrixCpp called with size=%d, eccLevel=%d", size, eccLevel);

    // Find the helper class
    jclass helperClass = env->FindClass("com/app/qr/QRGeneratorHelper");
    if (!helperClass) {
        LOGE("Failed to find com/app/qr/QRGeneratorHelper class");
        return nullptr;
    }

    // Find the static method
    jmethodID methodId = env->GetStaticMethodID(helperClass, "generateQRMatrix", "(Ljava/lang/String;I)[[Z");
    if (!methodId) {
        LOGE("Failed to find generateQRMatrix static method");
        return nullptr;
    }

    // Call static method to get the boolean[][] matrix
    jobjectArray matrix2D = (jobjectArray)env->CallStaticObjectMethod(helperClass, methodId, text, eccLevel);
    if (!matrix2D) {
        LOGE("Kotlin generateQRMatrix returned null");
        return nullptr;
    }

    int qrSize = env->GetArrayLength(matrix2D);
    if (qrSize == 0) {
        LOGE("QR matrix size is 0");
        return nullptr;
    }

    // Prepare a vector of booleans to represent the QR code matrix
    std::vector<std::vector<bool>> qrMatrix(qrSize, std::vector<bool>(qrSize, false));
    for (int y = 0; y < qrSize; ++y) {
        jbooleanArray row = (jbooleanArray)env->GetObjectArrayElement(matrix2D, y);
        jboolean* elements = env->GetBooleanArrayElements(row, nullptr);
        for (int x = 0; x < qrSize; ++x) {
            qrMatrix[y][x] = elements[x];
        }
        env->ReleaseBooleanArrayElements(row, elements, JNI_ABORT);
        env->DeleteLocalRef(row);
    }
    env->DeleteLocalRef(matrix2D);

    // Now, we will render this qrMatrix of size (qrSize x qrSize) into an output pixel buffer of size (size x size)
    jintArray resultPixels = env->NewIntArray(size * size);
    if (!resultPixels) {
        LOGE("Failed to allocate JNI output pixels array");
        return nullptr;
    }

    uint32_t* pixels = new uint32_t[size * size];

    // Scale the QR code modules to the destination pixel buffer size
    for (int y = 0; y < size; ++y) {
        int qrY = (y * qrSize) / size;
        for (int x = 0; x < size; ++x) {
            int qrX = (x * qrSize) / size;
            bool isBlack = qrMatrix[qrY][qrX];
            pixels[y * size + x] = isBlack ? (uint32_t)fgColor : (uint32_t)bgColor;
        }
    }

    // Apply the watermarking
    applyWatermarkToPixels(pixels, size, size);

    // Copy C++ pixel buffer to the JNI array
    env->SetIntArrayRegion(resultPixels, 0, size * size, (const jint*)pixels);
    delete[] pixels;

    return resultPixels;
}
