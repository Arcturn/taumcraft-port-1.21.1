package thaumcraft.port.content;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Both the shipped TC6 sound variants and the original registered IDs are retained. */
public final class PortSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, "thaumcraft");
    private static final Map<String, Supplier<SoundEvent>> EVENTS = new LinkedHashMap<>();
    static {
        try (var in = PortSounds.class.getResourceAsStream("/assets/thaumcraft/sounds.json")) {
            if (in == null) throw new IllegalStateException("Missing sound bank");
            var json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (String key : json.keySet()) EVENTS.put(key, SOUNDS.register(key,
                    () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("thaumcraft", key))));
        } catch (java.io.IOException e) { throw new ExceptionInInitializerError(e); }
    }
    public static SoundEvent get(String key) {
        var sound = EVENTS.get(key);
        if (sound == null) throw new IllegalArgumentException("Unknown Thaumcraft sound: " + key);
        return sound.get();
    }
    private static SoundType type(String key, float pitch) {
        Supplier<SoundEvent> supplier = () -> get(key);
        return new DeferredSoundType(.5f, pitch, supplier, supplier, supplier, supplier, supplier);
    }
    public static final SoundType CRYSTAL = type("crystal", 1);
    public static final SoundType GORE = type("gore", 1);
    public static final SoundType JAR = type("jar", 1);
    public static final SoundType URN = type("urnbreak", 1.5f);
    public static void register(IEventBus bus) { SOUNDS.register(bus); }
    private PortSounds() {}
}
