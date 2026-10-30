package thaumcraft.port.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;
import thaumcraft.port.ThaumcraftPort;

/**
 * Atlas-time composition: vanilla deepslate stays exact and follows the selected resource pack.
 * The atlas masks select pixels that differ between the original TC6 ores and their vanilla 1.12.2
 * stone substrate, preserving both the mineral inclusions and their original recessed edges.
 */
@EventBusSubscriber(modid = ThaumcraftPort.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public record DeepslateOreSpriteSource(ResourceLocation sprite, ResourceLocation base,
                                      ResourceLocation ore, List<String> mask) implements SpriteSource {
    public static final MapCodec<DeepslateOreSpriteSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("sprite").forGetter(DeepslateOreSpriteSource::sprite),
            ResourceLocation.CODEC.fieldOf("base").forGetter(DeepslateOreSpriteSource::base),
            ResourceLocation.CODEC.fieldOf("ore").forGetter(DeepslateOreSpriteSource::ore),
            Codec.STRING.listOf().fieldOf("mask").forGetter(DeepslateOreSpriteSource::mask)
    ).apply(instance, DeepslateOreSpriteSource::new));
    public static final SpriteSourceType TYPE = new SpriteSourceType(CODEC);

    public DeepslateOreSpriteSource {
        mask = List.copyOf(mask);
        DeepslateOrePixels.validateMask(mask);
    }

    @SubscribeEvent public static void register(RegisterSpriteSourceTypesEvent event) {
        event.register(ResourceLocation.fromNamespaceAndPath("thaumcraft", "deepslate_ore"), TYPE);
    }

    @Override public void run(ResourceManager resources, Output output) {
        // Resource packs may provide a complete custom ore texture instead of the generated fallback.
        var override = resources.getResource(TEXTURE_ID_CONVERTER.idToFile(sprite));
        if (override.isPresent()) {
            output.add(sprite, override.get());
            return;
        }
        output.add(sprite, loader -> {
            try (var baseSprite = loader.loadSprite(base, resources.getResourceOrThrow(TEXTURE_ID_CONVERTER.idToFile(base)));
                 var oreSprite = loader.loadSprite(ore, resources.getResourceOrThrow(TEXTURE_ID_CONVERTER.idToFile(ore)))) {
                if (baseSprite == null || oreSprite == null) return null;
                int baseSize = Math.max(baseSprite.width(), baseSprite.height());
                int oreSize = Math.max(oreSprite.width(), oreSprite.height());
                int size = Math.max(baseSize, oreSize);
                // Preserve one input's complete animation. If both animate, keep the background's
                // timeline and hold the veins at their first declared valid frame.
                var animated = hasAnimation(baseSprite) ? baseSprite : hasAnimation(oreSprite) ? oreSprite : null;
                int frames = animated == null ? 1 : frameCount(animated);
                var metadata = animated == null ? ResourceMetadata.EMPTY : animated.metadata();
                int[] basePixels = baseSprite.getOriginalImage().getPixelsRGBA();
                int[] orePixels = oreSprite.getOriginalImage().getPixelsRGBA();
                int firstBaseFrame = firstFrame(baseSprite);
                int firstOreFrame = firstFrame(oreSprite);
                var result = new NativeImage(size, size * frames, false);
                boolean transferred = false;
                try {
                    for (int frame = 0; frame < frames; frame++) {
                        int[] pixels = DeepslateOrePixels.compose(baseSize,
                                frame(baseSprite, basePixels, animated == baseSprite ? frame : firstBaseFrame),
                                oreSize, frame(oreSprite, orePixels, animated == oreSprite ? frame : firstOreFrame), mask);
                        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
                            result.setPixelRGBA(x, frame * size + y, pixels[y * size + x]);
                        }
                    }
                    var contents = new SpriteContents(sprite, new FrameSize(size, size), result, metadata);
                    transferred = true;
                    return contents;
                } finally {
                    if (!transferred) result.close();
                }
            } catch (IOException | IllegalArgumentException exception) {
                LogUtils.getLogger().error("Unable to compose deepslate ore sprite {}", sprite, exception);
                return null;
            }
        });
    }

    private static int frameCount(SpriteContents sprite) {
        return (sprite.getOriginalImage().getWidth() / sprite.width()) * (sprite.getOriginalImage().getHeight() / sprite.height());
    }

    private static boolean hasAnimation(SpriteContents sprite) {
        int[] counts = {0, 0}; // Declared frames, then valid timeline entries (matching SpriteContents).
        sprite.metadata().getSection(AnimationMetadataSection.SERIALIZER).ifPresent(animation ->
                animation.forEachFrame((index, time) -> {
                    counts[0]++;
                    if (index >= 0 && index < frameCount(sprite) && time > 0) counts[1]++;
                }));
        return counts[0] == 0 ? frameCount(sprite) > 1 : counts[1] > 1;
    }

    private static int firstFrame(SpriteContents sprite) {
        int[] first = {-1};
        sprite.metadata().getSection(AnimationMetadataSection.SERIALIZER).ifPresent(animation ->
                animation.forEachFrame((index, time) -> {
                    if (first[0] == -1 && index >= 0 && index < frameCount(sprite) && time > 0) first[0] = index;
                }));
        return Math.max(0, first[0]);
    }

    private static int[] frame(SpriteContents sprite, int[] pixels, int index) {
        var image = sprite.getOriginalImage();
        return DeepslateOrePixels.frame(image.getWidth(), image.getHeight(), pixels, sprite.width(), sprite.height(), index);
    }

    @Override public SpriteSourceType type() { return TYPE; }
}
