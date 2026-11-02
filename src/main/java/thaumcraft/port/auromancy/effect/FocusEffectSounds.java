package thaumcraft.port.auromancy.effect;

import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Exact stable-39 SRG sound mappings used by the BETA26 effect sources. */
public final class FocusEffectSounds {
    private static final Map<String,String> IDS=Map.of(
        "field_187524_aN","entity.ender_dragon.flap",
        "field_187523_aM","entity.dragon_fireball.explode",
        "field_187649_bu","item.flintandsteel.use",
        "field_187616_bj","item.firecharge.use",
        "field_187942_hp","entity.zombie_villager.cure",
        "field_187542_ac","block.chorus_flower.grow",
        "field_187514_aD","entity.elder_guardian.curse",
        "field_187598_bd","block.end_gateway.spawn",
        "field_190021_aL","block.enchantment_table.use");
    public static void play(Entity caster,String source,float volume,float pitch){play(caster.level(),Vec3.atCenterOf(caster.blockPosition().above()),source,volume,pitch);}
    public static void play(Level level,Vec3 pos,String source,float volume,float pitch){level.playSound(null,pos.x,pos.y,pos.z,BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.withDefaultNamespace(IDS.get(source))),SoundSource.PLAYERS,volume,pitch);}
    public static Map<String,String> sourceMappings(){return IDS;}
    private FocusEffectSounds(){}
}
