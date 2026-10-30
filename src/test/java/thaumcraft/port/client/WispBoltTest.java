package thaumcraft.port.client;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WispBoltTest {
    @Test void originalBoltKeepsEndpointsAndAddsRoughlyPiSegmentsPerBlock(){
        var end=new Vec3(4,2,-3);
        var nodes=WispBoltParticle.path(end,33,2,1,.5f);
        assertEquals((int)(end.length()*Math.PI),nodes.size());
        assertEquals(Vec3.ZERO,nodes.getFirst().position());
        assertEquals(end,nodes.getLast().position());
        assertEquals(.06,nodes.getFirst().radius());
        assertEquals(.06,nodes.getLast().radius());
    }
    @Test void seededPathAnimatesWithoutChangingTheRandomPattern(){
        var delta=new Vec3(10,0,0);
        var first=WispBoltParticle.path(delta,22,3,0,0);
        assertEquals(first,WispBoltParticle.path(delta,22,3,0,0));
        var later=WispBoltParticle.path(delta,22,3,2,.5f);
        assertNotEquals(first.get(1).position(),later.get(1).position());
        assertTrue(later.stream().anyMatch(n->n.radius()==.03));
        assertTrue(later.stream().allMatch(n->n.radius()>=.03&&n.radius()<=.06));
    }
    @Test void zeroLengthDoesNotProduceInvalidVertices(){
        var nodes=WispBoltParticle.path(Vec3.ZERO,0,0,0,0);
        assertEquals(2,nodes.size());
        assertTrue(nodes.stream().allMatch(n->n.position().equals(Vec3.ZERO)));
    }
}
