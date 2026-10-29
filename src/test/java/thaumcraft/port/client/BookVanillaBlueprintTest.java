package thaumcraft.port.client;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.port.research.ResearchCatalog;

import static org.junit.jupiter.api.Assertions.*;

class BookVanillaBlueprintTest {
    private static final String PREFIX = "thaumcraft:research/vanilla/";
    private static final Set<String> ROOT_MODELS = Set.of("nether_brick", "obsidian", "cauldron_empty",
            "anvil_undamaged", "piston_normal", "iron_bars_post_ends", "iron_bars_post", "iron_bars_cap",
            "iron_bars_cap_alt", "iron_bars_side", "iron_bars_side_alt");

    @BeforeAll static void bootstrapVanillaStates() {
        if (LoadingModList.get() == null) LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void everyOriginalVanillaBlueprintCellUsesAnExplicitBookOnlyModel() {
        var recipes = ResearchCatalog.read("/thaumcraft-port/book-recipes.json").getAsJsonObject("recipes");
        var seen = new HashSet<String>();
        for (var entry : recipes.entrySet()) {
            var recipe = entry.getValue().getAsJsonObject();
            if (!recipe.has("layers")) continue;
            for (var layer : recipe.getAsJsonArray("layers")) for (var row : layer.getAsJsonArray()) for (var cell : row.getAsJsonArray()) {
                if (cell.isJsonNull()) continue;
                String source = cell.getAsString(), id = source.split(";", 2)[0];
                if (!id.startsWith("minecraft:") || id.equals("minecraft:air") || id.equals("minecraft:lava")) continue;
                seen.add(id);
                var parts = BookBlocks.vanillaParts(source, BookBlocks.state(source));
                assertFalse(parts.isEmpty(), entry.getKey() + " must not fall back to modern vanilla art: " + source);
                for (var part : parts) assertTrue(part.get("model").getAsString().startsWith(PREFIX), source);
            }
        }
        assertEquals(Set.of("minecraft:nether_brick", "minecraft:obsidian", "minecraft:iron_bars",
                "minecraft:cauldron", "minecraft:anvil", "minecraft:piston"), seen);
        assertTrue(BookBlocks.vanillaParts("minecraft:lava", Blocks.LAVA.defaultBlockState()).isEmpty());
        assertTrue(BookBlocks.vanillaParts("minecraft:stone", Blocks.STONE.defaultBlockState()).isEmpty());
    }

    @Test void modelRegistrationIncludesEveryStandalonePartAndAllItsDependenciesAreLocal() {
        var expected = ROOT_MODELS.stream().map(name -> PREFIX + "block/" + name).collect(java.util.stream.Collectors.toSet());
        assertEquals(expected, Set.copyOf(BookBlocks.vanillaModels()));
        var registered = new HashSet<ModelResourceLocation>();
        BookBlocks.registerModels(new ModelEvent.RegisterAdditional(registered));
        for (String id : expected) assertTrue(registered.contains(ModelResourceLocation.standalone(ResourceLocation.parse(id))), id);
        var closure = new HashSet<String>();
        var textures = new HashSet<String>();
        for (String id : expected) visitModel(id, closure, textures);
        assertEquals(16, closure.size(), "Import only the six blueprints' exact model dependency closure");
        assertEquals(12, textures.size());
        var atlasSprites = new java.util.ArrayList<String>();
        for (var element : ResearchCatalog.read("/assets/minecraft/atlases/blocks.json").getAsJsonArray("sources")) {
            var source = element.getAsJsonObject();
            if (!source.get("type").getAsString().equals("minecraft:single")) continue;
            String sprite = source.has("sprite") ? source.get("sprite").getAsString() : source.get("resource").getAsString();
            if (!sprite.startsWith(PREFIX)) continue;
            assertEquals(sprite, source.get("resource").getAsString(), "The atlas must load the matching original PNG");
            atlasSprites.add(sprite);
        }
        assertEquals(textures, Set.copyOf(atlasSprites), "Every model texture outside vanilla directories must be explicitly stitched");
        assertEquals(textures.size(), atlasSprites.size(), "Repeated imports must not duplicate atlas sources");
        for (String name : ROOT_MODELS) assertEquals(name.startsWith("iron_bars_") ? "minecraft:cutout" : "minecraft:solid",
                model(PREFIX + "block/" + name).get("render_type").getAsString(), "Standalone models need their original block layer");
    }

    @Test void multipartBarsSelectOriginalCapsAndSidesForAllSixteenConnectionStates() {
        var properties = List.of(CrossCollisionBlock.NORTH, CrossCollisionBlock.EAST, CrossCollisionBlock.SOUTH, CrossCollisionBlock.WEST);
        for (int mask = 0; mask < 16; mask++) {
            var state = Blocks.IRON_BARS.defaultBlockState();
            for (int bit = 0; bit < 4; bit++) state = state.setValue(properties.get(bit), (mask & 1 << bit) != 0);
            var parts = BookBlocks.vanillaParts("minecraft:iron_bars", state);
            int connections = Integer.bitCount(mask);
            assertEquals(connections == 0 ? 2 : connections == 1 ? 3 : 1 + connections, parts.size(), "mask=" + mask);
            assertEquals(PREFIX + "block/iron_bars_post_ends", parts.getFirst().get("model").getAsString());
            var names = parts.stream().map(part -> part.get("model").getAsString()).toList();
            assertEquals(connections == 0, names.contains(PREFIX + "block/iron_bars_post"));
            assertEquals(connections == 1, names.contains(PREFIX + "block/iron_bars_cap") || names.contains(PREFIX + "block/iron_bars_cap_alt"));
            for (int bit = 0; bit < 4; bit++) if ((mask & 1 << bit) != 0) {
                String side = PREFIX + "block/iron_bars_side" + (bit >= 2 ? "_alt" : "");
                int yaw = bit % 2 == 0 ? 0 : 90;
                assertTrue(parts.stream().anyMatch(part -> part.get("model").getAsString().equals(side) && angle(part, "y") == yaw), "mask=" + mask + ", direction=" + bit);
            }
        }
    }

    @Test void furnaceAndPressUseConnectedGrilleAndOriginalMetadataOrientations() {
        var pos = BlockPos.ZERO;
        var furnace = BookBlocks.view(Map.of(pos, "minecraft:iron_bars", pos.east(), "minecraft:nether_brick", pos.west(), "minecraft:nether_brick"));
        var grille = BookBlocks.vanillaParts("minecraft:iron_bars", furnace.getBlockState(pos));
        assertEquals(List.of("iron_bars_post_ends", "iron_bars_side", "iron_bars_side_alt"),
                grille.stream().map(part -> part.get("model").getAsString().substring((PREFIX + "block/").length())).toList());
        assertEquals(90, angle(grille.get(1), "y")); assertEquals(90, angle(grille.get(2), "y"));
        assertEquals(2, BookBlocks.vanillaParts("minecraft:iron_bars", BookBlocks.state("minecraft:iron_bars")).size(), "Isolated press bars retain their center post");
        var piston = BookBlocks.state("minecraft:piston;1;1");
        assertEquals(Direction.UP, piston.getValue(DirectionalBlock.FACING));
        assertEquals(270, angle(BookBlocks.vanillaParts("minecraft:piston;1;1", piston).getFirst(), "x"));
        var anvil = BookBlocks.state("minecraft:anvil;1;0");
        assertEquals(Direction.SOUTH, anvil.getValue(HorizontalDirectionalBlock.FACING), "Legacy metadata zero uses horizontal index SOUTH");
        assertEquals(0, angle(BookBlocks.vanillaParts("minecraft:anvil;1;0", anvil).getFirst(), "y"));
    }

    @Test void cauldronAndAnvilRetainFacesRemovedFromModernVanillaModels() {
        var cauldron = model(PREFIX + "block/cauldron_empty").getAsJsonArray("elements");
        assertEquals(13, cauldron.size());
        assertTrue(cauldron.get(1).getAsJsonObject().getAsJsonObject("faces").has("north"));
        assertEquals("up", cauldron.get(1).getAsJsonObject().getAsJsonObject("faces").getAsJsonObject("up").get("cullface").getAsString());
        var anvil = model(PREFIX + "block/anvil").getAsJsonArray("elements");
        assertEquals(4, anvil.size());
        assertTrue(anvil.get(1).getAsJsonObject().getAsJsonObject("faces").has("down"));
        assertTrue(anvil.get(2).getAsJsonObject().getAsJsonObject("faces").has("up"));
    }

    @Test void allTwelveTextureFilesAreExactOriginal112Bytes() throws Exception {
        // Independently recorded from the unmodified official Minecraft 1.12.2 client JAR.
        var expected = Map.ofEntries(
                Map.entry("anvil_base", "1cc09bf9b5cda459fb88bd98b179d5273ce0ea50443afdd4d152514a58ee66c5"),
                Map.entry("anvil_top_damaged_0", "8c5df6f382350ee8d8cee7b7b8c21960236c8f21e08308b3447eb591ec4cf055"),
                Map.entry("cauldron_bottom", "1d4551282a53f19039a5842f86c1cf9e4e444ab350b18658d459f9de5ab0a1f2"),
                Map.entry("cauldron_inner", "b173973b947bae975b31bebbb39ac5dae63cdcf1011c8d2e1022b368c8e8817e"),
                Map.entry("cauldron_side", "d66f54c8c7d3cd56071eaec118e9530e470447057b8fe217779d872ed8ad2ed2"),
                Map.entry("cauldron_top", "4dc660779a84faa4bf96a9d2e76adaee83d921d26a754773b506e041130bb023"),
                Map.entry("iron_bars", "8ea0b87c6f9993233896587af1aeb6dfffd81878872d33be780f2d3a537365e2"),
                Map.entry("nether_brick", "a217f1eb8d6c7196e401e284efddc780c18bc7675ee2c35c2e083c14e2ec24ed"),
                Map.entry("obsidian", "5456fc99dd15a43e01fc34535ed6dff316795878fe3313fd2b2c2fd86511d5e9"),
                Map.entry("piston_bottom", "fc197ab254d72e04b150c45f7ff7950d1e82f7d662ccd63f570ab5aad6dda8a5"),
                Map.entry("piston_side", "e9e13d760031ccece029c7e1661400d6cb5f997d0eaa08a1061f650648e929bc"),
                Map.entry("piston_top_normal", "a7cae4895ac1844b9e35e30a87b5df25b844a7f03c1adf2b0cb366384e831aea"));
        for (var entry : expected.entrySet()) {
            byte[] bytes = resource("/assets/thaumcraft/textures/research/vanilla/blocks/" + entry.getKey() + ".png");
            assertEquals(entry.getValue(), HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)), entry.getKey());
        }
    }

    private static int angle(JsonObject part, String axis) { return part.has(axis) ? part.get(axis).getAsInt() : 0; }
    private static JsonObject model(String id) { return ResearchCatalog.read("/assets/thaumcraft/models/" + id.substring("thaumcraft:".length()) + ".json"); }
    private static void visitModel(String id, Set<String> seen, Set<String> textures) {
        assertTrue(id.startsWith(PREFIX), "Models must not inherit modern vanilla geometry: " + id);
        if (!seen.add(id)) return;
        var value = model(id);
        if (value.has("parent")) visitModel(value.get("parent").getAsString(), seen, textures);
        if (value.has("textures")) for (var texture : value.getAsJsonObject("textures").entrySet()) {
            String path = texture.getValue().getAsString();
            if (path.startsWith("#")) continue;
            assertTrue(path.startsWith(PREFIX), "Texture escaped the book namespace: " + path);
            textures.add(path);
            assertDoesNotThrow(() -> resource("/assets/thaumcraft/textures/" + path.substring("thaumcraft:".length()) + ".png"));
        }
    }
    private static byte[] resource(String path) throws IOException {
        try (var in = BookVanillaBlueprintTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path); return in.readAllBytes();
        }
    }
}
