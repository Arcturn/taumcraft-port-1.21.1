package thaumcraft.port.auromancy.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import thaumcraft.port.auromancy.focus.FocusPackage;

/** Immutable program is synchronized with entity metadata, including for late-joining observers. */
public abstract class SpellEntity extends Entity {
    private static final EntityDataAccessor<CompoundTag> PROGRAM = SynchedEntityData.defineId(SpellEntity.class,EntityDataSerializers.COMPOUND_TAG);
    private FocusPackage focusPackage;
    protected SpellEntity(EntityType<?> type,Level world) { super(type,world); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) { builder.define(PROGRAM,new CompoundTag()); }
    public void setFocusPackage(FocusPackage pack) { focusPackage=pack; pack.world=level(); entityData.set(PROGRAM,pack.serialize()); }
    public FocusPackage getFocusPackage() {
        if (focusPackage==null) {focusPackage=new FocusPackage(); focusPackage.deserialize(entityData.get(PROGRAM)); focusPackage.world=level();}
        return focusPackage;
    }
    public LivingEntity getCaster() { return getFocusPackage().getCaster(); }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) { super.onSyncedDataUpdated(key); if (PROGRAM.equals(key)) focusPackage=null; }
    @Override protected void addAdditionalSaveData(CompoundTag tag) { tag.put("pack",getFocusPackage().serialize()); tag.putInt("Age",tickCount); }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        var pack=new FocusPackage(); pack.deserialize(tag.getCompound("pack")); setFocusPackage(pack); tickCount=Math.max(0,tag.getInt("Age"));
    }
    @Override public boolean isPickable() { return false; }
}
