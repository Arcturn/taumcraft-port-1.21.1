package thaumcraft.port.artifice.furnace;

import java.util.function.Consumer;

/** Common event bridge; dedicated servers never resolve client particle classes. */
public final class InfernalFurnaceVisuals {
    public static Consumer<InfernalFurnaceEntity> completion=entity->{};
    public static void complete(InfernalFurnaceEntity entity){completion.accept(entity);}
    private InfernalFurnaceVisuals(){}
}
