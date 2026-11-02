package thaumcraft.port.warp.entity;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** Straight gravity-free BETA26 orb: two-block splash, magic damage and eight-second weakness. */
public final class EldritchOrb extends ThrowableProjectile {
    public EldritchOrb(EntityType<? extends EldritchOrb> type,Level level){super(type,level);setNoGravity(true);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder data){}
    @Override protected double getDefaultGravity(){return 0;}
    @Override public void tick(){super.tick();if(tickCount>100)discard();}
    @Override protected void onHit(HitResult hit){
        super.onHit(hit);
        if(!level().isClientSide&&getOwner() instanceof LivingEntity owner){
            float damage=(float)owner.getAttributeValue(Attributes.ATTACK_DAMAGE)*.666f;
            for(var target:level().getEntities(owner,getBoundingBox().inflate(2),entity->entity instanceof LivingEntity&&!entity.getType().is(EntityTypeTags.UNDEAD))){
                var living=(LivingEntity)target;
                living.hurt(damageSources().indirectMagic(this,owner),damage);
                living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,160,0),owner);
            }
            playSound(SoundEvents.LAVA_EXTINGUISH,.5f,2.6f+(random.nextFloat()-random.nextFloat())*.8f);discard();
        }
    }
}
