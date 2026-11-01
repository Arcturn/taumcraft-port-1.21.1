package thaumcraft.port.infusion.bauble;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.essentia.EssentiaContent;

public final class ThaumicBannerEntity extends BlockEntity {
    private Aspect aspect;
    public ThaumicBannerEntity(BlockPos pos,BlockState state){super(InfusionBaubles.BANNER_ENTITY.get(),pos,state);}
    public Aspect aspect(){return aspect;}
    public void setAspect(Aspect value){aspect=value;setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public void savePortable(ItemStack stack){if(aspect!=null)stack.set(EssentiaContent.ASPECT.get(),aspect.id());}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);if(aspect!=null)tag.putString("aspect",aspect.id());}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);aspect=Aspect.byId(tag.getString("aspect")).orElse(null);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
