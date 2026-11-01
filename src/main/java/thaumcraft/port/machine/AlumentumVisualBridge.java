package thaumcraft.port.machine;

import java.util.function.Consumer;
import thaumcraft.port.world.AlumentumProjectile;

public final class AlumentumVisualBridge {
    public static Consumer<AlumentumProjectile> tick=projectile->{};
    private AlumentumVisualBridge(){}
}
