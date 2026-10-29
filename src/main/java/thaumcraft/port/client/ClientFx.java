package thaumcraft.port.client;

import net.minecraft.client.Minecraft;
import thaumcraft.port.content.PortParticles;
import thaumcraft.port.network.WorldFxPayload;

public final class ClientFx {
    public static void receive(WorldFxPayload p) {
        var game=Minecraft.getInstance();var level=game.level;
        if(level==null||!p.valid()||game.player==null||game.player.position().distanceToSqr(p.from())>64*64)return;
        var random=level.random;
        if(p.kind()==3){
            // Scanning feedback now runs immediately in the local item-use action,
            // as in TC6. Ignore obsolete acknowledgements to avoid duplicate audio.
            return;
        }
        int count=p.kind()==1?32:p.kind()==0?1:6;
        for(int i=0;i<count;i++) {
            var pos=p.kind()==1?p.from().lerp(p.to(),i/(double)(count-1)):p.from();
            if(p.kind()==1)pos=pos.add((random.nextFloat()-.5)*.12,(random.nextFloat()-.5)*.12,(random.nextFloat()-.5)*.12);
            var particle=game.particleEngine.createParticle(PortParticles.get(p.kind()==0?"craft":p.kind()==1?"spark":"wisp"),
                    pos.x,pos.y,pos.z,(random.nextFloat()-.5)*.025,(random.nextFloat()-.5)*.025,(random.nextFloat()-.5)*.025);
            if(particle!=null)particle.setColor(((p.color()>>16)&255)/255f,((p.color()>>8)&255)/255f,(p.color()&255)/255f);
        }
    }
    private ClientFx() {}
}
