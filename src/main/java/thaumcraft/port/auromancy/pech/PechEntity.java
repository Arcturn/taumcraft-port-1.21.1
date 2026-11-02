package thaumcraft.port.auromancy.pech;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import thaumcraft.port.auromancy.focus.*;
import thaumcraft.port.auromancy.focus.effect.*;
import thaumcraft.port.auromancy.focus.medium.FocusMediumProjectile;
import thaumcraft.port.content.PortSounds;

/** BETA26's neutral magical-forest scavenger, three professions and temporary friendships. */
public final class PechEntity extends Monster implements RangedAttackMob {
    private static final EntityDataAccessor<Byte> TYPE=SynchedEntityData.defineId(PechEntity.class,EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ANGER=SynchedEntityData.defineId(PechEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TAMED=SynchedEntityData.defineId(PechEntity.class,EntityDataSerializers.BOOLEAN);
    public static final TagKey<Biome> MAGICAL=TagKey.create(Registries.BIOME,ResourceLocation.parse("thaumcraft:pech_spawns"));
    public final NonNullList<ItemStack> loot=NonNullList.withSize(9,ItemStack.EMPTY);
    public boolean trading;
    public UUID tradingPlayer;
    public float mumble;
    private int chargeCount;
    private final RangedAttackGoal arrowAttack=new RangedAttackGoal(this,.6,20,50,15);
    private final RangedAttackGoal blastAttack=new RangedAttackGoal(this,.6,20,50,15);
    private final MeleeAttackGoal meleeAttack=new MeleeAttackGoal(this,.6,false);
    private final AvoidEntityGoal<Player> avoidPlayer=new AvoidEntityGoal<>(this,Player.class,8,.5,.6);
    public PechEntity(EntityType<? extends PechEntity> type,Level level){super(type,level);xpReward=8;setDropChance(EquipmentSlot.MAINHAND,.2f);setDropChance(EquipmentSlot.OFFHAND,.2f);setCombatTask();}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.ATTACK_DAMAGE,6).add(Attributes.MOVEMENT_SPEED,.5).add(Attributes.ARMOR,2);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(TYPE,(byte)0);builder.define(ANGER,0);builder.define(TAMED,false);}
    public int getPechType(){return entityData.get(TYPE);}
    public void setPechType(int type){entityData.set(TYPE,(byte)Math.clamp(type,0,2));}
    public int getAnger(){return entityData.get(ANGER);}
    public void setAnger(int anger){entityData.set(ANGER,Math.max(0,anger));}
    public boolean isTamed(){return entityData.get(TAMED);}
    public void setTamed(boolean tamed){entityData.set(TAMED,tamed);setCombatTask();}
    @Override protected Component getTypeName(){return Component.translatable(getPechType()==0?"entity.Pech.name":"entity.Pech."+getPechType()+".name");}
    @Override protected void registerGoals(){
        if(getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation ground)ground.setCanFloat(false);
        goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new TradeGoal(this));goalSelector.addGoal(3,new FindItemGoal(this));goalSelector.addGoal(5,new OpenDoorGoal(this,true));
        goalSelector.addGoal(6,new MoveTowardsRestrictionGoal(this,.5));goalSelector.addGoal(6,new MoveThroughVillageGoal(this,1,false,4,()->true));
        goalSelector.addGoal(9,new RandomStrollGoal(this,.6));goalSelector.addGoal(9,new LookAtPlayerGoal(this,Player.class,3,1));goalSelector.addGoal(10,new LookAtPlayerGoal(this,LivingEntity.class,8));goalSelector.addGoal(11,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,new NearestAttackableTargetGoal<Player>(this,Player.class,true){@Override public boolean canUse(){return getAnger()>0&&super.canUse();}});
    }
    public void setCombatTask(){
        if(level()==null||level().isClientSide||meleeAttack==null)return;
        goalSelector.removeGoal(meleeAttack);goalSelector.removeGoal(arrowAttack);goalSelector.removeGoal(blastAttack);goalSelector.removeGoal(avoidPlayer);
        goalSelector.addGoal(2,getMainHandItem().is(Items.BOW)?arrowAttack:getMainHandItem().is(PechContent.WAND.get())?blastAttack:meleeAttack);
        if(!isTamed())goalSelector.addGoal(4,avoidPlayer);
    }
    @Override public void setItemSlot(EquipmentSlot slot,ItemStack stack){super.setItemSlot(slot,stack);if(slot==EquipmentSlot.MAINHAND)setCombatTask();}
    @Override protected void populateDefaultEquipmentSlots(RandomSource random,DifficultyInstance difficulty){
        super.populateDefaultEquipmentSlots(random,difficulty);int roll=random.nextInt(20);
        Item item=switch(roll){case 0,12->PechContent.WAND.get();case 1->Items.STONE_SWORD;case 3->Items.STONE_AXE;case 5->Items.IRON_SWORD;case 6->Items.IRON_AXE;case 7->Items.FISHING_ROD;case 8->Items.STONE_PICKAXE;case 9->Items.IRON_PICKAXE;case 2,4,10,11,13->Items.BOW;default->Items.AIR;};
        if(item!=Items.AIR)setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,@Nullable SpawnGroupData data){
        populateDefaultEquipmentSlots(random,difficulty);
        if(getMainHandItem().is(PechContent.WAND.get())){setPechType(1);setDropChance(EquipmentSlot.MAINHAND,.1f);}
        else if(!getMainHandItem().isEmpty()){if(getMainHandItem().is(Items.BOW))setPechType(2);populateDefaultEquipmentEnchantments(level,random,difficulty);}
        setCanPickUpLoot(random.nextFloat()<.75f*difficulty.getSpecialMultiplier());setCombatTask();return super.finalizeSpawn(level,difficulty,reason,data);
    }
    public static boolean canSpawn(EntityType<PechEntity> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random){
        var biome=level.getBiome(pos);boolean magical=biome.is(MAGICAL);
        if(!level.getLevel().dimension().equals(Level.OVERWORLD)&&!biome.is(ResourceLocation.parse("thaumcraft:magical_forest"))&&!biome.is(ResourceLocation.parse("thaumcraft:eerie")))magical=false;
        return magical&&level.getLevel().getEntitiesOfClass(PechEntity.class,new AABB(pos).inflate(16)).size()<4&&Monster.checkMonsterSpawnRules(type,level,reason,pos,random);
    }
    @Override public boolean removeWhenFarAway(double distance){return loot.stream().filter(stack->!stack.isEmpty()).count()<5;}
    @Override public boolean canBeLeashed(){return false;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putByte("PechType",(byte)getPechType());tag.putShort("Anger",(short)getAnger());tag.putBoolean("Tamed",isTamed());ContainerHelper.saveAllItems(tag,loot,registryAccess());}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setPechType(tag.getByte("PechType"));setAnger(tag.getShort("Anger"));setTamed(tag.getBoolean("Tamed"));Collections.fill(loot,ItemStack.EMPTY);ContainerHelper.loadAllItems(tag,loot,registryAccess());setCombatTask();}
    @Override protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,DamageSource source,boolean recentlyHit){
        for(var stack:loot)if(!stack.isEmpty()&&random.nextFloat()<.33f)spawnAtLocation(stack.copy(),1.5f);super.dropCustomDeathLoot(level,source,recentlyHit);
    }
    @Override public int getArmorValue(){return Math.min(20,super.getArmorValue());}
    @Override public int getAmbientSoundInterval(){return 120;}
    @Override protected float getSoundVolume(){return .4f;}
    @Override protected SoundEvent getAmbientSound(){return PortSounds.get("pech_idle");}
    @Override protected SoundEvent getHurtSound(DamageSource source){return PortSounds.get("pech_hit");}
    @Override protected SoundEvent getDeathSound(){return PortSounds.get("pech_death");}
    @Override public void playAmbientSound(){
        if(!level().isClientSide){
            if(random.nextInt(3)==0&&!level().getEntitiesOfClass(PechEntity.class,getBoundingBox().inflate(4,2,4),pech->pech!=this).isEmpty()){level().broadcastEntityEvent(this,(byte)17);playSound(PortSounds.get("pech_trade"),getSoundVolume(),getVoicePitch());return;}
            level().broadcastEntityEvent(this,(byte)16);
        }
        super.playAmbientSound();
    }
    public void becomeAngryAt(LivingEntity target){
        if(target instanceof Player player&&player.getAbilities().instabuild)return;
        if(getAnger()<=0){level().broadcastEntityEvent(this,(byte)19);playSound(PortSounds.get("pech_charge"),getSoundVolume(),getVoicePitch());}
        setTarget(target);setAnger(400+random.nextInt(400));setTamed(false);
    }
    @Override public boolean hurt(DamageSource source,float amount){
        if(isInvulnerableTo(source))return false;
        if(!level().isClientSide&&source.getEntity() instanceof Player player){for(var other:level().getEntitiesOfClass(PechEntity.class,getBoundingBox().inflate(32,16,32),pech->pech!=this))other.becomeAngryAt(player);becomeAngryAt(player);}
        return super.hurt(source,amount);
    }
    @Override public void tick(){
        if(mumble>0)mumble*=.75f;
        if(!level().isClientSide){
            if(getAnger()>0)setAnger(getAnger()-1);
            if(getAnger()>0&&getTarget()!=null){if(chargeCount>0)chargeCount--;if(chargeCount==0){chargeCount=100;playSound(PortSounds.get("pech_charge"),getSoundVolume(),getVoicePitch());}level().broadcastEntityEvent(this,(byte)17);}
        }else{
            if(random.nextInt(15)==0&&getAnger()>0)emotion(false,1);
            if(random.nextInt(25)==0&&isTamed())emotion(true,1);
        }
        super.tick();
    }
    @Override protected void customServerAiStep(){super.customServerAiStep();if(tickCount%40==0)heal(1);}
    @Override public void handleEntityEvent(byte event){if(event==16)mumble=(float)Math.PI;else if(event==17)mumble=(float)Math.PI*2;else if(event==18)emotion(true,5);else if(event==19){emotion(false,5);mumble=(float)Math.PI*2;}else super.handleEntityEvent(event);}
    private void emotion(boolean happy,int count){for(int i=0;i<count;i++)level().addParticle(happy?ParticleTypes.HAPPY_VILLAGER:ParticleTypes.ANGRY_VILLAGER,getX()+(random.nextFloat()*getBbWidth()*2)-getBbWidth(),getY()+.5+random.nextFloat()*getBbHeight(),getZ()+random.nextFloat()*getBbWidth()*2-getBbWidth(),random.nextGaussian()*.02,random.nextGaussian()*.02,random.nextGaussian()*.02);}
    public boolean isValued(ItemStack item){return PechTrades.valued(item,level());}
    public int getValue(ItemStack item){return PechTrades.value(item,level());}
    public boolean canPickup(ItemStack stack){
        if(stack.isEmpty())return false;if(!isTamed()&&stack.is(Items.ENDER_PEARL))return true;
        for(var held:loot)if(held.isEmpty()||ItemStack.isSameItemSameComponents(stack,held)&&stack.getCount()+held.getCount()<=held.getMaxStackSize())return true;return false;
    }
    public ItemStack pickupItem(ItemStack stack){
        if(stack.isEmpty())return ItemStack.EMPTY;
        if(!isTamed()&&isValued(stack)){if(random.nextInt(10)<getValue(stack)){setTamed(true);level().broadcastEntityEvent(this,(byte)18);}stack.shrink(1);return stack.isEmpty()?ItemStack.EMPTY:stack;}
        for(var held:loot)if(!held.isEmpty()&&held.getCount()<held.getMaxStackSize()&&ItemStack.isSameItemSameComponents(stack,held)){int amount=Math.min(stack.getCount(),held.getMaxStackSize()-held.getCount());held.grow(amount);stack.shrink(amount);if(stack.isEmpty())return ItemStack.EMPTY;}
        for(int slot=0;slot<loot.size();slot++)if(loot.get(slot).isEmpty()){loot.set(slot,stack.copy());return ItemStack.EMPTY;}return stack;
    }
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        if(player.isShiftKeyDown()||player.getItemInHand(hand).is(Items.NAME_TAG))return InteractionResult.PASS;
        if(isTamed()){
            if(!level().isClientSide&&player instanceof ServerPlayer server&&!trading)server.openMenu(new SimpleMenuProvider((id,inventory,ignored)->new PechMenu(id,inventory,this),getDisplayName()),buffer->buffer.writeVarInt(getId()));
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player,hand);
    }
    @Override public void performRangedAttack(LivingEntity target,float power){
        if(getPechType()==2){
            ItemStack ammunition=new ItemStack(Items.ARROW);
            if(random.nextFloat()<.2){ammunition=new ItemStack(Items.TIPPED_ARROW);ammunition.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.empty(),Optional.empty(),List.of(new MobEffectInstance(MobEffects.POISON,40))));}
            var arrow=new Arrow(level(),this,ammunition,getMainHandItem());double dx=target.getX()-getX(),dy=target.getBoundingBox().minY+target.getBbHeight()/3-arrow.getY(),dz=target.getZ()-getZ();
            arrow.shoot(dx,dy+Math.sqrt(dx*dx+dz*dz)*.2,dz,1.6f,14-level().getDifficulty().getId()*4);
            var enchantments=registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            int powerLevel=EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.POWER),getMainHandItem());
            arrow.setBaseDamage(power*2+random.nextGaussian()*.25+level().getDifficulty().getId()*.11+(powerLevel>0?.5:0));
            // Modern Power supplies level * .5 on impact; source adds another .5 for any enchanted bow. Punch also uses the retained weapon.
            if(EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.FLAME),getMainHandItem())>0)arrow.igniteForSeconds(100);
            playSound(SoundEvents.ARROW_SHOOT,1,1/(random.nextFloat()*.4f+.8f));level().addFreshEntity(arrow);
        }else if(getPechType()==1){
            var pack=new FocusPackage(this);var root=new FocusMediumRoot();root.setupFromCasterToTarget(this,target,distanceTo(target)/6f);pack.addNode(root);var projectile=new FocusMediumProjectile();projectile.getSetting("speed").setValue(2);pack.addNode(projectile);
            pack.addNode(random.nextBoolean()?new FocusEffectCurse():random.nextBoolean()?new FocusEffectFlux():random.nextBoolean()?new FocusEffectEarth():random.nextBoolean()?new FocusEffectAir():new FocusEffectFire());
            FocusEngine.castFocusPackage(this,pack,true);swing(InteractionHand.MAIN_HAND);
        }
    }
    private static final class TradeGoal extends Goal {
        private final PechEntity pech;TradeGoal(PechEntity pech){this.pech=pech;setFlags(EnumSet.of(Flag.MOVE,Flag.JUMP));}
        @Override public boolean canUse(){return pech.isAlive()&&!pech.isInWater()&&pech.isTamed()&&pech.onGround()&&!pech.hurtMarked&&pech.trading;}
        @Override public void start(){pech.getNavigation().stop();}
        @Override public void stop(){pech.trading=false;}
    }
    private static final class FindItemGoal extends Goal {
        private final PechEntity pech;private ItemEntity target;private int count,failedPathPenalty;
        FindItemGoal(PechEntity pech){this.pech=pech;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){
            if(--count>0)return false;double nearest=Double.MAX_VALUE;target=null;
            for(var item:pech.level().getEntitiesOfClass(ItemEntity.class,pech.getBoundingBox().inflate(16))){double distance=pech.distanceToSqr(item);if(!item.getPersistentData().getBoolean("PechDrop")&&pech.canPickup(item.getItem())&&distance<nearest&&distance<=256){nearest=distance;target=item;}}
            return target!=null;
        }
        @Override public boolean canContinueToUse(){return target!=null&&target.isAlive()&&!pech.getNavigation().isDone()&&target.distanceToSqr(pech)<256;}
        @Override public void start(){pech.getNavigation().moveTo(pech.getNavigation().createPath(target,0),pech.getAttributeValue(Attributes.MOVEMENT_SPEED)*1.5);count=0;}
        @Override public void stop(){target=null;}
        @Override public void tick(){
            if(target==null)return;pech.getLookControl().setLookAt(target,30,30);
            if(pech.getSensing().hasLineOfSight(target)&&--count<=0){count=failedPathPenalty+4+pech.random.nextInt(4);pech.getNavigation().moveTo(target,pech.getAttributeValue(Attributes.MOVEMENT_SPEED)*1.5);Path path=pech.getNavigation().getPath();var last=path==null?null:path.getEndNode();if(last!=null&&target.distanceToSqr(last.x,last.y,last.z)<1)failedPathPenalty=0;else failedPathPenalty+=10;}
            if(pech.distanceToSqr(target.getX(),target.getBoundingBox().minY,target.getZ())<=1.5){count=0;int before=target.getItem().getCount();var remaining=pech.pickupItem(target.getItem());if(remaining.isEmpty())target.discard();else target.setItem(remaining);if(remaining.isEmpty()||remaining.getCount()!=before)pech.level().playSound(null,target.getX(),target.getY(),target.getZ(),SoundEvents.ITEM_PICKUP,SoundSource.NEUTRAL,.2f,((pech.random.nextFloat()-pech.random.nextFloat())*.7f+1)*2);}
        }
    }
}
