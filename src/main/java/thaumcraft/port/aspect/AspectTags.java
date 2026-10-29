package thaumcraft.port.aspect;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.port.content.CrystalItem;

/** Migrated TC6 definitions plus separately documented modern seeds; no universal fallback. */
public final class AspectTags {
    private static final Map<String, Map<Aspect, Integer>> VALUES = load();
    private AspectTags() {}

    private static Map<String, Map<Aspect, Integer>> load() {
        Map<String, Map<Aspect, Integer>> result = new LinkedHashMap<>();
        for (String file : new String[]{"aspect-tags.json","world-aspect-tags.json","vanilla-aspect-tags.json","additional-aspect-tags.json","deepslate-aspect-tags.json","modern-aspect-tags.json"}) {
        try (var in = new InputStreamReader(Objects.requireNonNull(AspectTags.class.getResourceAsStream(
                "/thaumcraft-port/" + file)), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(in).getAsJsonObject();
            root.entrySet().forEach(entry -> {
                Map<Aspect, Integer> amounts = new LinkedHashMap<>();
                entry.getValue().getAsJsonObject().entrySet().forEach(value -> {
                    Aspect aspect = Aspect.byId(value.getKey()).orElseThrow();
                    int amount = value.getValue().getAsInt();
                    if (amount <= 0) throw new IllegalArgumentException("Nonpositive aspect amount");
                    amounts.put(aspect, amount);
                });
                if (file.equals("modern-aspect-tags.json") && result.containsKey(entry.getKey()))
                    throw new IllegalArgumentException("Modern definition overrides a migrated aspect: " + entry.getKey());
                result.put(entry.getKey(), Collections.unmodifiableMap(amounts));
            });
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load migrated aspect definitions", e);
        }
        }
        return Collections.unmodifiableMap(result);
    }

    public static Map<Aspect, Integer> forItem(ItemStack stack) {
        return ItemAspectBonuses.apply(stack, baseForItem(stack));
    }

    public static Map<Aspect, Integer> baseForItem(ItemStack stack) {
        if (stack.isEmpty()) return Map.of();
        if (stack.getItem() instanceof CrystalItem) return Map.of(CrystalItem.aspect(stack), 1);
        if(stack.is(thaumcraft.port.essentia.EssentiaContent.PHIAL.get())){var a=thaumcraft.port.essentia.EssentiaItem.aspect(stack);return a==null?Map.of(Aspect.VOID,3):Map.of(a,10);}
        var exact = VALUES.get("item:" + BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (exact != null) return exact;
        for (var entry : VALUES.entrySet()) {
            if (entry.getKey().startsWith("tag:") && stack.is(TagKey.create(Registries.ITEM,
                    ResourceLocation.parse(entry.getKey().substring(4))))) return entry.getValue();
        }
        return Map.of();
    }

    public static Map<Aspect, Integer> forBlock(BlockState block) {
        var explicit = VALUES.get("block:" + BuiltInRegistries.BLOCK.getKey(block.getBlock()));
        return explicit != null ? explicit : forItem(new ItemStack(block.getBlock().asItem()));
    }

    public static Map<Aspect, Integer> forEntity(Entity entity) {
        if (entity instanceof net.minecraft.world.entity.player.Player player)
            return AspectMath.player(player.getGameProfile().getName());
        if (entity instanceof thaumcraft.port.world.Wisp wisp) {
            Map<Aspect,Integer> values=new LinkedHashMap<>();values.merge(wisp.aspect(),5,Integer::sum);values.merge(Aspect.AURA,5,Integer::sum);values.merge(Aspect.FLIGHT,5,Integer::sum);return Collections.unmodifiableMap(values);
        }
        if (entity instanceof ItemEntity item) return forItem(item.getItem());
        return VALUES.getOrDefault("entity:" + BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()), Map.of());
    }
}
