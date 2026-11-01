package thaumcraft.port.infusion.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;

public final class ElementalHoeItem extends HoeItem {
    public ElementalHoeItem(Properties properties) { super(InfusionTools.ELEMENTAL,properties); }
    @Override public int getEnchantmentValue() { return 5; }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player=context.getPlayer();
        if (player==null) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) return super.useOn(context);
        var level=context.getLevel();
        boolean tilled=false;
        for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) {
            BlockPos pos=context.getClickedPos().offset(x,0,z);
            if (context.getItemInHand().isEmpty() || !level.mayInteract(player,pos)
                    || !player.mayUseItemAt(pos,context.getClickedFace(),context.getItemInHand())) continue;
            var hit=new BlockHitResult(context.getClickLocation().add(x,0,z),context.getClickedFace(),pos,context.isInside());
            if (super.useOn(new UseOnContext(player,context.getHand(),hit)).consumesAction()) {
                tilled=true;
                if (level.isClientSide) ElementalToolVisuals.till.accept(pos,x==0&&z==0);
            }
        }
        if (!tilled && level.mayInteract(player,context.getClickedPos())
                && player.mayUseItemAt(context.getClickedPos(),context.getClickedFace(),context.getItemInHand())
                && BoneMealItem.applyBonemeal(new ItemStack(Items.BONE_MEAL),level,context.getClickedPos(),player)) {
            if (!level.isClientSide) {
                context.getItemInHand().hurtAndBreak(3,player,LivingEntity.getSlotForHand(context.getHand()));
                level.levelEvent(2005,context.getClickedPos(),0);
            } else ElementalToolVisuals.growth.accept(context.getClickedPos());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
