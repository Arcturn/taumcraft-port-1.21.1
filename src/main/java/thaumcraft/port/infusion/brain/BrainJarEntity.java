package thaumcraft.port.infusion.brain;

import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.port.content.PortSounds;

/** TC6 brain animation and server-owned XP storage. */
public final class BrainJarEntity extends BlockEntity {
    public static final int CAPACITY=2000;
    private int xp,eatDelay;
    public float rotation,previousRotation;
    private float targetRotation;
    private long nextSigh=System.currentTimeMillis()+1500;
    public BrainJarEntity(BlockPos pos,BlockState state){super(BrainJarContent.ENTITY.get(),pos,state);}
    public int experience(){return xp;}
    public int delay(){return eatDelay;}
    public int comparator(){return xp*14/CAPACITY+(xp>0?1:0);}
    public void restoreExperience(int value){xp=Math.clamp(value,0,CAPACITY);sync();}
    public void tick(){if(level==null)return;if(level.isClientSide)clientTick();else serverTick();}
    public void serverTick(){
        if(!(level instanceof ServerLevel server))return;
        if(eatDelay>0){--eatDelay;return;}
        if(xp>=CAPACITY)return;
        double x=worldPosition.getX()+.5,y=worldPosition.getY()+.5,z=worldPosition.getZ()+.5;
        var closest=server.getEntitiesOfClass(ExperienceOrb.class,new AABB(worldPosition).inflate(8),Entity::isAlive).stream()
                .min(Comparator.comparingDouble(orb->orb.distanceToSqr(x,y,z))).orElse(null);
        if(closest!=null)attract(closest);
        boolean changed=false;
        for(var orb:server.getEntitiesOfClass(ExperienceOrb.class,new AABB(worldPosition).inflate(.1),Entity::isAlive)){
            if(xp>=CAPACITY)break;
            int accepted=collect(server,orb,CAPACITY-xp);if(accepted==0)continue;xp+=accepted;
            server.playSound(null,orb.blockPosition(),SoundEvents.EXPERIENCE_ORB_PICKUP,SoundSource.BLOCKS,.1f,
                    (server.random.nextFloat()-server.random.nextFloat())*.2f+1);
            changed=true;
        }
        if(changed)sync();
    }
    /** Adapt merged modern orbs without TC6's over-cap XP destruction; at most one remainder orb. */
    private static int collect(ServerLevel level,ExperienceOrb orb,int available){
        int value=orb.getValue();if(value<=0||available<=0)return 0;
        var saved=new CompoundTag();orb.addAdditionalSaveData(saved);int count=Math.max(1,saved.getInt("Count"));
        int accepted=(int)Math.min(available,(long)value*count),partial=accepted%value;
        int remainingCount=count-accepted/value-(partial>0?1:0);
        if(partial>0){
            var remainder=new ExperienceOrb(level,orb.getX(),orb.getY(),orb.getZ(),value-partial);remainder.setDeltaMovement(orb.getDeltaMovement());
            // Respect entity-spawn cancellation without consuming the XP that could not be returned.
            if(!level.addFreshEntity(remainder))return 0;
        }
        if(remainingCount>0){saved.putInt("Count",remainingCount);orb.readAdditionalSaveData(saved);orb.value=value;}else orb.discard();
        return accepted;
    }
    private void attract(ExperienceOrb orb){
        var delta=new net.minecraft.world.phys.Vec3(worldPosition.getX()+.5-orb.getX(),worldPosition.getY()+.5-orb.getY(),worldPosition.getZ()+.5-orb.getZ()).scale(1/25.0);
        double length=delta.length(),strength=1-length;
        if(length>1.0E-9&&strength>0)orb.setDeltaMovement(orb.getDeltaMovement().add(delta.multiply(.3,.5,.3).scale(strength*strength/length)));
    }
    /** Original click releases 0..63 XP and prevents immediate recapture for forty ticks. */
    public int releaseExperience(){
        if(!(level instanceof ServerLevel server))return 0;
        eatDelay=40;int released=server.random.nextInt(Math.min(xp+1,64));xp-=released;
        for(int remaining=released;remaining>0;){int amount=ExperienceOrb.getExperienceValue(remaining);remaining-=amount;
            server.addFreshEntity(new ExperienceOrb(server,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,amount));}
        server.playSound(null,worldPosition,PortSounds.get("jar"),SoundSource.BLOCKS,.2f,1);sync();return released;
    }
    public void savePortable(ItemStack stack){var tag=new CompoundTag();tag.putString("id","thaumcraft:jar_brain");tag.putInt("XP",xp);stack.set(DataComponents.BLOCK_ENTITY_DATA,CustomData.of(tag));}
    private void sync(){setChanged();if(level!=null&&!level.isClientSide){level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);level.updateNeighbourForOutputSignal(worldPosition,getBlockState().getBlock());}}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.putInt("XP",xp);tag.putInt("EatDelay",eatDelay);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);xp=Math.clamp(tag.getInt("XP"),0,CAPACITY);eatDelay=Math.clamp(tag.getInt("EatDelay"),0,40);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    public void clientTick(){
        if(level==null||!level.isClientSide)return;
        double x=worldPosition.getX()+.5,y=worldPosition.getY()+.5,z=worldPosition.getZ()+.5;
        Entity target=xp>=CAPACITY?null:level.getEntitiesOfClass(ExperienceOrb.class,new AABB(worldPosition).inflate(8)).stream()
                .min(Comparator.comparingDouble(entity->entity.distanceToSqr(x,y,z))).orElse(null);
        if(target instanceof ExperienceOrb orb&&eatDelay==0)attract(orb);
        if(eatDelay>0)--eatDelay;
        previousRotation=rotation;
        if(target==null){
            target=level.getNearestPlayer(x,y,z,6,false);
            if(target!=null&&nextSigh<System.currentTimeMillis()){
                level.playLocalSound(x,y,z,PortSounds.get("brain"),SoundSource.AMBIENT,.15f,.8f+level.random.nextFloat()*.4f,false);
                nextSigh=System.currentTimeMillis()+5000+level.random.nextInt(25000);
            }
        }
        if(target!=null)targetRotation=(float)Math.atan2(target.getZ()-z,target.getX()-x);
        else targetRotation+=.01f;
        rotation=wrap(rotation);targetRotation=wrap(targetRotation);
        rotation+=wrap(targetRotation-rotation)*.04f;
    }
    public float interpolatedRotation(float partial){return previousRotation+wrap(rotation-previousRotation)*partial;}
    private static float wrap(float angle){while(angle>=3.141593f)angle-=6.283185f;while(angle< -3.141593f)angle+=6.283185f;return angle;}
}
