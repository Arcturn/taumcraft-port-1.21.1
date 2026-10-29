package thaumcraft.port.theory;

import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import thaumcraft.port.research.ResearchStorage;

/** Original EntityEvents: zombie brains and research-sensitive Crimson Rites drops. */
public final class TheoryDrops {
    public static void drops(LivingDropsEvent event){
        var entity=event.getEntity();if(entity.level().isClientSide)return;
        if(entity instanceof Zombie&&event.isRecentlyHit()){
            int looting=event.getSource().getEntity() instanceof LivingEntity attacker?EnchantmentHelper.getEnchantmentLevel(entity.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING),attacker):0;
            if(entity.getRandom().nextInt(10)-looting<1)add(event,"brain");
        }
        if(entity instanceof thaumcraft.port.world.Cultist&&event.getSource().getEntity() instanceof ServerPlayer p&&!(p instanceof net.neoforged.neoforge.common.util.FakePlayer)){
            var state=ResearchStorage.get(p.getServer()).player(p.getUUID());int chance=state.knows("!CrimsonCultist@2")?20:4;
            for(var stack:p.getInventory().items)if(stack.getItem() instanceof ResearchCurioItem item&&item.kind.equals("rites")){chance=50;break;}
            if(entity.getRandom().nextInt(chance)==0)add(event,"curio_rites");
        }
    }
    private static void add(LivingDropsEvent event,String id){var e=event.getEntity();event.getDrops().add(new ItemEntity(e.level(),e.getX(),e.getY()+e.getEyeHeight(),e.getZ(),new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:"+id)))));}
    private TheoryDrops(){}
}
