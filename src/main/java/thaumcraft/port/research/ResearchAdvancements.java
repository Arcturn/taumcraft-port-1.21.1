package thaumcraft.port.research;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** New 1.21.1 advancement integration. TC6 BETA26 itself shipped no advancement tree. */
public final class ResearchAdvancements extends SimpleCriterionTrigger<ResearchAdvancements.Instance> {
    private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS=DeferredRegister.create(Registries.TRIGGER_TYPE,"thaumcraft");
    public static final Supplier<ResearchAdvancements> RESEARCH=TRIGGERS.register("research",ResearchAdvancements::new);
    public static void register(IEventBus bus){TRIGGERS.register(bus);}
    @Override public Codec<Instance> codec(){return Instance.CODEC;}
    public void trigger(ServerPlayer player,ResearchState state){trigger(player,condition->condition.matches(state));}

    public record Instance(Optional<ContextAwarePredicate> player,String research,String category) implements SimpleInstance {
        public static final Codec<Instance> CODEC=RecordCodecBuilder.<Instance>create(instance->instance.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player),
                Codec.STRING.optionalFieldOf("research","").forGetter(Instance::research),
                Codec.STRING.optionalFieldOf("category","").forGetter(Instance::category)
        ).apply(instance,Instance::new)).validate(condition->{
            if(condition.research.isEmpty()==condition.category.isEmpty())return DataResult.error(()->"Specify exactly one research or category");
            if(!condition.category.isEmpty()&&!ResearchCatalog.CATEGORIES.contains(condition.category))return DataResult.error(()->"Unknown research category: "+condition.category);
            if(!condition.research.isEmpty()&&!condition.research.equals("!gotthaumonomicon")&&ResearchCatalog.get(condition.research)==null)return DataResult.error(()->"Unknown research: "+condition.research);
            return DataResult.success(condition);
        });
        public boolean matches(ResearchState state){return ResearchMilestones.matches(state,research,category);}
    }
}
