package thaumcraft.port.artifice.construct;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.scores.PlayerTeam;
import thaumcraft.port.content.PortSounds;

public abstract class OwnedConstruct extends PathfinderMob implements OwnableEntity, MenuProvider {
    private static final EntityDataAccessor<Optional<UUID>> OWNER=SynchedEntityData.defineId(OwnedConstruct.class,EntityDataSerializers.OPTIONAL_UUID);
    private boolean validSpawn;
    /** Equipment packets and vanilla equipment NBT are authoritative for the single inventory slot. */
    public final Container inventory=new SimpleContainer(1){
        @Override public ItemStack getItem(int slot){return slot==0?getMainHandItem():ItemStack.EMPTY;}
        @Override public void setItem(int slot,ItemStack stack){if(slot==0){setItemInHand(InteractionHand.MAIN_HAND,stack);setChanged();}}
        @Override public ItemStack removeItem(int slot,int count){var result=getItem(slot).split(count);setChanged();return result;}
        @Override public ItemStack removeItemNoUpdate(int slot){var result=getItem(slot);setItem(slot,ItemStack.EMPTY);return result;}
        @Override public boolean isEmpty(){return getMainHandItem().isEmpty();}
        @Override public void clearContent(){setItem(0,ItemStack.EMPTY);}
        @Override public boolean stillValid(Player player){return isAlive()&&isOwner(player)&&player.distanceToSqr(OwnedConstruct.this)<64;}
        @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==0&&accepts(stack);}
    };
    protected OwnedConstruct(EntityType<? extends OwnedConstruct> type,Level level){super(type,level);setPersistenceRequired();setDropChance(EquipmentSlot.MAINHAND,0);getAttribute(Attributes.STEP_HEIGHT).setBaseValue(0);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(OWNER,Optional.empty());}
    @Override public UUID getOwnerUUID(){return entityData.get(OWNER).orElse(null);}
    public void setOwnerUUID(UUID owner){entityData.set(OWNER,Optional.ofNullable(owner));}
    @Override public LivingEntity getOwner(){return getOwnerUUID()==null?null:level().getPlayerByUUID(getOwnerUUID());}
    public boolean isOwner(LivingEntity player){return player!=null&&player.getUUID().equals(getOwnerUUID());}
    public void setValidSpawn(){validSpawn=true;}
    @Override public PlayerTeam getTeam(){var owner=getOwner();return owner==null?super.getTeam():owner.getTeam();}
    @Override public boolean isAlliedTo(Entity entity){var owner=getOwner();return entity==owner||(owner!=null?owner.isAlliedTo(entity):super.isAlliedTo(entity));}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override protected int decreaseAirSupply(int air){return air;}
    @Override public boolean isPushable(){return true;}
    @Override public int getAmbientSoundInterval(){return 240;}
    @Override protected SoundEvent getAmbientSound(){return PortSounds.get("clack");}
    @Override protected SoundEvent getHurtSound(DamageSource source){return PortSounds.get("clack");}
    @Override protected SoundEvent getDeathSound(){return PortSounds.get("tool");}
    @Override public void tick(){super.tick();if(!level().isClientSide&&!validSpawn)discard();if(getTarget()!=null&&isAlliedTo(getTarget()))setTarget(null);}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("v",validSpawn);if(getOwnerUUID()!=null)tag.putUUID("OwnerUUID",getOwnerUUID());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);validSpawn=tag.getBoolean("v");if(tag.hasUUID("OwnerUUID"))setOwnerUUID(tag.getUUID("OwnerUUID"));else if(tag.contains("OwnerUUID",8))try{setOwnerUUID(UUID.fromString(tag.getString("OwnerUUID")));}catch(IllegalArgumentException ignored){}}
    public abstract boolean accepts(ItemStack stack);
    public abstract String placerId();
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        if(player.getItemInHand(hand).is(Items.NAME_TAG))return InteractionResult.PASS;
        if(!isAlive())return InteractionResult.PASS;
        if(!isOwner(player)){if(!level().isClientSide)player.displayClientMessage(Component.translatable("tc.notowned").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE,net.minecraft.ChatFormatting.ITALIC),true);return InteractionResult.sidedSuccess(level().isClientSide);}
        if(!level().isClientSide){
            if(player.isShiftKeyDown()){playSound(PortSounds.get("zap"),1,1);dropInventory();spawnAtLocation(item(placerId()),.5f);discard();player.swing(hand,true);}
            else if(player instanceof ServerPlayer server)server.openMenu(this,buffer->buffer.writeVarInt(getId()));
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    protected void dropInventory(){if(!getMainHandItem().isEmpty()){spawnAtLocation(getMainHandItem().copy(),.5f);setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);}}
    @Override public void die(DamageSource source){if(!level().isClientSide&&hasCustomName()&&getOwner() instanceof ServerPlayer owner&&level().getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES))owner.sendSystemMessage(getCombatTracker().getDeathMessage());super.die(source);if(!level().isClientSide)dropInventory();}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new ConstructMenu(id,inventory,this);}
    protected int looting(DamageSource source){return source.getEntity() instanceof LivingEntity living?net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING),living.getMainHandItem()):0;}
    protected static ItemStack item(String id){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id.contains(":")?id:"thaumcraft:"+id)));}
    @Override public void knockback(double strength,double x,double z){super.knockback(strength,x/10,z/10);var motion=getDeltaMovement();if(motion.y>.1)setDeltaMovement(motion.x,.1,motion.z);}
}
