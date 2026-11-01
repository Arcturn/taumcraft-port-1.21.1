package thaumcraft.port.infusion.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.port.aspect.Aspect;

public final class RechargePedestalEntity extends DisplayPedestalEntity {
    private int counter;
    public RechargePedestalEntity(BlockPos pos, BlockState state) { super(StructureContent.RECHARGE_ENTITY.get(), pos, state); }
    @Override public int getMaxStackSize() { return 64; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && stack.getItem() instanceof RechargeableItem; }
    public void tick() {
        if (level instanceof ServerLevel server && counter++ % 10 == 0 && ItemCharge.recharge(server, getItem(0), worldPosition, null, 5) > 0) {
            sync();
            Aspect[] primals = {Aspect.AIR, Aspect.FIRE, Aspect.WATER, Aspect.EARTH, Aspect.ORDER, Aspect.ENTROPY};
            level.blockEvent(worldPosition, getBlockState().getBlock(), 5, primals[level.random.nextInt(primals.length)].color());
        }
    }
}
