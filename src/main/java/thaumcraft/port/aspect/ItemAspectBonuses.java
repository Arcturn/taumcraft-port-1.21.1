package thaumcraft.port.aspect;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;

/** TC6 getBonusTags: equipment and enchantments merge by maximum, never by addition. */
public final class ItemAspectBonuses {
    private static final TagKey<Item> DYES = TagKey.create(Registries.ITEM, ResourceLocation.parse("c:dyes"));

    public static Map<Aspect, Integer> apply(ItemStack stack, Map<Aspect, Integer> base) {
        if (stack.isEmpty()) return Map.of();
        var result = new LinkedHashMap<>(base);
        Item item = stack.getItem();
        if (item instanceof ArmorItem armor) AspectMath.merge(result, Aspect.PROTECT, armor.getDefense() * 4);
        else if (item instanceof SwordItem sword)
            AspectMath.merge(result, Aspect.AVERSION, (int) (4 + sword.getTier().getAttackDamageBonus()) * 4);
        else if (item instanceof BowItem) {
            AspectMath.merge(result, Aspect.AVERSION, 10);
            AspectMath.merge(result, Aspect.FLIGHT, 5);
        } else if (item instanceof HoeItem || item instanceof ShearsItem) {
            // In 1.21 hoes are DiggerItems; TC6 handled them separately by durability.
            int durability = item.getDefaultInstance().getMaxDamage();
            AspectMath.merge(result, Aspect.TOOL, durability <= 59 ? 4 : durability <= 131 ? 8 : durability <= 250 ? 12 : 16);
        } else if (item instanceof DiggerItem tool) {
            AspectMath.merge(result, Aspect.TOOL, (harvestLevel(tool.getTier()) + 1) * 4);
        }
        ModernAspectBonuses.item(stack, result);
        // These four ingredients were dyes themselves before the vanilla item split.
        if (item instanceof DyeItem || stack.is(DYES) || stack.is(Items.INK_SAC) || stack.is(Items.COCOA_BEANS)
                || stack.is(Items.LAPIS_LAZULI) || stack.is(Items.BONE_MEAL)) AspectMath.merge(result, Aspect.SENSES, 5);

        var enchantments = stack.getOrDefault(stack.is(Items.ENCHANTED_BOOK)
                ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        int magic = 0;
        // The component's iteration order replaces legacy enchantment-list order on 1.21.
        for (var entry : enchantments.entrySet()) {
            int level = entry.getIntValue() * 3;
            var holder = entry.getKey();
            String id = holder.unwrapKey().map(key -> key.location().toString()).orElse("");
            switch (id) {
                case "minecraft:aqua_affinity", "minecraft:depth_strider" -> AspectMath.merge(result, Aspect.WATER, level);
                case "minecraft:bane_of_arthropods" -> split(result, Aspect.BEAST, Aspect.AVERSION, level);
                case "minecraft:blast_protection" -> split(result, Aspect.PROTECT, Aspect.ENTROPY, level);
                case "minecraft:efficiency" -> AspectMath.merge(result, Aspect.TOOL, level);
                case "minecraft:feather_falling" -> AspectMath.merge(result, Aspect.FLIGHT, level);
                case "minecraft:fire_aspect" -> split(result, Aspect.FIRE, Aspect.AVERSION, level);
                case "minecraft:fire_protection" -> split(result, Aspect.PROTECT, Aspect.FIRE, level);
                case "minecraft:flame" -> AspectMath.merge(result, Aspect.FIRE, level);
                case "minecraft:fortune", "minecraft:looting", "minecraft:luck_of_the_sea" -> AspectMath.merge(result, Aspect.DESIRE, level);
                case "minecraft:infinity", "minecraft:mending" -> AspectMath.merge(result, Aspect.CRAFT, level);
                case "minecraft:knockback", "minecraft:punch", "minecraft:respiration" -> AspectMath.merge(result, Aspect.AIR, level);
                case "minecraft:power", "minecraft:sharpness", "minecraft:thorns" -> AspectMath.merge(result, Aspect.AVERSION, level);
                case "minecraft:projectile_protection", "minecraft:protection" -> AspectMath.merge(result, Aspect.PROTECT, level);
                case "minecraft:silk_touch" -> AspectMath.merge(result, Aspect.EXCHANGE, level);
                case "minecraft:smite" -> split(result, Aspect.UNDEAD, Aspect.AVERSION, level);
                case "minecraft:unbreaking" -> AspectMath.merge(result, Aspect.EARTH, level);
                case "minecraft:lure" -> AspectMath.merge(result, Aspect.BEAST, level);
                case "minecraft:frost_walker" -> AspectMath.merge(result, Aspect.COLD, level);
            }
            ModernAspectBonuses.enchantment(id, level, result);
            int weight = holder.value().getWeight();
            magic += level + (weight >= 10 ? 0 : weight >= 5 ? 2 : weight >= 2 ? 4 : 6);
        }
        AspectMath.merge(result, Aspect.MAGIC, magic);
        return AspectMath.finish(result);
    }

    private static void split(Map<Aspect, Integer> result, Aspect first, Aspect second, int level) {
        AspectMath.merge(result, first, level / 2);
        AspectMath.merge(result, second, level / 2);
    }

    private static int harvestLevel(Tier tier) {
        if (tier == Tiers.WOOD || tier == Tiers.GOLD) return 0;
        if (tier == Tiers.STONE) return 1;
        if (tier == Tiers.IRON) return 2;
        if (tier == Tiers.DIAMOND) return 3;
        if (tier == Tiers.NETHERITE) return 4;
        // Custom 1.21 tiers no longer expose the old harvest-level integer.
        var denied = tier.getIncorrectBlocksForDrops();
        if (!Blocks.OBSIDIAN.defaultBlockState().is(denied)) return 3;
        if (!Blocks.DIAMOND_ORE.defaultBlockState().is(denied)) return 2;
        return !Blocks.IRON_ORE.defaultBlockState().is(denied) ? 1 : 0;
    }

    private ItemAspectBonuses() {}
}
