package thaumcraft.port.client;

import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FocusParticleMotionTest {
    @Test void downwardCollisionDoesNotPreventLaterUpwardOrHorizontalDrift(){
        var floor=List.of(Shapes.create(new AABB(-10,-1,-10,10,0,10)));
        var box=new AABB(-.05,.01,-.05,.05,.11,.05);
        var landing=FocusParticleMotion.collide(box,new Vec3(.2,-.1,0),floor);
        assertEquals(-.01,landing.y,1e-8);assertEquals(.2,landing.x,1e-8);
        box=box.move(landing);
        var next=FocusParticleMotion.collide(box,new Vec3(.1,.03,.2),floor);
        assertEquals(new Vec3(.1,.03,.2),next,"FXGeneric must resume random/upward movement after touching the ground");
    }
    @Test void upwardCollisionDoesNotFreezeFallingParticles(){
        var ceiling=List.of(Shapes.create(new AABB(-10,1,-10,10,2,10)));
        var box=new AABB(-.05,.89,-.05,.05,.99,.05);
        var hit=FocusParticleMotion.collide(box,new Vec3(0,.1,0),ceiling);
        assertEquals(.01,hit.y,1e-8);
        assertEquals(new Vec3(0,-.04,.1),FocusParticleMotion.collide(box.move(hit),new Vec3(0,-.04,.1),ceiling));
    }
    @Test void cornerSlidingPreservesOriginalXBeforeZOrder(){
        var corner=List.of(Shapes.create(new AABB(.2,0,.2,1,1,1)));
        var box=new AABB(-.05,0,-.05,.05,.1,.05);
        var result=FocusParticleMotion.collide(box,new Vec3(.4,0,.6),corner);
        assertEquals(.4,result.x,1e-8,"The original resolves X before Z even when Z motion is larger");
        assertEquals(.15,result.z,1e-8);
    }
}
