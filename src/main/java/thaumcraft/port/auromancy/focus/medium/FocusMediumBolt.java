package thaumcraft.port.auromancy.focus.medium;

import net.minecraft.world.phys.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.auromancy.focus.*;
import thaumcraft.port.auromancy.entity.FocusMediaVisuals;

public final class FocusMediumBolt extends FocusMediumTouch {
    @Override public String getResearch(){return "FOCUSBOLT";}
    @Override public String getKey(){return "thaumcraft.BOLT";}
    @Override public int getComplexity(){return 5;}
    @Override public Aspect getAspect(){return Aspect.ENERGY;}
    @Override public boolean execute(Trajectory trajectory){
        var hit=trace(trajectory);Vec3 end=hit.getLocation();
        if(hit instanceof EntityHitResult entity)end=trajectory.source.add(trajectory.direction.normalize().scale(trajectory.source.distanceTo(entity.getEntity().position())));
        FocusMediaVisuals.zap(getPackage(),trajectory.source,end);return true;
    }
}
