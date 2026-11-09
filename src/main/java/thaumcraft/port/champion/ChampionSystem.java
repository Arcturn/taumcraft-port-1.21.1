package thaumcraft.port.champion;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import thaumcraft.port.boss.BossLoot;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.mob.*;
import thaumcraft.port.research.*;
import thaumcraft.port.world.WorldConfig;

/** TC6 champion selection, combat and loot. Attribute modifiers persist and sync through vanilla. */
@EventBusSubscriber(modid="thaumcraft")
public final class ChampionSystem {
    private static final ResourceLocation HEALTH=id("a62bef38-48cc-42a6-ac5e-ef913841c4fd"),DAMAGE=id("a340d2db-d881-4c25-ac62-f0ad14cd63b0"),
        BOLD=id("4b1edd33-caa9-47ae-a702-d86c05701037"),MIGHTY=id("7163897f-07f5-49b3-9ce4-b74beb83d2d3"),WARD=id("champion_ward_capacity"),
        TAINT_HEALTH=id("0f354f6a-33c5-40be-93be-81b1338567f1"),TAINT_DAMAGE=id("534f8c57-929a-48cf-bbd6-0fd851030748");
    private static final String TAINT_AI="thaumcraft.champion_taint_ai";
    public static int getModifier(LivingEntity mob){var attribute=mob.getAttribute(ChampionContent.MODIFIER);return attribute==null?-2:(int)attribute.getValue();}
    public static boolean isChampion(LivingEntity mob){return ChampionModifier.byId(getModifier(mob))!=null;}
    public static boolean isTainted(LivingEntity mob){return mob instanceof TaintedMob||getModifier(mob)==13;}
    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("thaumcraft",path);}
    private static void add(LivingEntity mob,net.minecraft.core.Holder<Attribute> attribute,ResourceLocation key,double amount,AttributeModifier.Operation operation){
        var instance=mob.getAttribute(attribute);if(instance!=null){instance.removeModifier(key);instance.addPermanentModifier(new AttributeModifier(key,amount,operation));}
    }
    public static boolean makeChampion(Monster mob,boolean persist){return makeChampion(mob,mob instanceof Creeper?0:mob.getRandom().nextInt(14),persist);}
    public static boolean makeChampion(Monster mob,int type,boolean persist){
        var modifier=ChampionModifier.byId(type);if(modifier==null||getModifier(mob)>-2||mob.level().isClientSide)return false;
        add(mob,ChampionContent.MODIFIER,id(modifier.legacyUuid),type+2,AttributeModifier.Operation.ADD_VALUE);
        if(!(mob instanceof ChampionBoss)){
            add(mob,Attributes.MAX_HEALTH,HEALTH,100,AttributeModifier.Operation.ADD_VALUE);
            add(mob,Attributes.ATTACK_DAMAGE,DAMAGE,2,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            mob.heal(25);
            mob.setCustomName(modifier.displayName().copy().append(" ").append(mob.getName()));
        }else ((ChampionBoss)mob).generateName();
        if(persist)mob.setPersistenceRequired();
        switch(modifier){
            case BOLD->add(mob,Attributes.MOVEMENT_SPEED,BOLD,.3,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            case MIGHTY->add(mob,Attributes.ATTACK_DAMAGE,MIGHTY,2,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            case WARDED->{int shield=baseShield(mob);add(mob,Attributes.MAX_ABSORPTION,WARD,shield,AttributeModifier.Operation.ADD_VALUE);mob.setAbsorptionAmount(mob.getAbsorptionAmount()+shield);}
            default->{}
        }
        return true;
    }
    /** Infection may also convert ordinary passive creatures; it is not the random champion health buff. */
    public static boolean makeTainted(PathfinderMob mob){
        int previous=getModifier(mob);if(previous>=0||mob.level().isClientSide)return false;
        var attribute=mob.getAttribute(ChampionContent.MODIFIER);if(attribute==null)return false;
        // An ordinary mob has already received the +1 "normal" marker.
        if(previous==-1)add(mob,ChampionContent.MODIFIER,id("f48eb416-8321-46e6-8c07-a07bed729a0c"),-1,AttributeModifier.Operation.ADD_VALUE);
        add(mob,ChampionContent.MODIFIER,id(ChampionModifier.TAINTED.legacyUuid),15,AttributeModifier.Operation.ADD_VALUE);
        if(!(mob instanceof ChampionBoss)){
            add(mob,Attributes.MAX_HEALTH,TAINT_HEALTH,25,AttributeModifier.Operation.ADD_VALUE);
            if(!ChampionContent.hadAttackAttribute(mob.getType()))mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.max(2,(mob.getBbHeight()+mob.getBbWidth())*2));
            else add(mob,Attributes.ATTACK_DAMAGE,TAINT_DAMAGE,.5,AttributeModifier.Operation.ADD_VALUE);
            mob.heal(25);
        }else ((ChampionBoss)mob).generateName();
        resetTaintedAI(mob);return true;
    }
    public static int baseShield(LivingEntity mob){return (int)mob.getAttributeBaseValue(Attributes.MAX_HEALTH)/2;}
    @SubscribeEvent public static void joined(EntityJoinLevelEvent event){
        if(event.getLevel().isClientSide||!(event.getEntity() instanceof PathfinderMob mob))return;
        if(getModifier(mob)==13)mob.getPersistentData().remove(TAINT_AI);
        if(!(mob instanceof Monster monster)||getModifier(mob)!=-2)return;
        int weight=whitelistWeight(mob);
        var biome=mob.level().getBiome(mob.blockPosition());
        boolean spooky=biome.is(BiomeTags.IS_NETHER)||biome.is(BiomeTags.IS_END)||biome.is(TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("c","is_spooky")));
        int roll=mob.getRandom().nextInt(100);
        if(weight>=0&&mob.getAttributeBaseValue(Attributes.MAX_HEALTH)>=10&&qualifies(roll,weight,mob.level().getDifficulty(),spooky,WorldConfig.CHAMPION_MOBS.get()))makeChampion(monster,false);
        else add(mob,ChampionContent.MODIFIER,id("1e645a3d-9115-4807-a61c-705172839f87"),1,AttributeModifier.Operation.ADD_VALUE);
    }
    public static boolean qualifies(int roll,int weight,Difficulty difficulty,boolean spooky,boolean enabled){
        if(difficulty==Difficulty.EASY||!enabled)roll+=2;
        if(difficulty==Difficulty.HARD&&enabled)roll-=2;
        if(spooky)roll-=enabled?2:1;
        return roll-(enabled?Math.max(0,weight-1):0)<=0;
    }
    public static int whitelistWeight(PathfinderMob mob){
        if(mob instanceof InhabitedZombie)return 3;
        if(mob instanceof Taintacle)return 2;
        if(mob instanceof Witch)return 1;
        return mob instanceof Zombie||mob instanceof Spider||mob instanceof Blaze||mob instanceof EnderMan||mob instanceof AbstractSkeleton||mob instanceof EldritchCrab?0:-1;
    }
    @SubscribeEvent public static void tick(EntityTickEvent.Post event){
        if(event.getEntity() instanceof PathfinderMob mob&&!mob.level().isClientSide&&mob.isAlive())tickModifier(mob);
    }
    public static void tickModifier(PathfinderMob mob){
        switch(getModifier(mob)){
            case 5->{if(mob.invulnerableTime<=0&&mob.tickCount%25==0&&mob.getAbsorptionAmount()<baseShield(mob))mob.setAbsorptionAmount(mob.getAbsorptionAmount()+1);}
            case 7->{if(mob.tickCount%20==0)mob.heal(1);}
            case 13->resetTaintedAI(mob);
            default->{}
        }
    }
    private static void resetTaintedAI(PathfinderMob mob){
        if(mob instanceof Monster||mob.getPersistentData().getBoolean(TAINT_AI))return;
        mob.goalSelector.removeAllGoals(goal->true);mob.targetSelector.removeAllGoals(goal->true);
        mob.goalSelector.addGoal(0,new FloatGoal(mob));mob.goalSelector.addGoal(2,new MeleeAttackGoal(mob,1.2,false));
        mob.goalSelector.addGoal(5,new MoveTowardsRestrictionGoal(mob,1));mob.goalSelector.addGoal(7,new WaterAvoidingRandomStrollGoal(mob,1));
        mob.goalSelector.addGoal(8,new LookAtPlayerGoal(mob,Player.class,8));mob.goalSelector.addGoal(8,new RandomLookAroundGoal(mob));
        mob.targetSelector.addGoal(1,new HurtByTargetGoal(mob,ZombifiedPiglin.class).setAlertOthers());
        mob.targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(mob,Player.class,true));
        mob.getPersistentData().putBoolean(TAINT_AI,true);
    }
    @SubscribeEvent public static void hurt(LivingIncomingDamageEvent event){
        var victim=event.getEntity();if(victim.level().isClientSide||event.getAmount()<=0)return;
        event.addReductionModifier(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ARMOR,(container,reduction)->{
            float raw=container.getNewDamage();
            float amount=applyCombatEffects(victim,event.getSource(),raw);
            if(amount==raw)return reduction;
            return raw-CombatRules.getDamageAfterAbsorb(victim,amount,event.getSource(),victim.getArmorValue(),(float)victim.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        });
    }
    private static float applyCombatEffects(LivingEntity victim,DamageSource source,float amount){
        if(amount<=0)return amount;
        if(victim instanceof PathfinderMob creature&&creature.getHealth()<2&&creature.isAlive()
            &&!creature.getType().is(net.minecraft.tags.EntityTypeTags.UNDEAD)&&!(creature instanceof thaumcraft.port.artifice.construct.OwnedConstruct)
            &&!(creature instanceof TaintedMob)&&creature.hasEffect(thaumcraft.port.world.WorldEntities.TAINT)&&creature.getRandom().nextBoolean()){
            makeTainted(creature);return amount;
        }
        if(victim instanceof Monster mob&&source.getEntity() instanceof LivingEntity attacker&&!(mob instanceof thaumcraft.port.warp.entity.EldritchMob&&mob.getAbsorptionAmount()>0)){
            switch(getModifier(mob)){
                case 1->{if(!source.is(DamageTypes.THORNS)){attacker.hurt(mob.damageSources().thorns(mob),1+mob.getRandom().nextInt(3));attacker.playSound(net.minecraft.sounds.SoundEvents.THORNS_HIT,.5f,1);}}
                case 2->{if(!source.is(DamageTypeTags.BYPASSES_ARMOR))amount=amount*19/25;}
                case 12->{if(mob.getRandom().nextFloat()<.4f){var crawler=MobContent.CRAWLER.get().create(mob.level());if(crawler!=null){crawler.moveTo(mob.getX(),mob.getY()+mob.getBbHeight()/2,mob.getZ(),mob.getRandom().nextFloat()*360,0);mob.level().addFreshEntity(crawler);mob.playSound(PortSounds.get("gore"),.5f,1);}}}
                default->{}
            }
        }
        // BETA26's accidental victim-is-monster nesting made player-targeted effects unreachable.
        if(amount>0&&source.getEntity() instanceof Monster attacker){
            switch(getModifier(attacker)){
                case 4->{if(attacker.getRandom().nextFloat()<.4f)victim.addEffect(new MobEffectInstance(MobEffects.WITHER,200),attacker);}
                case 6->{if(victim instanceof ServerPlayer player&&attacker.getRandom().nextFloat()<.33f){var storage=ResearchStorage.get(player.getServer());var state=storage.player(player.getUUID());state.warp(state.permanentWarp(),state.normalWarp(),state.temporaryWarp()+1+attacker.getRandom().nextInt(3));storage.setDirty();ResearchProgress.sync(player);}}
                case 8->{if(attacker.getRandom().nextFloat()<.4f)victim.igniteForSeconds(4);}
                case 9->{if(attacker.getRandom().nextFloat()<.4f)victim.addEffect(new MobEffectInstance(MobEffects.HUNGER,500),attacker);}
                case 10->{if(attacker.getRandom().nextFloat()<.4f)victim.addEffect(new MobEffectInstance(MobEffects.POISON,100),attacker);}
                case 11->attacker.heal(Math.max(2,amount/2));
                default->{}
            }
        }
        return amount;
    }
    @SubscribeEvent public static void shieldHit(LivingDamageEvent.Post event){
        var mob=event.getEntity();
        if(!(mob instanceof Monster)||!(mob.level() instanceof ServerLevel level)||(getModifier(mob)!=5&&!(mob instanceof thaumcraft.port.warp.entity.EldritchMob))
            ||event.getReduction(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ABSORPTION)<=0)return;
        var source=event.getSource();int target=source.getEntity()==null?-1:source.getEntity().getId();
        if(source.is(DamageTypes.FALL))target=-2;if(source.is(DamageTypes.FALLING_BLOCK))target=-3;
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(level,null,mob.getX(),mob.getY(),mob.getZ(),32,
            new thaumcraft.port.infusion.armor.InfusionFxPayload(1,net.minecraft.core.BlockPos.ZERO,mob.getId(),target));
        mob.playSound(PortSounds.get("runicshieldcharge"),.66f,1.1f+mob.getRandom().nextFloat()*.1f);
    }
    @SubscribeEvent public static void drops(LivingDropsEvent event){
        var mob=event.getEntity();int modifier=getModifier(mob);
        if(!(mob instanceof Monster)||mob instanceof ChampionBoss||!(mob.level() instanceof ServerLevel level)||!event.isRecentlyHit()||event.getSource().getEntity() instanceof FakePlayer||modifier<0||modifier==13)return;
        int looting=event.getSource().getEntity() instanceof LivingEntity attacker?EnchantmentHelper.getEnchantmentLevel(mob.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING),attacker):0;
        ExperienceOrb.award(level,mob.position(),5+mob.getRandom().nextInt(3));
        int quality=Math.min(2,(mob.getRandom().nextInt(9)+looting)/5);
        event.getDrops().add(new ItemEntity(level,mob.getX(),mob.getEyeY(),mob.getZ(),BossLoot.lootBag(quality)));
    }
    private ChampionSystem(){}
}
