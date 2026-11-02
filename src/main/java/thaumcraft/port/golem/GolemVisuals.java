package thaumcraft.port.golem;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import thaumcraft.port.golem.projectile.GolemOrbEntity;

/** Client callbacks, with no client class resolution on dedicated servers. */
public final class GolemVisuals {
    public static Consumer<GolemEntity> tick = entity -> {};
    public static BiConsumer<GolemEntity,Byte> event = (entity,event) -> {};
    public static Consumer<GolemOrbEntity> orbBurst = entity -> {};
    private GolemVisuals() {}
}
