package thaumcraft.port.auromancy.entity;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.port.auromancy.focus.FocusEngine;
import thaumcraft.port.auromancy.focus.FocusPackage;
import thaumcraft.port.auromancy.focus.Trajectory;

/** Dedicated-server-safe client callbacks and original 64-block effect packet range. */
public final class FocusMediaVisuals {
    public static Consumer<SpellEntity> entityTick = entity -> { };
    public static Consumer<EntitySpellBat> batTick = entity -> { };
    public static Consumer<FocusMediaPayload> receive = packet -> { };
    public static void entityTick(SpellEntity entity) {entityTick.accept(entity);}
    public static void batTick(EntitySpellBat entity) {batTick.accept(entity);}
    public static int color(FocusPackage pack) {
        var effects=pack.getFocusEffects();if(effects.length==0)return 0xFFFFFF;
        int red=0,green=0,blue=0;
        for(var effect:effects){int color=FocusEngine.getElementColor(effect.getKey());red+=color>>16&255;green+=color>>8&255;blue+=color&255;}
        return red/effects.length<<16 | green/effects.length<<8 | blue/effects.length;
    }
    public static void touch(FocusPackage pack,Trajectory trajectory) {
        var keys=Arrays.stream(pack.getFocusEffects()).map(e->e.getKey()).toList();
        if (!keys.isEmpty()) send(pack,new FocusMediaPayload(0,trajectory.source,trajectory.direction.scale(.5),0,0,keys));
    }
    public static void zap(FocusPackage pack,Vec3 from,Vec3 to) {send(pack,new FocusMediaPayload(1,from,to,color(pack),pack.getPower()*.66f,List.of()));}
    private static void send(FocusPackage pack,FocusMediaPayload payload) {
        if(pack.world instanceof ServerLevel level)PacketDistributor.sendToPlayersNear(level,null,payload.from().x,payload.from().y,payload.from().z,64,payload);
    }
    private FocusMediaVisuals() { }
}
