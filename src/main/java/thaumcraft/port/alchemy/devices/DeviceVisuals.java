package thaumcraft.port.alchemy.devices;

import java.util.function.Consumer;

/** Client-only presentation is installed during client setup; dedicated servers never load it. */
public final class DeviceVisuals {
    public static Consumer<EverfullUrnEntity> urnTick=entity->{};
    public static Consumer<EverfullUrnEntity> urnSplash=entity->{};
    public static Consumer<PotionSprayerEntity> sprayerTick=entity->{};
    private DeviceVisuals(){}
}
