import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;

/** Run outside the game with the real mapped 1.21.1 client classes and LWJGL natives.
 * Arguments: minecraft.jar mod.jar expected-sprite-ids.txt
 * Reads the actual vanilla atlas definition and decodes its PNGs through Minecraft.
 * Does not create a client window, a server, a world, or an OpenGL context.
 */
public final class AtlasSmokeTest {
    private static PackResources pack(String name, String file) {
        var info = new PackLocationInfo(name, Component.literal(name), PackSource.BUILT_IN, Optional.empty());
        return new FilePackResources.FileResourcesSupplier(Path.of(file)).openPrimary(info);
    }

    public static void main(String[] args) throws Exception {
        Set<String> expected = new HashSet<>(Files.readAllLines(Path.of(args[2])));
        Set<String> loaded = new HashSet<>();
        try (var resources = new MultiPackResourceManager(PackType.CLIENT_RESOURCES,
                List.of(pack("minecraft", args[0]), pack("thaumcraft", args[1])))) {
            var atlas = SpriteSourceList.load(resources, ResourceLocation.parse(args.length>3?args[3]:"minecraft:blocks"));
            var loader = SpriteResourceLoader.create(List.of(AnimationMetadataSection.SERIALIZER));
            int decoded = 0;
            for (var supplier : atlas.list(resources)) {
                try (SpriteContents sprite = supplier.apply(loader)) {
                    if (sprite == null) continue;
                    decoded++;
                    if (sprite.name().getNamespace().equals("thaumcraft")) {
                        if (sprite.width() <= 0 || sprite.height() <= 0) throw new AssertionError("Invalid frame size");
                        loaded.add(sprite.name().toString());
                    }
                }
            }
            var missing = new java.util.TreeSet<>(expected);
            missing.removeAll(loaded);
            System.out.println("Decoded atlas sprites: " + decoded);
            System.out.println("Thaumcraft sprites loaded: " + loaded.size() + "; required: " + expected.size());
            if (!missing.isEmpty()) throw new AssertionError("Missing from Minecraft atlas: " + missing);
            System.out.println("PASS: all referenced Thaumcraft sprites discovered and decoded by Minecraft 1.21.1");
        }
    }
}
