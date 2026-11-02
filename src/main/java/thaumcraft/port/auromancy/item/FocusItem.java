package thaumcraft.port.auromancy.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import thaumcraft.port.auromancy.focus.*;

/** Configured focus crystals retain the original package format, capacities and cost formulae. */
public final class FocusItem extends Item {
    private final int maximumComplexity;
    public FocusItem(Properties properties, int maximumComplexity) {
        super(properties.stacksTo(1).rarity(Rarity.RARE)); this.maximumComplexity = maximumComplexity;
    }
    public int getMaxComplexity() { return maximumComplexity; }
    public static FocusPackage getPackage(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof FocusItem)) return null;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains("package", 10)) return null;
        var pack = new FocusPackage(); pack.deserialize(tag.getCompound("package")); return pack;
    }
    public static void setPackage(ItemStack stack, FocusPackage pack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (pack == null) tag.remove("package"); else tag.put("package", pack.serialize());
            tag.remove("color"); tag.remove("srt");
        });
    }
    public float getVisCost(ItemStack stack) { var pack = getPackage(stack); return pack == null ? 0 : pack.getVisCost(); }
    public int getActivationTime(ItemStack stack) { var pack = getPackage(stack); return pack == null ? 0 : pack.getActivationTime(); }
    public String getSortingHelper(ItemStack stack) {
        var pack = getPackage(stack); return pack == null ? null : stack.getHoverName().getString() + pack.getSortingHelper();
    }
    public int getFocusColor(ItemStack stack) {
        var pack = getPackage(stack); if (pack == null) return 0xFFFFFF;
        var effects = pack.getFocusEffects(); if (effects.length == 0) return 0;
        int r = 0, g = 0, b = 0;
        for (var effect : effects) { int color = FocusEngine.getElementColor(effect.getKey()); r += color >> 16 & 255; g += color >> 8 & 255; b += color & 255; }
        return (r / effects.length << 16) | (g / effects.length << 8) | b / effects.length;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var pack = getPackage(stack); if (pack == null) return;
        tooltip.add(Component.literal(String.format(java.util.Locale.ROOT, "%.2f ", pack.getVisCost())).append(Component.translatable("item.Focus.cost1")));
        appendNodes(pack, tooltip, 0);
    }
    private static void appendNodes(FocusPackage pack, List<Component> tooltip, int depth) {
        if (depth > FocusPackage.MAX_DEPTH) return;
        for (var element : pack.nodes) {
            if (!(element instanceof FocusNode node) || node instanceof FocusMediumRoot) continue;
            var line = Component.literal("  ".repeat(depth)).append(Component.translatable(node.getUnlocalizedName())).withStyle(ChatFormatting.DARK_PURPLE);
            if (!node.getSettingList().isEmpty()) {
                var settings = Component.literal(" [").withStyle(ChatFormatting.DARK_AQUA); boolean first = true;
                for (String key : node.getSettingList()) {
                    var setting = node.getSetting(key); if (!first) settings.append(", "); first = false;
                    settings.append(setting.getLocalizedName() + " " + setting.getValueText());
                }
                line.append(settings.append("]"));
            }
            tooltip.add(line);
            if (node instanceof FocusModSplit split) for (var child : split.getSplitPackages()) appendNodes(child, tooltip, depth + 1);
        }
    }
}
