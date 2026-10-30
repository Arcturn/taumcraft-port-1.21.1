package thaumcraft.port.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import thaumcraft.port.content.PortParticles;
import thaumcraft.port.network.WispZapPayload;
import thaumcraft.port.world.Wisp;

public final class WispFx {
    public static void tick(Wisp wisp){
        if(wisp.tickCount<=1)burst(wisp.position(),10);
        var r=wisp.level().random;
        if(!wisp.isDeadOrDying()&&r.nextBoolean()&&wisp.hasAspect()){
            var pos=wisp.position().add((r.nextFloat()-r.nextFloat())*.7f,
                    (r.nextFloat()-r.nextFloat())*.7f,(r.nextFloat()-r.nextFloat())*.7f);
            var particle=create(pos);
            if(particle!=null)particle.wispMote(wisp.aspect().color());
        }
    }
    public static void death(Wisp wisp){burst(wisp.position().add(0,.45f,0),1);}
    private static void burst(Vec3 pos,float size){var p=create(pos);if(p!=null)p.wispBurst(size);}
    private static SalisMundusParticle create(Vec3 pos){
        var p=Minecraft.getInstance().particleEngine.createParticle(PortParticles.get("salis_mundus"),pos.x,pos.y,pos.z,0,0,0);
        return p instanceof SalisMundusParticle fx?fx:null;
    }
    public static void zap(WispZapPayload packet){
        var game=Minecraft.getInstance();var level=game.level;if(level==null)return;
        var from=level.getEntity(packet.source());var to=level.getEntity(packet.target());
        if(!(from instanceof Wisp wisp)||to==null||from.distanceToSqr(to)>64*64)return;
        game.particleEngine.add(new WispBoltParticle(level,from.position(),to.position(),wisp.aspect().color()));
    }
    private WispFx(){}
}
