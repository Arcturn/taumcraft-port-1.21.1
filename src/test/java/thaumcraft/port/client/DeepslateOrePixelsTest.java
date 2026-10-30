package thaumcraft.port.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeepslateOrePixelsTest {
    private static List<JsonObject> definitions() throws Exception {
        try (var stream = DeepslateOrePixelsTest.class.getResourceAsStream("/assets/minecraft/atlases/blocks.json")) {
            assertNotNull(stream);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject()
                    .getAsJsonArray("sources").asList().stream().map(element -> element.getAsJsonObject())
                    .filter(source -> source.get("type").getAsString().equals("thaumcraft:deepslate_ore")).toList();
        }
    }

    @Test void everyExposedPixelComesFromTheBaseAndEveryVeinPixelKeepsOriginalArt() throws Exception {
        var definitions = definitions();
        assertEquals(3, definitions.size());
        for (var definition : definitions) {
            var mask = definition.getAsJsonArray("mask").asList().stream().map(row -> row.getAsString()).toList();
            String texture = definition.get("ore").getAsString().replace(':', '/');
            try (var stream = getClass().getResourceAsStream("/assets/" + texture.replace("/block/", "/textures/block/") + ".png")) {
                assertNotNull(stream, texture);
                var image = ImageIO.read(stream);
                assertEquals(16, image.getWidth());
                assertEquals(16, image.getHeight());
                int[] veins = image.getRGB(0, 0, 16, 16, null, 0, 16);
                int[] base = new int[256];
                // A unique value at every position detects any shift, flip, tint or replacement pattern.
                for (int i = 0; i < base.length; i++) base[i] = 0xff105000 | i;
                int[] result = DeepslateOrePixels.compose(16, base, 16, veins, mask);
                int selected = 0;
                for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                    int index = y * 16 + x;
                    boolean isVein = mask.get(y).charAt(x) == '#';
                    assertEquals(isVein ? veins[index] : base[index], result[index], texture + " at " + x + "," + y);
                    if (isVein) selected++;
                }
                int expected = texture.endsWith("amber") ? 104 : texture.endsWith("cinnabar") ? 71 : 160;
                assertEquals(expected, selected, "TC6 vein/shadow footprint changed");
            }
        }
    }

    @Test void higherResolutionBaseRetainsItsPixelsWhileOriginalVeinsUseNearestSampling() throws Exception {
        var mask = definitions().getFirst().getAsJsonArray("mask").asList().stream().map(row -> row.getAsString()).toList();
        int[] base = new int[32 * 32], ore = new int[16 * 16];
        for (int i = 0; i < base.length; i++) base[i] = 0xff001000 + i;
        for (int i = 0; i < ore.length; i++) ore[i] = 0xff002000 + i;
        int[] result = DeepslateOrePixels.compose(32, base, 16, ore, mask);
        assertEquals(1024, result.length);
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) {
            boolean isVein = mask.get(y / 2).charAt(x / 2) == '#';
            assertEquals(isVein ? ore[(y / 2) * 16 + x / 2] : base[y * 32 + x], result[y * 32 + x]);
        }
    }

    @Test void malformedMaskOrDimensionsAreRejectedBeforeSampling() {
        var valid = Collections.nCopies(16, "................");
        assertThrows(IllegalArgumentException.class, () -> DeepslateOrePixels.compose(16, new int[255], 16, new int[256], valid));
        assertThrows(IllegalArgumentException.class, () -> DeepslateOrePixels.compose(0, new int[0], 16, new int[256], valid));
        assertThrows(IllegalArgumentException.class, () -> DeepslateOrePixels.validateMask(List.of("................")));
        var invalid = new java.util.ArrayList<>(valid);
        invalid.set(3, "........?.......");
        assertThrows(IllegalArgumentException.class, () -> DeepslateOrePixels.validateMask(invalid));
    }

    @Test void verticalAnimationComposesEachFrameWithTheOtherTextureHeldStatic() throws Exception {
        var mask = definitions().getFirst().getAsJsonArray("mask").asList().stream().map(row -> row.getAsString()).toList();
        int[] sheet = new int[16 * 48], still = new int[16 * 16];
        for (int i = 0; i < sheet.length; i++) sheet[i] = 0xff101000 + i;
        for (int i = 0; i < still.length; i++) still[i] = 0xff202000 + i;
        for (int frame = 0; frame < 3; frame++) {
            var animated = DeepslateOrePixels.frame(16, 48, sheet, 16, 16, frame);
            var animatedBase = DeepslateOrePixels.compose(16, animated, 16, still, mask);
            var animatedOre = DeepslateOrePixels.compose(16, still, 16, animated, mask);
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                int index = y * 16 + x;
                boolean isVein = mask.get(y).charAt(x) == '#';
                assertEquals(isVein ? still[index] : sheet[frame * 256 + index], animatedBase[index]);
                assertEquals(isVein ? sheet[frame * 256 + index] : still[index], animatedOre[index]);
            }
        }
    }

    @Test void tiledAndRectangularFramesUseMetadataDimensions() {
        int[] pixels = new int[32 * 32];
        for (int i = 0; i < pixels.length; i++) pixels[i] = i;
        for (int index = 0; index < 4; index++) {
            var frame = DeepslateOrePixels.frame(32, 32, pixels, 16, 16, index);
            for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
                assertEquals((index / 2 * 16 + y) * 32 + index % 2 * 16 + x, frame[y * 16 + x]);
            }
        }
        var rectangular = DeepslateOrePixels.frame(32, 32, pixels, 8, 16, 5);
        assertEquals(256, rectangular.length);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            assertEquals((16 + y) * 32 + 8 + x / 2, rectangular[y * 16 + x]);
        }
        assertThrows(IllegalArgumentException.class, () -> DeepslateOrePixels.frame(32, 32, pixels, 16, 17, 0));
        assertThrows(IllegalArgumentException.class, () -> DeepslateOrePixels.frame(32, 32, pixels, 16, 16, 4));
    }
}
