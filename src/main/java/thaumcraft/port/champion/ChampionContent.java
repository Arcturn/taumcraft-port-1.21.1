package thaumcraft.port.champion;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.*;

/** A watched vanilla attribute carries both the selected modifier and the once-only spawn roll. */
public final class ChampionContent {
    private static final DeferredRegister<Attribute> ATTRIBUTES=DeferredRegister.create(Registries.ATTRIBUTE,"thaumcraft");
    private static final java.util.Set<net.minecraft.world.entity.EntityType<?>> ADDED_DAMAGE=new java.util.HashSet<>();
    public static boolean hadAttackAttribute(net.minecraft.world.entity.EntityType<?> type){return !ADDED_DAMAGE.contains(type);}
    public static final DeferredHolder<Attribute,Attribute> MODIFIER=ATTRIBUTES.register("champion_modifier",()->new RangedAttribute("attribute.thaumcraft.champion_modifier",-2,-2,13).setSyncable(true));
    public static void register(IEventBus bus){ATTRIBUTES.register(bus);bus.addListener(ChampionContent::attributes);}
    private static void attributes(EntityAttributeModificationEvent event){
        // Entity types do not expose their runtime superclass here. Events below restrict behavior to PathfinderMob.
        for(var type:event.getTypes()){
            event.add(type,MODIFIER);
            if(!event.has(type,Attributes.ATTACK_DAMAGE)){ADDED_DAMAGE.add(type);event.add(type,Attributes.ATTACK_DAMAGE,2);}
        }
    }
    private ChampionContent(){}
}
