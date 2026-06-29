#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <algorithm>

#define LOG_TAG "QR_NATIVE_IMG"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// External declaration for watermarking
extern void applyWatermarkToPixels(uint32_t* pixels, int width, int height);

// Helper function to get the QR Matrix from Kotlin JNI callback
std::vector<std::vector<bool>> getQRMatrixFromKotlin(JNIEnv* env, jstring text) {
    std::vector<std::vector<bool>> emptyMatrix;
    jclass helperClass = env->FindClass("com/app/qr/QRGeneratorHelper");
    if (!helperClass) {
        LOGE("Failed to find QRGeneratorHelper class");
        return emptyMatrix;
    }
    jmethodID methodId = env->GetStaticMethodID(helperClass, "generateQRMatrix", "(Ljava/lang/String;I)[[Z");
    if (!methodId) {
        LOGE("Failed to find generateQRMatrix method");
        return emptyMatrix;
    }
    // Default ecc level 1 (M)
    jobjectArray matrix2D = (jobjectArray)env->CallStaticObjectMethod(helperClass, methodId, text, 1);
    if (!matrix2D) {
        LOGE("Kotlin generateQRMatrix returned null");
        return emptyMatrix;
    }
    int qrSize = env->GetArrayLength(matrix2D);
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
    return qrMatrix;
}

// JNI Implementation for blending QR on background image
extern "C" JNIEXPORT jintArray JNICALL
Java_com_app_qr_ImageProcessor_blendQR(JNIEnv* env, jobject thiz,
                                      jintArray bgPixels, jint bgWidth, jint bgHeight,
                                      jstring qrText, jint qrSize,
                                      jint posX, jint posY, jfloat opacity,
                                      jint fgColor, jint bgColor) {
    LOGI("blendQR called with bgWidth=%d, bgHeight=%d, qrSize=%d, posX=%d, posY=%d, opacity=%f",
         bgWidth, bgHeight, qrSize, posX, posY, opacity);

    // Get background pixels
    jsize len = env->GetArrayLength(bgPixels);
    std::vector<uint32_t> pixels(len);
    env->GetIntArrayRegion(bgPixels, 0, len, (jint*)pixels.data());

    // Generate QR matrix
    std::vector<std::vector<bool>> qrMatrix = getQRMatrixFromKotlin(env, qrText);
    int qrMatrixSize = qrMatrix.size();
    if (qrMatrixSize == 0) {
        LOGE("Failed to generate QR Matrix for blending");
        return bgPixels;
    }

    // Blend QR pixels onto background pixels at position (posX, posY)
    for (int dy = 0; dy < qrSize; ++dy) {
        int py = posY + dy;
        if (py < 0 || py >= bgHeight) continue;

        // Map dy to QR matrix row index
        int qrY = (dy * qrMatrixSize) / qrSize;

        for (int dx = 0; dx < qrSize; ++dx) {
            int px = posX + dx;
            if (px < 0 || px >= bgWidth) continue;

            // Map dx to QR matrix col index
            int qrX = (dx * qrMatrixSize) / qrSize;

            bool isBlack = qrMatrix[qrY][qrX];
            uint32_t qrPixelColor = isBlack ? (uint32_t)fgColor : (uint32_t)bgColor;

            // Blend based on opacity
            int idx = py * bgWidth + px;
            uint32_t bgCol = pixels[idx];

            uint8_t bgA = (bgCol >> 24) & 0xFF;
            uint8_t bgR = (bgCol >> 16) & 0xFF;
            uint8_t bgG = (bgCol >> 8) & 0xFF;
            uint8_t bgB = bgCol & 0xFF;

            uint8_t qrA = (qrPixelColor >> 24) & 0xFF;
            uint8_t qrR = (qrPixelColor >> 16) & 0xFF;
            uint8_t qrG = (qrPixelColor >> 8) & 0xFF;
            uint8_t qrB = qrPixelColor & 0xFF;

            // Alpha blending formula
            float alpha = opacity;
            uint8_t outR = (uint8_t)(qrR * alpha + bgR * (1.0f - alpha));
            uint8_t outG = (uint8_t)(qrG * alpha + bgG * (1.0f - alpha));
            uint8_t outB = (uint8_t)(qrB * alpha + bgB * (1.0f - alpha));

            pixels[idx] = (0xFF << 24) | (outR << 16) | (outG << 8) | outB;
        }
    }

    // Apply the mandatory watermark on top-left of final composited image
    applyWatermarkToPixels(pixels.data(), bgWidth, bgHeight);

    // Return the final composited image
    jintArray result = env->NewIntArray(len);
    env->SetIntArrayRegion(result, 0, len, (const jint*)pixels.data());
    return result;
}

// JNI Implementation for LSB Steganography (Subtle +15 / -15 Delta QR embedding)
extern "C" JNIEXPORT jintArray JNICALL
Java_com_app_qr_ImageProcessor_hideQR(JNIEnv* env, jobject thiz,
                                      jintArray bgPixels, jint bgWidth, jint bgHeight,
                                      jstring qrText) {
    LOGI("hideQR called for steganography on bgWidth=%d, bgHeight=%d", bgWidth, bgHeight);

    jsize len = env->GetArrayLength(bgPixels);
    std::vector<uint32_t> pixels(len);
    env->GetIntArrayRegion(bgPixels, 0, len, (jint*)pixels.data());

    // Generate QR matrix
    std::vector<std::vector<bool>> qrMatrix = getQRMatrixFromKotlin(env, qrText);
    int qrMatrixSize = qrMatrix.size();
    if (qrMatrixSize == 0) {
        LOGE("Failed to generate QR Matrix for steganography");
        return bgPixels;
    }

    // Embed QR subtly in the entire background image
    for (int y = 0; y < bgHeight; ++y) {
        int qrY = (y * qrMatrixSize) / bgHeight;
        for (int x = 0; x < bgWidth; ++x) {
            int qrX = (x * qrMatrixSize) / bgWidth;

            bool isBlack = qrMatrix[qrY][qrX];
            int idx = y * bgWidth + x;
            uint32_t bgCol = pixels[idx];

            uint8_t a = (bgCol >> 24) & 0xFF;
            int r = (bgCol >> 16) & 0xFF;
            int g = (bgCol >> 8) & 0xFF;
            int b = bgCol & 0xFF;

            // Apply +15 / -15 delta
            if (isBlack) {
                r = std::max(0, r - 15);
                g = std::max(0, g - 15);
                b = std::max(0, b - 15);
            } else {
                r = std::min(255, r + 15);
                g = std::min(255, g + 15);
                b = std::min(255, b + 15);
            }

            pixels[idx] = (a << 24) | (r << 16) | (g << 8) | b;
        }
    }

    // Apply the mandatory watermark
    applyWatermarkToPixels(pixels.data(), bgWidth, bgHeight);

    jintArray result = env->NewIntArray(len);
    env->SetIntArrayRegion(result, 0, len, (const jint*)pixels.data());
    return result;
}

// JNI Implementation for dynamic transparency / opacity adjustment
extern "C" JNIEXPORT jintArray JNICALL
Java_com_app_qr_ImageProcessor_adjustOpacity(JNIEnv* env, jobject thiz,
                                            jintArray qrPixels, jint width, jint height,
                                            jfloat opacityLevel) {
    LOGI("adjustOpacity called with opacityLevel=%f", opacityLevel);

    jsize len = env->GetArrayLength(qrPixels);
    std::vector<uint32_t> pixels(len);
    env->GetIntArrayRegion(qrPixels, 0, len, (jint*)pixels.data());

    // Adjust opacity dynamically without re-rendering QR layout
    for (int i = 0; i < len; ++i) {
        uint32_t col = pixels[i];
        uint8_t r = (col >> 16) & 0xFF;
        uint8_t g = (col >> 8) & 0xFF;
        uint8_t b = col & 0xFF;
        uint8_t currentAlpha = (col >> 24) & 0xFF;

        // Apply opacityLevel to alpha channel
        uint8_t newAlpha = (uint8_t)(currentAlpha * opacityLevel);
        pixels[i] = (newAlpha << 24) | (r << 16) | (g << 8) | b;
    }

    jintArray result = env->NewIntArray(len);
    env->SetIntArrayRegion(result, 0, len, (const jint*)pixels.data());
    return result;
}
