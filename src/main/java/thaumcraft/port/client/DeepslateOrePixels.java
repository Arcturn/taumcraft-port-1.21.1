package thaumcraft.port.client;

import java.util.List;

/** Keeps the TC6 ore pixels while taking every exposed stone pixel from the active deepslate texture. */
final class DeepslateOrePixels {
    static final int MASK_SIZE = 16;

    static void validateMask(List<String> mask) {
        if (mask.size() != MASK_SIZE || mask.stream().anyMatch(row -> row.length() != MASK_SIZE
                || row.chars().anyMatch(pixel -> pixel != '.' && pixel != '#'))) {
            throw new IllegalArgumentException("Ore mask must contain sixteen rows of sixteen '.' or '#' pixels");
        }
    }

    static int[] compose(int baseSize, int[] base, int oreSize, int[] ore, List<String> mask) {
        validateMask(mask);
        if (baseSize < 1 || oreSize < 1 || base.length != baseSize * baseSize || ore.length != oreSize * oreSize) {
            throw new IllegalArgumentException("Deepslate and ore textures must be square");
        }
        int size = Math.max(baseSize, oreSize);
        int[] pixels = new int[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean inclusion = mask.get(y * MASK_SIZE / size).charAt(x * MASK_SIZE / size) == '#';
                int sourceSize = inclusion ? oreSize : baseSize;
                int[] source = inclusion ? ore : base;
                pixels[y * size + x] = source[(y * sourceSize / size) * sourceSize + x * sourceSize / size];
            }
        }
        return pixels;
    }

    /** Extracts one metadata-sized frame from a vertical, horizontal or tiled animation sheet. */
    static int[] frame(int width, int height, int[] pixels, int frameWidth, int frameHeight, int index) {
        if (width < 1 || height < 1 || frameWidth < 1 || frameHeight < 1
                || pixels.length != width * height || width % frameWidth != 0 || height % frameHeight != 0
                || index < 0 || index >= (width / frameWidth) * (height / frameHeight)) {
            throw new IllegalArgumentException("Invalid ore animation frame or sheet dimensions");
        }
        int size = Math.max(frameWidth, frameHeight);
        int originX = (index % (width / frameWidth)) * frameWidth;
        int originY = (index / (width / frameWidth)) * frameHeight;
        int[] frame = new int[size * size];
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            frame[y * size + x] = pixels[(originY + y * frameHeight / size) * width + originX + x * frameWidth / size];
        }
        return frame;
    }

    private DeepslateOrePixels() {}
}
