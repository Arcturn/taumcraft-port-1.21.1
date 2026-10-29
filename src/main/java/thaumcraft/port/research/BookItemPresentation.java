package thaumcraft.port.research;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import thaumcraft.port.aspect.Aspect;

/**
 * Read-only TC6 item descriptors for the book, not registered gameplay items.
 * Variant order: ConfigItems, ItemTCBase, ItemBaubles, ItemAmuletVis,
 * ItemTurretPlacer, ItemChunksEdible and ItemSealPlacer from BETA26.
 */
public final class BookItemPresentation {
    private static final Map<String, List<String>> VARIANTS = Map.ofEntries(
            Map.entry("ingot", List.of("thaumium", "void", "brass")),
            Map.entry("nugget", List.of("iron", "copper", "tin", "silver", "lead", "quicksilver", "thaumium", "void", "brass", "quartz", "rareearth")),
            Map.entry("cluster", List.of("iron", "gold", "copper", "tin", "silver", "lead", "cinnabar", "quartz")),
            Map.entry("plate", List.of("brass", "iron", "thaumium", "void")),
            Map.entry("mind", List.of("clockwork", "biothaumic")),
            Map.entry("module", List.of("vision", "aggression")),
            Map.entry("turret", List.of("basic", "advanced", "bore")),
            Map.entry("chunk", List.of("beef", "chicken", "pork", "fish", "rabbit", "mutton")),
            Map.entry("baubles", List.of("amulet_mundane", "ring_mundane", "girdle_mundane", "ring_apprentice", "amulet_fancy", "ring_fancy", "girdle_fancy")),
            Map.entry("amulet_vis", List.of("found", "crafted")),
            Map.entry("curio", List.of("arcane", "preserved", "ancient", "eldritch", "knowledge", "twisted", "rites")),
            Map.entry("phial", List.of("empty", "filled")),
            Map.entry("label", List.of("blank", "filled")));
    private static final Map<Integer, String> ENCHANTMENTS = Map.of(
            0, "protection", 16, "sharpness", 33, "silk_touch", 35, "fortune");

    public record Descriptor(String id, int metadata, String data) {
        public String item() { return id.startsWith("thaumcraft:") ? id.substring(11) : ""; }
        public CompoundTag tag() {
            if (!data.startsWith("{")) return new CompoundTag();
            try { return TagParser.parseTag(data); }
            catch (com.mojang.brigadier.exceptions.CommandSyntaxException exception) { return new CompoundTag(); }
        }
    }

    public static Descriptor descriptor(String source) {
        String[] parts = source.split(";", 4);
        String id = parts[0].equals("minecraft:thaumcraft:enchanted_placeholder")
                ? "thaumcraft:enchanted_placeholder" : parts[0];
        int metadata = 0;
        try { if (parts.length > 2) metadata = Integer.parseInt(parts[2]); }
        catch (NumberFormatException ignored) {}
        return new Descriptor(id, metadata, parts.length > 3 ? parts[3] : "");
    }

    /** Stable book descriptor; stack quantity never changes a NORMAL item tooltip. */
    public static String canonicalSource(String source) {
        var value = descriptor(source);
        return value.id() + ";1;" + (value.metadata() == 32767 ? 0 : value.metadata())
                + (value.data().isEmpty() ? "" : ";" + value.data());
    }

    private static final class OriginalTooltips {
        private static final com.google.gson.JsonObject CATALOG = ResearchCatalog.read("/thaumcraft-port/book-item-tooltips.json");
        private static final com.google.gson.JsonObject ENTRIES = CATALOG.getAsJsonObject("entries");
        private static final com.google.gson.JsonObject ADVANCED = CATALOG.getAsJsonObject("advanced_entries");
    }

    /**
     * Actual original-client strings, including attributes and event-added lines.
     * Translation keys allow resource packs to replace any captured line. Formatting codes
     * stay in the translated text and are interpreted by the font, as in the 1.12 client.
     * Empty means this exact descriptor has no captured original tooltip.
     */
    public static List<Component> catalogTooltip(String source) {
        return catalogTooltip(source, false);
    }

    public static List<Component> catalogTooltip(String source, boolean advanced) {
        var entries = advanced && OriginalTooltips.ADVANCED != null ? OriginalTooltips.ADVANCED : OriginalTooltips.ENTRIES;
        String canonical = canonicalSource(source);
        var keys = entries.getAsJsonArray(canonical);
        // Modern dye naming for the same original candle, used by OreIngredient cycling.
        if (keys == null && canonical.startsWith("thaumcraft:candle_light_gray;"))
            keys = entries.getAsJsonArray(canonical.replace("thaumcraft:candle_light_gray;", "thaumcraft:candle_silver;"));
        if (keys == null) return List.of();
        var lines = new ArrayList<Component>();
        for (var key : keys) lines.add(Component.translatable(key.getAsString()));
        return List.copyOf(lines);
    }

    /** Original ItemVoidseerCharm 64-76 and PlayerEvents 238-242: current-player tooltip data. */
    public static List<Component> withPlayerContext(String source, List<Component> base, int permanentWarp) {
        var descriptor = descriptor(source);
        if (!descriptor.item().equals("voidseer_charm")) return base;
        var lines = new ArrayList<>(base);
        // The fallback may already have an NBT-added warp line. Recompute the total once.
        lines.removeIf(line -> line.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents text
                && (text.getKey().equals("item.warping") || text.getKey().equals("tc.visdiscount")));
        int discount = (int)((float)Math.clamp(permanentWarp, 0, 100) / 100.0f * 25.0f);
        int warp = discount / 5 + descriptor.tag().getByte("TC.WARP");
        // Both the captured and fallback Voidseer tooltip end with the Baubles slot line.
        int insertion = Math.max(0, lines.size() - 1);
        if (warp > 0) lines.add(insertion++, Component.translatable("item.warping").append(" " + warp).withStyle(ChatFormatting.DARK_PURPLE));
        if (discount > 0) lines.add(insertion, Component.translatable("tc.visdiscount").append(": " + discount + "%").withStyle(ChatFormatting.DARK_PURPLE));
        return List.copyOf(lines);
    }

    /** Null means normal item/block naming should handle this descriptor. */
    public static String nameKey(String source) {
        var descriptor = descriptor(source);
        String item = descriptor.item();
        var variants = VARIANTS.get(item);
        if (variants != null && descriptor.metadata() >= 0 && descriptor.metadata() < variants.size())
            return "item." + item + "." + variants.get(descriptor.metadata()) + ".name";
        if (item.equals("seal") || item.equals("seals")) {
            String seal = descriptor.data();
            if (seal.startsWith("thaumcraft:")) return "item.seal." + seal.substring(11) + ".name";
            if (descriptor.metadata() == 0) return "item.seal.blank.name";
        }
        if (item.equals("crystal_essence") && !containedAspects(descriptor).isEmpty()) return "item.crystal_essence.name";
        if (item.equals("enchanted_placeholder")) return "item.enchanted_placeholder.name";
        return null;
    }

    public static Component name(String source) {
        String key = nameKey(source);
        if (key == null) return null;
        var descriptor = descriptor(source);
        var aspects = containedAspects(descriptor);
        if (!aspects.isEmpty() && (descriptor.item().equals("crystal_essence") || descriptor.item().equals("phial")))
            return Component.translatable(key, aspects.getFirst().aspect().displayName());
        return Component.translatable(key);
    }

    public static boolean hasOwnTooltip(String source) {
        String item = descriptor(source).item();
        return VARIANTS.containsKey(item) || item.equals("seal") || item.equals("seals")
                || item.equals("crystal_essence") || item.equals("enchanted_placeholder")
                || item.equals("verdant_charm") || item.equals("voidseer_charm");
    }

    /** Extra lines with their original item/global-tooltip formatting, in source order. */
    public static List<Component> details(String source) {
        var descriptor = descriptor(source);
        var tag = descriptor.tag();
        String item = descriptor.item();
        var lines = new ArrayList<Component>();
        if (item.equals("amulet_vis")) lines.add(Component.translatable("item.amulet_vis.text").withStyle(ChatFormatting.AQUA));
        if (item.equals("curio")) lines.add(Component.translatable("item.curio.text"));
        if (item.equals("voidseer_charm")) lines.add(Component.translatable("item.voidseer_charm.text")
                .withStyle(ChatFormatting.DARK_BLUE, ChatFormatting.ITALIC));
        if (item.equals("enchanted_placeholder")) {
            lines.add(Component.translatable("item.enchanted_placeholder.text").withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_AQUA));
            var enchantments = tag.getList("ench", Tag.TAG_COMPOUND);
            for (int i = 0; i < enchantments.size(); i++) {
                var enchantment = enchantments.getCompound(i);
                String name = ENCHANTMENTS.get(enchantment.getInt("id"));
                int level = enchantment.getInt("lvl");
                if (name == null || level <= 0) continue;
                var line = Component.translatable("enchantment.minecraft." + name);
                if (!name.equals("silk_touch") || level != 1)
                    line.append(" ").append(Component.translatable("enchantment.level." + level));
                lines.add(line);
            }
        }
        if (item.equals("fortress_helm")) {
            if (tag.contains("goggles")) lines.add(Component.translatable("item.goggles.name").withStyle(ChatFormatting.DARK_PURPLE));
            if (tag.contains("mask")) lines.add(Component.translatable("item.fortress_helm.mask." + tag.getInt("mask")).withStyle(ChatFormatting.GOLD));
        }
        if (item.equals("verdant_charm")) {
            int type = tag.getInt("type");
            if (type == 1 || type == 2) lines.add(Component.translatable("item.verdant_charm." + (type == 1 ? "life" : "sustain") + ".text").withStyle(ChatFormatting.GOLD));
        }
        // PlayerEvents.itemTooltip: runic, warp, discount, charge, then contained aspects.
        if (tag.getInt("TC.RUNIC") > 0) lines.add(Component.translatable("item.runic.charge").append(" +" + tag.getInt("TC.RUNIC")).withStyle(ChatFormatting.GOLD));
        if (tag.getInt("TC.WARP") > 0) lines.add(Component.translatable("item.warping").append(" " + tag.getInt("TC.WARP")).withStyle(ChatFormatting.DARK_PURPLE));
        if (item.equals("baubles") && descriptor.metadata() == 3)
            lines.add(Component.translatable("tc.visdiscount").append(": 5%").withStyle(ChatFormatting.DARK_PURPLE));
        if (item.equals("verdant_charm")) lines.add(Component.translatable("tc.charge").append(" " + tag.getInt("tc.charge")).withStyle(ChatFormatting.YELLOW));
        if (item.equals("crystal_essence") || item.equals("phial") || item.equals("label"))
            containedAspects(descriptor).stream().sorted(Comparator.comparing(value -> value.aspect().displayName()))
                    .forEach(value -> lines.add(Component.literal(value.aspect().displayName() + " x" + value.amount())));
        // Baubles 1.5.2 appends its gold slot line after TC's global tooltip event.
        // Original EN/RU name.TYPE strings are imported with the book's tooltip catalog.
        String bauble = item.equals("amulet_vis") ? "Amulet" : item.equals("verdant_charm") || item.equals("voidseer_charm") ? "Charm" : null;
        if (item.equals("baubles") && descriptor.metadata() >= 0 && descriptor.metadata() <= 6)
            bauble = switch (descriptor.metadata()) {
                case 1, 3, 5 -> "Ring";
                case 2, 6 -> "Belt";
                default -> "Amulet";
            };
        if (bauble != null) lines.add(Component.translatableWithFallback("name." + bauble.toUpperCase(java.util.Locale.ROOT),
                "Bauble (" + bauble + ")").withStyle(ChatFormatting.GOLD));
        return List.copyOf(lines);
    }

    private record ContainedAspect(Aspect aspect, int amount) {}
    private static List<ContainedAspect> containedAspects(Descriptor descriptor) {
        var out = new ArrayList<ContainedAspect>();
        var aspects = descriptor.tag().getList("Aspects", Tag.TAG_COMPOUND);
        for (int i = 0; i < aspects.size(); i++) {
            var value = aspects.getCompound(i);
            var aspect = Aspect.byId(value.getString("key"));
            if (aspect.isPresent() && value.getInt("amount") > 0) out.add(new ContainedAspect(aspect.get(), value.getInt("amount")));
        }
        return out;
    }

    private BookItemPresentation() {}
}
