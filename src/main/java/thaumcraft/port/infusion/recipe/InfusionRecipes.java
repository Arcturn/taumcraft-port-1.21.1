package thaumcraft.port.infusion.recipe;

import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.port.content.PortContent;

public final class InfusionRecipes {
    private static final DeferredRegister<RecipeType<?>> TYPES=DeferredRegister.create(Registries.RECIPE_TYPE,"thaumcraft");
    public static final Supplier<RecipeType<InfusionRecipe>> TYPE=TYPES.register("infusion",()->new RecipeType<InfusionRecipe>(){@Override public String toString(){return "thaumcraft:infusion";}});
    public static final Supplier<RecipeSerializer<InfusionRecipe>> SERIALIZER=PortContent.SERIALIZERS.register("infusion",InfusionRecipe.Serializer::new);
    public static void register(IEventBus bus){InfusionFood.register();TYPES.register(bus);}
    private InfusionRecipes(){}
}
