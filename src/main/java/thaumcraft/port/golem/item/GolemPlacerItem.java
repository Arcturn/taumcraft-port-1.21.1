package thaumcraft.port.golem.item;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import thaumcraft.port.golem.*;
import thaumcraft.port.golem.part.*;

public final class GolemPlacerItem extends Item {
    public GolemPlacerItem(Properties properties) { super(properties); }
    public static GolemProperties getProperties(ItemStack stack) {
        long encoded = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLong("props");
        try { return GolemProperties.fromLong(encoded); } catch (IllegalArgumentException invalid) { return new GolemProperties(); }
    }
    public static int getRankXp(ItemStack stack) { return Math.max(0, stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getInt("xp")); }
    public static void setProperties(ItemStack stack, IGolemProperties properties, int xp) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> { tag.putLong("props", properties.toLong()); tag.putInt("xp", Math.max(0, xp)); });
    }
    @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var player = context.getPlayer(); var level = context.getLevel(); var clicked = context.getClickedPos();
        if (player == null || !level.getBlockState(clicked).blocksMotion()) return InteractionResult.FAIL;
        var position = clicked.relative(context.getClickedFace());
        if (!level.mayInteract(player, position) || !player.mayUseItemAt(position, context.getClickedFace(), stack)) return InteractionResult.FAIL;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        var golem = GolemContent.GOLEM.get().create(level); if (golem == null) return InteractionResult.FAIL;
        golem.moveTo(position.getX() + .5, position.getY(), position.getZ() + .5, 0, 0);
        if (!level.noCollision(golem)) return InteractionResult.FAIL;
        golem.setOwnerUUID(player.getUUID()); golem.setValidSpawn(); golem.setProperties(getProperties(stack)); golem.setRankXp(getRankXp(stack)); golem.initializePlaced();
        if (!level.addFreshEntity(golem)) return InteractionResult.FAIL;
        if (!player.isCreative()) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        var properties = getProperties(stack);
        if (properties.hasTrait(EnumGolemTrait.SMART)) {
            var rank = Component.translatable("golem.rank").append(" " + properties.getRank()).withStyle(ChatFormatting.GOLD);
            if (properties.getRank() < 10) rank.append(Component.literal(" (" + getRankXp(stack) + "/" + ((properties.getRank()+1)*(properties.getRank()+1)*1000) + ")").withStyle(ChatFormatting.DARK_GREEN));
            tooltip.add(rank);
        }
        tooltip.add(Component.translatable("golem.material." + properties.getMaterial().key.toLowerCase(Locale.ROOT)).withStyle(ChatFormatting.GREEN));
        for (var trait : properties.getTraits()) tooltip.add(Component.literal("-").append(Component.translatable("golem.trait." + trait.name().toLowerCase(Locale.ROOT))).withStyle(ChatFormatting.BLUE));
    }
    public List<ItemStack> creativeVariants() {
        var variants = new ArrayList<ItemStack>(); var props = new GolemProperties();
        var stack = new ItemStack(this); setProperties(stack, props, 0); variants.add(stack);
        props = new GolemProperties(); props.setHead(GolemHead.getHeads()[1]); props.setArms(GolemArm.getArms()[1]);
        stack = new ItemStack(this); setProperties(stack, props, 0); variants.add(stack);
        props = new GolemProperties(); props.setMaterial(GolemMaterial.getMaterials()[1]); props.setHead(GolemHead.getHeads()[1]); props.setArms(GolemArm.getArms()[2]);
        stack = new ItemStack(this); setProperties(stack, props, 0); variants.add(stack);
        props = new GolemProperties(); props.setMaterial(GolemMaterial.getMaterials()[4]); props.setHead(GolemHead.getHeads()[1]); props.setArms(GolemArm.getArms()[3]);
        stack = new ItemStack(this); setProperties(stack, props, 0); variants.add(stack);
        return variants;
    }
}
