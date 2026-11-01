package thaumcraft.port.infusion.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.port.world.AuraSystem;
import thaumcraft.port.world.FluxRift;

/** Each stored mitigation point costs .25 flux; no aura vis or redstone powers the stabilizer. */
public final class StabilizerEntity extends BlockEntity {
    private int energy, ticks, delay;
    public StabilizerEntity(BlockPos pos, BlockState state) { super(StructureContent.STABILIZER_ENTITY.get(), pos, state); }
    public int getEnergy() { return energy; }
    public boolean mitigate(int amount) {
        if (amount <= 0 || energy < amount) return false;
        energy -= amount; sync(); return true;
    }
    public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        ticks++;
        if (energy < 15 && ticks % 20 == 0) {
            energy++; AuraSystem.pollute(server, worldPosition, .25f, true); sync();
        }
        if (energy > 0 && delay <= 0 && ticks % 5 == 0) {
            for (FluxRift rift : level.getEntitiesOfClass(FluxRift.class, new AABB(worldPosition).inflate(8))) {
                if (rift.isAlive() && rift.stability() <= 50 && mitigate(1)) { rift.addStability(.125f); delay += 5; }
                if (energy <= 0) break;
            }
        }
        if (delay > 0) delay--;
    }
    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            InlayNetwork.update(level, worldPosition);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }
    @Override public void onLoad() { super.onLoad(); if (level != null) InlayNetwork.update(level, worldPosition); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); tag.putInt("Energy", energy); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);int previous=energy;energy=Math.clamp(tag.getInt("Energy"),0,15);
        // The original client explicitly rebuilt the block mesh when stored energy changed.
        // The glow is a baked block-color tint, so a BE data packet alone is insufficient.
        if(level!=null && level.isClientSide && energy!=previous)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
