package thaumcraft.port.content;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PortParticles {
    private static final DeferredRegister<ParticleType<?>> TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, "thaumcraft");
    public static final Map<String, Supplier<SimpleParticleType>> ALL = new LinkedHashMap<>();
    static { for (String name : List.of("wisp", "taint", "spark", "rune", "craft", "alumentum", "nitor_core", "scan_rune", "scan_spark", "theory_rune", "salis_mundus"))
        ALL.put(name, TYPES.register(name, () -> new SimpleParticleType(false) {})); }
    static { for(var color:net.minecraft.world.item.DyeColor.values()) {
        String name="nitor_"+color.getName();ALL.put(name,TYPES.register(name,()->new SimpleParticleType(false){}));
    }}
    public static SimpleParticleType get(String name) { return ALL.get(name).get(); }
    public static void register(IEventBus bus) { TYPES.register(bus); }
    private PortParticles() {}
}
