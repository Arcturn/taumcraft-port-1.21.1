package thaumcraft.port.content;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import thaumcraft.port.essentia.BufferEntity;
import thaumcraft.port.essentia.TubeBlock;
import thaumcraft.port.essentia.TubeEntity;
import thaumcraft.port.infusion.matrix.InfusionMatrixEntity;

/** TC6 gauntlet: hand-specific focus casting and the original block interactions. */
public final class CasterItem extends Item {
    public CasterItem(Properties properties) { super(properties); }

    @Override public InteractionResult onItemUseFirst(net.minecraft.world.item.ItemStack stack, UseOnContext context) {
        var player = context.getPlayer();
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (player == null || !level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, context.getClickedFace(), stack)) return InteractionResult.FAIL;
        var entity = level.getBlockEntity(pos);
        if (entity instanceof InfusionMatrixEntity matrix) {
            if (player instanceof ServerPlayer serverPlayer) matrix.activateOrStart(serverPlayer);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!(entity instanceof TubeEntity tube)) {
            if (player.isShiftKeyDown() && entity == null && hasBlockPicker(stack)) {
                var picked = level.getBlockState(pos).getBlock().getCloneItemStack(level, pos, level.getBlockState(pos));
                if (!picked.isEmpty()) {
                    if (!level.isClientSide) setPickedBlock(stack, picked);
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
            return InteractionResult.PASS;
        }
        var relative = context.getClickLocation().subtract(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
        double half = tube instanceof BufferEntity ? .25 : .125;
        Direction port = Math.abs(relative.x) > half + .001 ? (relative.x > 0 ? Direction.EAST : Direction.WEST)
                : Math.abs(relative.y) > half + .001 ? (relative.y > 0 ? Direction.UP : Direction.DOWN)
                : Math.abs(relative.z) > half + .001 ? (relative.z > 0 ? Direction.SOUTH : Direction.NORTH) : null;
        if (tube instanceof BufferEntity && port == null) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (port != null) {
                if (tube instanceof BufferEntity buffer && player.isShiftKeyDown()) buffer.choke(port);
                else tube.toggle(port);
            } else {
                // Clicking the body rotates the tube's output/closed face to a connected side.
                int current = tube.facing().ordinal();
                for (int i = 1; i <= 6; i++) {
                    Direction next = Direction.values()[(current + i) % 6];
                    var neighbor = tube.neighbour(next.getOpposite());
                    if (neighbor == null || !neighbor.connects(next)) continue;
                    level.setBlock(pos, tube.getBlockState().setValue(TubeBlock.FACING, next), 3);
                    tube.sync();
                    break;
                }
            }
            boolean choke = tube instanceof BufferEntity && player.isShiftKeyDown();
            level.playSound(null, pos, PortSounds.get(choke ? "squeek" : "tool"), SoundSource.BLOCKS,
                    choke ? .6f : .5f, (choke ? 2 : .9f) + level.random.nextFloat() * .2f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    public static net.minecraft.world.item.ItemStack getFocusStack(net.minecraft.world.item.ItemStack caster) {
        return caster.getOrDefault(net.minecraft.core.component.DataComponents.CONTAINER,
                net.minecraft.world.item.component.ItemContainerContents.EMPTY).stream().findFirst().orElse(net.minecraft.world.item.ItemStack.EMPTY).copy();
    }
    public static void setFocus(net.minecraft.world.item.ItemStack caster, net.minecraft.world.item.ItemStack focus) {
        caster.set(net.minecraft.core.component.DataComponents.CONTAINER,
                net.minecraft.world.item.component.ItemContainerContents.fromItems(focus.isEmpty() ? java.util.List.of() : java.util.List.of(focus.copyWithCount(1))));
    }
    public static thaumcraft.port.auromancy.focus.FocusPackage getFocus(net.minecraft.world.item.ItemStack caster) {
        return thaumcraft.port.auromancy.item.FocusItem.getPackage(getFocusStack(caster));
    }
    public static boolean hasBlockPicker(net.minecraft.world.item.ItemStack caster) {
        return thaumcraft.port.auromancy.item.CasterManager.contains(getFocus(caster), "thaumcraft.EXCHANGE");
    }
    public static net.minecraft.world.item.ItemStack getPickedBlock(net.minecraft.world.item.ItemStack caster) {
        if (!hasBlockPicker(caster)) return net.minecraft.world.item.ItemStack.EMPTY;
        var tag = caster.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        var id = net.minecraft.resources.ResourceLocation.tryParse(tag.getString("picked"));
        if (id == null) return net.minecraft.world.item.ItemStack.EMPTY;
        var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
        if (!(item instanceof net.minecraft.world.item.BlockItem)) return net.minecraft.world.item.ItemStack.EMPTY;
        var result = new net.minecraft.world.item.ItemStack(item);
        if (tag.contains("pickedState", 10)) {
            var properties = new java.util.HashMap<String, String>(); var state = tag.getCompound("pickedState");
            for (String key : state.getAllKeys()) properties.put(key, state.getString(key));
            result.set(net.minecraft.core.component.DataComponents.BLOCK_STATE, new net.minecraft.world.item.component.BlockItemStateProperties(properties));
        }
        return result;
    }
    public static void setPickedBlock(net.minecraft.world.item.ItemStack caster, net.minecraft.world.item.ItemStack picked) {
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, caster, tag -> {
            tag.putString("picked", net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(picked.getItem()).toString());
            var properties = picked.get(net.minecraft.core.component.DataComponents.BLOCK_STATE); tag.remove("pickedState");
            if (properties != null) { var state = new net.minecraft.nbt.CompoundTag(); properties.properties().forEach(state::putString); tag.put("pickedState", state); }
        });
    }
    public float getConsumptionModifier(net.minecraft.world.item.ItemStack caster, net.minecraft.world.entity.player.Player player, boolean crafting) {
        return Math.max(.1f, 1 - thaumcraft.port.auromancy.item.CasterManager.getTotalVisDiscount(player));
    }
    public boolean consumeVis(net.minecraft.world.item.ItemStack caster, net.minecraft.world.entity.player.Player player, float amount, boolean crafting, boolean simulate) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level) || !Float.isFinite(amount) || amount < 0) return false;
        float cost = amount * getConsumptionModifier(caster, player, crafting);
        if (thaumcraft.port.world.AuraSystem.drain(level, player.blockPosition(), cost, false, true) < cost) return false;
        return simulate || thaumcraft.port.world.AuraSystem.drain(level, player.blockPosition(), cost, false, false) >= cost;
    }
    @Override public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        var stack = player.getItemInHand(hand); var focusStack = getFocusStack(stack);
        var pack = thaumcraft.port.auromancy.item.FocusItem.getPackage(focusStack);
        if (!(focusStack.getItem() instanceof thaumcraft.port.auromancy.item.FocusItem focus) || pack == null || !pack.validate(focus.getMaxComplexity(), null).valid()
                || thaumcraft.port.auromancy.item.CasterManager.isOnCooldown(player)) return net.minecraft.world.InteractionResultHolder.pass(stack);
        thaumcraft.port.auromancy.item.CasterManager.setCooldown(player, pack.getActivationTime());
        if (player.isShiftKeyDown() && hasBlockPicker(stack)) return net.minecraft.world.InteractionResultHolder.pass(stack);
        if (level.isClientSide) return net.minecraft.world.InteractionResultHolder.success(stack);
        if (!consumeVis(stack, player, pack.getVisCost(), false, false)) return net.minecraft.world.InteractionResultHolder.fail(stack);
        pack.setCastingHand(hand); thaumcraft.port.auromancy.focus.FocusEngine.castFocusPackage(player, pack);
        player.swing(hand, true); return net.minecraft.world.InteractionResultHolder.success(stack);
    }
    @Override public void inventoryTick(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (entity instanceof ServerPlayer player && player.tickCount % 10 == 0 && (player.getMainHandItem() == stack || player.getOffhandItem() == stack)) {
            var aura = thaumcraft.port.world.AuraSystem.get(player.serverLevel(), player.blockPosition());
            if (aura != null) thaumcraft.port.auromancy.item.CasterPayloads.sendAura(player, aura.base(), aura.vis());
        }
    }
    @Override public boolean shouldCauseReequipAnimation(net.minecraft.world.item.ItemStack oldStack, net.minecraft.world.item.ItemStack newStack, boolean changed) {
        if (oldStack.getItem() != newStack.getItem()) return true;
        var oldFocus = getFocus(oldStack); var newFocus = getFocus(newStack);
        return (oldFocus == null ? 0 : oldFocus.getSortingHelper()) != (newFocus == null ? 0 : newFocus.getSortingHelper());
    }
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        var focus = getFocusStack(stack);
        if (focus.getItem() instanceof thaumcraft.port.auromancy.item.FocusItem item) {
            float cost = item.getVisCost(focus);
            tooltip.add(net.minecraft.network.chat.Component.translatable("tc.vis.cost").append(" ")
                    .append(net.minecraft.network.chat.Component.literal(String.format(java.util.Locale.ROOT,"%.2f ",cost)).append(net.minecraft.network.chat.Component.translatable("item.Focus.cost1")))
                    .withStyle(net.minecraft.ChatFormatting.ITALIC,net.minecraft.ChatFormatting.AQUA));
            tooltip.add(focus.getHoverName().copy().withStyle(net.minecraft.ChatFormatting.BOLD,net.minecraft.ChatFormatting.ITALIC,net.minecraft.ChatFormatting.GREEN));
            item.appendHoverText(focus,context,tooltip,flag);
        }
    }

}
