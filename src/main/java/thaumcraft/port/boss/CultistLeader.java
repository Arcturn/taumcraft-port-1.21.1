package thaumcraft.port.boss;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.*;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.world.Cultist;

/** Crimson praetor: close combat, long-range homing orbs and a cultist regeneration aura. */
public final class CultistLeader extends ThaumcraftBoss implements RangedAttackMob {
    private static final EntityDataAccessor<Byte> TITLE=SynchedEntityData.defineId(CultistLeader.class,EntityDataSerializers.BYTE);
    private static final String[] TITLES={"Alberic","Anselm","Bastian","Beturian","Chabier","Chorache","Chuse","Dodorol","Ebardo","Ferrando","Fertus","Guillen","Larpe","Obano","Zelipe"};
    public CultistLeader(EntityType<? extends CultistLeader> type,Level level){super(type,level);xpReward=40;}
    public static AttributeSupplier.Builder attributes(){return bossAttributes().add(Attributes.MAX_HEALTH,150).add(Attributes.ATTACK_DAMAGE,5).add(Attributes.MOVEMENT_SPEED,.32);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(TITLE,(byte)0);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new RangedAttackGoal(this,1,30,40,24){
            @Override public boolean canUse(){return super.canUse()&&getTarget()!=null&&distanceToSqr(getTarget())>=16*16;}
        });
        goalSelector.addGoal(3,new MeleeAttackGoal(this,1.1,false));
        goalSelector.addGoal(6,new MoveTowardsRestrictionGoal(this,.8));
        goalSelector.addGoal(7,new WaterAvoidingRandomStrollGoal(this,.8));
        goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));
        goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this){
            @Override protected void alertOthers(){
                var attacker=getLastHurtByMob();if(attacker==null)return;
                for(var cultist:level().getEntitiesOfClass(Cultist.class,getBoundingBox().inflate(getFollowDistance(),10,getFollowDistance())))
                    if(cultist.getTarget()==null&&!cultist.isAlliedTo(attacker))cultist.setTarget(attacker);
            }
        }.setAlertOthers());
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));
    }
    public String getTitle(){return TITLES[Byte.toUnsignedInt(entityData.get(TITLE))%TITLES.length];}
    @Override public void generateName(){
        var modifier=thaumcraft.port.champion.ChampionModifier.byId(thaumcraft.port.champion.ChampionSystem.getModifier(this));
        if(modifier!=null)setCustomName(Component.translatable("entity.thaumcraft.cultist_leader.custom",getTitle(),modifier.displayName()));
    }
    @Override protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random,DifficultyInstance difficulty){
        setItemSlot(EquipmentSlot.HEAD,new ItemStack(BossGear.PRAETOR_HELM.get()));
        setItemSlot(EquipmentSlot.CHEST,new ItemStack(BossGear.PRAETOR_CHEST.get()));
        setItemSlot(EquipmentSlot.LEGS,new ItemStack(BossGear.PRAETOR_LEGS.get()));
        setItemSlot(EquipmentSlot.FEET,new ItemStack(BossGear.CRIMSON_BOOTS.get()));
        setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(level().getDifficulty()==Difficulty.EASY?BossGear.VOID_SWORD.get():BossGear.CRIMSON_BLADE.get()));
    }
    @Override protected void populateDefaultEquipmentEnchantments(ServerLevelAccessor level,net.minecraft.util.RandomSource random,DifficultyInstance difficulty){
        float special=difficulty.getSpecialMultiplier();
        if(!getMainHandItem().isEmpty()&&random.nextFloat()<.5f*special){
            var enchantments=level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).get(net.minecraft.tags.EnchantmentTags.ON_RANDOM_LOOT);
            setItemSlot(EquipmentSlot.MAINHAND,EnchantmentHelper.enchantItem(random,getMainHandItem(),(int)(7+special*random.nextInt(22)),level.registryAccess(),enchantments));
        }
    }
    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data){
        populateDefaultEquipmentSlots(random,difficulty);populateDefaultEquipmentEnchantments(level,random,difficulty);
        entityData.set(TITLE,(byte)random.nextInt(TITLES.length));return super.finalizeSpawn(level,difficulty,reason,data);
    }
    @Override public boolean isAlliedTo(Entity entity){return entity instanceof Cultist||entity instanceof CultistLeader||super.isAlliedTo(entity);}
    @Override public boolean canAttackType(EntityType<?> type){return type!=thaumcraft.port.world.WorldEntities.CLERIC.get()&&type!=thaumcraft.port.world.WorldEntities.KNIGHT.get()&&type!=BossContent.CULTIST_LEADER.get()&&super.canAttackType(type);}
    @Override protected void customServerAiStep(){
        super.customServerAiStep();
        for(var cultist:level().getEntitiesOfClass(Cultist.class,getBoundingBox().inflate(8),mob->mob.isAlive()&&distanceToSqr(mob)<=64))
            if(!cultist.hasEffect(MobEffects.REGENERATION))cultist.addEffect(new MobEffectInstance(MobEffects.REGENERATION,60,1));
    }
    @Override public void performRangedAttack(LivingEntity victim,float power){
        if(!hasLineOfSight(victim)||level().isClientSide)return;
        swing(InteractionHand.MAIN_HAND);getLookControl().setLookAt(victim.getX(),victim.getY()+victim.getBbHeight()/2,victim.getZ(),30,30);
        var orb=BossContent.GOLEM_ORB.get().create(level());if(orb==null)return;
        orb.setOwner(this);orb.setTarget(victim);orb.setRed(true);orb.setPos(getX(),getEyeY()-.1,getZ());
        orb.shoot(victim.getX()-getX(),victim.getY()+victim.getBbHeight()/2-(getY()+getBbHeight()/2)+2,victim.getZ()-getZ(),.66f,3);
        orb.setPos(orb.position().add(orb.getDeltaMovement().x/2,0,orb.getDeltaMovement().z/2));
        playSound(PortSounds.get("egattack"),1,1+random.nextFloat()*.1f);level().addFreshEntity(orb);
    }
    public void spawnBurst(){if(level() instanceof ServerLevel server)thaumcraft.port.network.WorldFxPayload.send(server,0,position().add(0,1,0),position().add(0,1,0),0xaa0011);}
    @Override protected void dropCustomDeathLoot(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,boolean hit){spawnAtLocation(BossLoot.bag(2),1.5f);}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putByte("title",entityData.get(TITLE));}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(TITLE,(byte)Math.floorMod(tag.getByte("title"),TITLES.length));}
}
