package thaumcraft.port.infusion.brain;

import java.util.Comparator;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.port.content.PortSounds;

/** Client animation from TileJarBrain. No inventory-model substitute is rendered in the world. */
public final class BrainJarEntity extends BlockEntity {
    public float rotation,previousRotation;
    private float targetRotation;
    private long nextSigh=System.currentTimeMillis()+1500;
    public BrainJarEntity(BlockPos pos,BlockState state){super(BrainJarContent.ENTITY.get(),pos,state);}
    public void clientTick(){
        if(level==null||!level.isClientSide)return;
        double x=worldPosition.getX()+.5,y=worldPosition.getY()+.5,z=worldPosition.getZ()+.5;
        Entity target=level.getEntitiesOfClass(ExperienceOrb.class,new AABB(worldPosition).inflate(8)).stream()
                .min(Comparator.comparingDouble(entity->entity.distanceToSqr(x,y,z))).orElse(null);
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
