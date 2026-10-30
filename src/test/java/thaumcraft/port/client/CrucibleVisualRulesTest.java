package thaumcraft.port.client;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CrucibleVisualRulesTest {
    private static final float EPS = .000001f;

    @Test void pollutionCloudsStartInsideTheBlockAndRiseAtOriginalSpeedForFiveToEightSeconds() {
        var random = RandomSource.create(11226);
        int minimumLife = Integer.MAX_VALUE, maximumLife = 0;
        double horizontalMotion = 0;
        for (int i = 0; i < 2000; i++) {
            var cloud = CrucibleVisualRules.pollution(random);
            for (double coordinate : new double[]{cloud.offset().x, cloud.offset().y, cloud.offset().z})
                assertTrue(coordinate >= .2f && coordinate < .8f);
            assertEquals(.02, cloud.velocity().y);
            assertTrue(Math.abs(cloud.velocity().x) < .005);
            assertTrue(Math.abs(cloud.velocity().z) < .005);
            horizontalMotion += cloud.velocity().x;
            minimumLife = Math.min(minimumLife, cloud.lifetime());
            maximumLife = Math.max(maximumLife, cloud.lifetime());
        }
        assertEquals(100, minimumLife);
        assertEquals(159, maximumLife);
        assertEquals(0, horizontalMotion / 2000, .0002);
    }

    @Test void pollutionKeepsTheOriginalCloudSpritePinkColourGrowthAndFade() {
        assertEquals(16, CrucibleVisualRules.POLLUTION_GRID);
        assertEquals(56, CrucibleVisualRules.POLLUTION_FRAME);
        assertEquals(new CrucibleVisualRules.Colour(1, .3f, .9f), CrucibleVisualRules.POLLUTION_COLOUR);
        assertEquals(.2f, CrucibleVisualRules.POLLUTION_START_SIZE);
        assertEquals(.5f, CrucibleVisualRules.POLLUTION_END_SIZE);
        assertEquals(.5f, CrucibleVisualRules.POLLUTION_ALPHA);
        assertEquals(1, CrucibleVisualRules.POLLUTION_SLOWDOWN);
        assertEquals(.001, CrucibleVisualRules.POLLUTION_WIND);
    }

    @Test void contentsModulateOriginalBlueWaterInsteadOfBiomeColour() {
        assertEquals(new CrucibleVisualRules.Colour(1,1,1), CrucibleVisualRules.fluidColour(0));
        var one = CrucibleVisualRules.fluidColour(1);
        assertEquals(1 - .501f / 3, one.red(), EPS);
        assertEquals(.499f, one.green(), EPS);
        assertEquals(.7495f, one.blue(), EPS);
        var full = CrucibleVisualRules.fluidColour(500);
        assertEquals(2f / 3, full.red(), EPS);
        assertEquals(0, full.green(), EPS);
        assertEquals(.5f, full.blue(), EPS);
        assertEquals(full, CrucibleVisualRules.fluidColour(501));
        assertEquals(full, CrucibleVisualRules.fluidColour(100000));
    }

    @Test void frothAndOverflowHaveOriginalSizeGravityAgeAndBlend() {
        var random = RandomSource.create(11226);
        for (int i = 0; i < 100; i++) {
            var froth = CrucibleVisualRules.bubble(random, CrucibleVisualRules.Kind.FROTH, 1);
            assertTrue(froth.lifetime() >= 4 && froth.lifetime() <= 6);
            assertTrue(froth.halfSize() >= .02f && froth.halfSize() < .04f);
            assertEquals(.1f, froth.gravity());
            assertEquals(64, froth.firstFrame());
            assertArrayEquals(new int[]{65,66}, froth.finalFrames());
            assertTrue(froth.additive());
            assertTrue(froth.collision());
            var overflow = CrucibleVisualRules.bubble(random, CrucibleVisualRules.Kind.OVERFLOW, 1);
            assertTrue(overflow.lifetime() >= 12 && overflow.lifetime() <= 23);
            assertTrue(overflow.halfSize() >= .04f && overflow.halfSize() < .06f);
            assertEquals(.05f, overflow.gravity());
            assertEquals(73, overflow.firstFrame());
            assertFalse(overflow.additive());
            assertFalse(overflow.collision(), "Overflow must pass through the rim and run down the sides");
            assertEquals(.8f, overflow.alpha());
        }
    }

    @Test void emptyingBubblesRiseFiveTimesAsFastAsDissolvingBubbles() {
        var dissolving = CrucibleVisualRules.bubble(RandomSource.create(52), CrucibleVisualRules.Kind.BOIL, 1);
        var emptying = CrucibleVisualRules.bubble(RandomSource.create(52), CrucibleVisualRules.Kind.BOIL, 5);
        assertEquals(dissolving.lifetime(), emptying.lifetime());
        assertEquals(dissolving.halfSize(), emptying.halfSize());
        assertEquals(dissolving.gravity() * 5, emptying.gravity());
        assertEquals(-.125f, emptying.gravity());
        assertTrue(dissolving.collision());
        assertTrue(emptying.collision());
        for (int i = 0; i < 100; i++) {
            var ambient = CrucibleVisualRules.bubble(RandomSource.create(i), CrucibleVisualRules.Kind.BUBBLE, 1);
            assertTrue(ambient.lifetime() >= 15 && ambient.lifetime() <= 24);
            assertTrue(ambient.halfSize() >= .03f && ambient.halfSize() < .06f);
            assertArrayEquals(new int[]{65,66,66}, ambient.finalFrames());
            assertEquals(-.001f, ambient.gravity());
            assertTrue(ambient.collision());
        }
    }

    @Test void bubblePopFramesPreserveOriginalRemainingLifetimeOrdering() {
        int[] pop = {65,66,66};
        assertEquals(64, CrucibleVisualRules.frame(64,pop,16,20));
        assertEquals(64, CrucibleVisualRules.frame(64,pop,17,20));
        assertEquals(66, CrucibleVisualRules.frame(64,pop,18,20));
        assertEquals(66, CrucibleVisualRules.frame(64,pop,19,20));
        assertEquals(65, CrucibleVisualRules.frame(64,pop,20,20));
        assertEquals(65, CrucibleVisualRules.frame(64,pop,21,20));
    }

    @Test void legacyWaterIsUnmodifiedAnimatedAndExplicitlyIncludedInBlockAtlas() throws Exception {
        var root = Path.of("src/main/resources/assets");
        var texture = root.resolve("thaumcraft/textures/block/crucible_water.png");
        assertEquals("2fef429515591b671525477e886da265595027b288b771cac185fa331b0c52ba",
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(texture))));
        var animation = JsonParser.parseString(Files.readString(Path.of(texture + ".mcmeta")))
                .getAsJsonObject().getAsJsonObject("animation");
        assertEquals(2, animation.get("frametime").getAsInt());
        var sources = JsonParser.parseString(Files.readString(root.resolve("minecraft/atlases/blocks.json")))
                .getAsJsonObject().getAsJsonArray("sources");
        assertTrue(sources.asList().stream().map(element -> element.getAsJsonObject()).anyMatch(source ->
                source.has("resource") && source.get("resource").getAsString().equals("thaumcraft:block/crucible_water")
                        && source.get("type").getAsString().equals("minecraft:single")));
    }
}
