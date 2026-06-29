#include <stdint.h>
#include <string.h>
#include <algorithm>

// 5x7 Font bitmaps for the 16 unique characters in "TG- @BLOODYNIGHTMODMENU"
static const uint8_t* getCharPattern(char c) {
    static const uint8_t space[] = {0x00, 0x00, 0x00, 0x00, 0x00};
    static const uint8_t T_pat[] = {0x01, 0x01, 0x7F, 0x01, 0x01};
    static const uint8_t G_pat[] = {0x3E, 0x41, 0x49, 0x49, 0x3A};
    static const uint8_t dash[]  = {0x08, 0x08, 0x08, 0x08, 0x08};
    static const uint8_t at_pat[] = {0x32, 0x49, 0x79, 0x41, 0x3E};
    static const uint8_t B_pat[] = {0x7F, 0x49, 0x49, 0x49, 0x36};
    static const uint8_t L_pat[] = {0x7F, 0x40, 0x40, 0x40, 0x40};
    static const uint8_t O_pat[] = {0x3E, 0x41, 0x41, 0x41, 0x3E};
    static const uint8_t D_pat[] = {0x7F, 0x41, 0x41, 0x22, 0x1C};
    static const uint8_t Y_pat[] = {0x03, 0x04, 0x78, 0x04, 0x03};
    static const uint8_t N_pat[] = {0x7F, 0x04, 0x08, 0x10, 0x7F};
    static const uint8_t I_pat[] = {0x41, 0x41, 0x7F, 0x41, 0x41};
    static const uint8_t H_pat[] = {0x7F, 0x08, 0x08, 0x08, 0x7F};
    static const uint8_t M_pat[] = {0x7F, 0x02, 0x0C, 0x02, 0x7F};
    static const uint8_t E_pat[] = {0x7F, 0x49, 0x49, 0x49, 0x41};
    static const uint8_t U_pat[] = {0x3F, 0x40, 0x40, 0x40, 0x3F};
    
    switch (c) {
        case 'T': return T_pat;
        case 'G': return G_pat;
        case '-': return dash;
        case ' ': return space;
        case '@': return at_pat;
        case 'B': return B_pat;
        case 'L': return L_pat;
        case 'O': return O_pat;
        case 'D': return D_pat;
        case 'Y': return Y_pat;
        case 'N': return N_pat;
        case 'I': return I_pat;
        case 'H': return H_pat;
        case 'M': return M_pat;
        case 'E': return E_pat;
        case 'U': return U_pat;
        default: return space;
    }
}

// Function to draw text in C++ onto an ARGB pixel buffer with scaling and opacity
void drawWatermarkString(uint32_t* pixelBuffer, int imgWidth, int imgHeight, const char* str, int startX, int startY, int charHeight, uint32_t color, float opacity) {
    int scale = charHeight / 7;
    if (scale < 1) scale = 1;
    
    int currentX = startX;
    while (*str) {
        char c = *str;
        const uint8_t* pattern = getCharPattern(c);
        
        // Render characters pixel-by-pixel
        for (int col = 0; col < 5; ++col) {
            uint8_t byte = pattern[col];
            for (int row = 0; row < 7; ++row) {
                if ((byte >> row) & 1) {
                    for (int dy = 0; dy < scale; ++dy) {
                        for (int dx = 0; dx < scale; ++dx) {
                            int px = currentX + col * scale + dx;
                            int py = startY + row * scale + dy;
                            if (px >= 0 && px < imgWidth && py >= 0 && py < imgHeight) {
                                int idx = py * imgWidth + px;
                                uint32_t bgPixel = pixelBuffer[idx];
                                
                                // Blend the color (0xFFFFFF white) with the background
                                uint8_t bgR = (bgPixel >> 16) & 0xFF;
                                uint8_t bgG = (bgPixel >> 8) & 0xFF;
                                uint8_t bgB = bgPixel & 0xFF;
                                
                                uint8_t fgR = (color >> 16) & 0xFF;
                                uint8_t fgG = (color >> 8) & 0xFF;
                                uint8_t fgB = color & 0xFF;
                                
                                uint8_t outR = (uint8_t)(fgR * opacity + bgR * (1.0f - opacity));
                                uint8_t outG = (uint8_t)(fgG * opacity + bgG * (1.0f - opacity));
                                uint8_t outB = (uint8_t)(fgB * opacity + bgB * (1.0f - opacity));
                                
                                pixelBuffer[idx] = (0xFF << 24) | (outR << 16) | (outG << 8) | outB;
                            }
                        }
                    }
                }
            }
        }
        currentX += 6 * scale; // 5 columns + 1 pixel gap scaled
        str++;
    }
}

// Applies the mandatory watermark: "TG- @BLOODYNIGHTMODMENU"
void applyWatermarkToPixels(uint32_t* pixels, int width, int height) {
    const char* WATERMARK = "TG- @BLOODYNIGHTMODMENU";
    
    // Text size must be 5% of the image width
    int charHeight = (int)(width * 0.05f);
    if (charHeight < 8) charHeight = 8; // safeguard min height
    
    // Position it in the top-left with some margin
    int margin = (int)(width * 0.03f);
    if (margin < 5) margin = 5;
    
    // Render 80% opacity white (0xFFFFFF)
    drawWatermarkString(pixels, width, height, WATERMARK, margin, margin, charHeight, 0xFFFFFF, 0.80f);
}
