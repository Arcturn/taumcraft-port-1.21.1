package thaumcraft.port.data;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.BlockItem;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.content.ResearchDisplayItem;
import thaumcraft.port.research.ResearchCatalog;
import thaumcraft.port.research.ResearchRequirementAudit;
import thaumcraft.port.theory.TheoryAids;
import thaumcraft.port.theory.TheoryCards;

/** Actual loaded registries, never inferred from textures or display-only book models. */
final class ContentSnapshot {
    static JsonObject capture(RegistryAccess access, ResourceManager resources) throws Exception {
        JsonObject root = new JsonObject();
        root.addProperty("schema", 1);
        root.addProperty("mod_version", net.neoforged.fml.ModList.get().getModContainerById("thaumcraft").orElseThrow().getModInfo().getVersion().toString());
        root.addProperty("source", "NeoForge GatherDataEvent: loaded Minecraft registries and decoded datapack");
        root.addProperty("gameplay_verified", false);
        root.addProperty("client_rendered", false);
        JsonObject registries = new JsonObject();
        access.registries().sorted(Comparator.comparing(e -> e.key().location().toString())).forEach(entry -> {
            JsonArray ids = new JsonArray();
            entry.value().keySet().stream().filter(ContentSnapshot::ours).sorted().forEach(id -> ids.add(id.toString()));
            if (!ids.isEmpty()) registries.add(entry.key().location().toString(), ids);
        });
        root.add("registries", registries);
        JsonObject items = new JsonObject();
        BuiltInRegistries.ITEM.keySet().stream().filter(ContentSnapshot::ours).sorted().forEach(id -> {
            var item = BuiltInRegistries.ITEM.get(id);
            JsonObject value = new JsonObject();
            value.addProperty("class", item.getClass().getName());
            value.addProperty("display_only", item instanceof ResearchDisplayItem || item == PortContent.RESEARCH_ICON.get());
            if (item instanceof BlockItem block) value.addProperty("block", BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString());
            items.add(id.toString(), value);
        });
        root.add("items", items);
        JsonObject recipes = new JsonObject();
        for (var entry : new TreeMap<>(resources.listResources("recipe", id -> ours(id) && id.getPath().endsWith(".json"))).entrySet()) {
            try (var reader = entry.getValue().openAsReader()) {
                String path = entry.getKey().getPath();
                recipes.add(entry.getKey().getNamespace() + ":" + path.substring(7, path.length() - 5), JsonParser.parseReader(reader));
            }
        }
        root.add("recipes", recipes);
        JsonArray research = new JsonArray();
        ResearchCatalog.all().stream().map(ResearchCatalog.Entry::key).sorted().forEach(research::add);
        root.add("research", research);
        JsonArray requirements = new JsonArray();
        for (var requirement : ResearchRequirementAudit.collect()) {
            JsonObject value = new JsonObject();
            value.addProperty("research", requirement.research());
            value.addProperty("stage", requirement.stage());
            value.addProperty("field", requirement.field());
            value.addProperty("source", requirement.source());
            value.addProperty("craft_key", requirement.craftKey());
            value.addProperty("supported", requirement.supported());
            requirements.add(value);
        }
        root.add("item_requirements", requirements);
        JsonArray cards = new JsonArray();
        TheoryCards.KINDS.forEach(cards::add);
        root.add("theory_cards", cards);
        JsonObject aids = new JsonObject();
        TheoryAids.DEFINITIONS.forEach((name, aid) -> {
            JsonObject value = new JsonObject();
            value.addProperty("object", aid.object());
            value.addProperty("definition_only", true);
            aids.add(name, value);
        });
        root.add("theory_aids", aids);
        JsonArray aspects = new JsonArray();
        for (var aspect : thaumcraft.port.aspect.Aspect.values()) aspects.add(aspect.id());
        root.add("aspects", aspects);
        if (items.size() != PortContent.ITEMS.getEntries().size()) throw new IllegalStateException("Item inventory mismatch");
        return root;
    }

    private static boolean ours(ResourceLocation id) { return id.getNamespace().equals("thaumcraft"); }
    private ContentSnapshot() {}
}
