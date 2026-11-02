package thaumcraft.port.auromancy.focus.medium;

import java.util.ArrayList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.auromancy.focus.*;
import thaumcraft.port.auromancy.entity.FocusMediaVisuals;

public class FocusMediumTouch extends FocusMedium {
    @Override public String getResearch(){return "BASEAUROMANCY";}
    @Override public String getKey(){return "thaumcraft.TOUCH";}
    @Override public int getComplexity(){return 2;}
    @Override public Aspect getAspect(){return Aspect.AVERSION;}
    @Override public EnumSupplyType[] willSupply(){return new EnumSupplyType[]{EnumSupplyType.TRAJECTORY,EnumSupplyType.TARGET};}
    protected double range(){return this instanceof FocusMediumBolt?16:getPackage().getCaster() instanceof Player player?player.blockInteractionRange():4.5;}
    protected HitResult trace(Trajectory trajectory){return FocusTargeting.ray(getPackage().world,getPackage().getCaster(),trajectory.source,trajectory.direction,range());}
    @Override public Trajectory[] supplyTrajectories(){
        if(getParent()==null||getParent().supplyTrajectories()==null)return new Trajectory[0];
        var out=new ArrayList<Trajectory>();
        for(var source:getParent().supplyTrajectories()){
            var hit=trace(source);Vec3 end=hit.getLocation();
            // TC6 places continuation trajectories at the target's position distance, not the hitbox surface.
            if(hit instanceof EntityHitResult entity)end=source.source.add(source.direction.normalize().scale(source.source.distanceTo(entity.getEntity().position())));
            out.add(new Trajectory(end,source.direction.normalize()));
        }
        return out.toArray(Trajectory[]::new);
    }
    @Override public HitResult[] supplyTargets(){
        if(getParent()==null||!(getPackage().getCaster() instanceof Player)||getParent().supplyTrajectories()==null)return new HitResult[0];
        var out=new ArrayList<HitResult>();for(var trajectory:getParent().supplyTrajectories()){var hit=trace(trajectory);if(hit.getType()!=HitResult.Type.MISS)out.add(hit);}
        return out.toArray(HitResult[]::new);
    }
    @Override public boolean execute(Trajectory trajectory){FocusMediaVisuals.touch(getPackage(),trajectory);return true;}
}
