package thaumcraft.port.mob;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootTable;

/** BETA26 brainy zombie, including its separate, looting-sensitive brain roll. */
public class BrainyZombie extends Zombie {
    public BrainyZombie(EntityType<? extends BrainyZombie> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder attributes(){return Zombie.createAttributes().add(Attributes.MAX_HEALTH,25).add(Attributes.ATTACK_DAMAGE,5).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE,0);}
    @Override protected void registerGoals(){super.registerGoals();targetSelector.addGoal(1,new HurtByTargetGoal(this));}
    @Override public int getArmorValue(){return super.getArmorValue()+1;}
    @Override protected boolean convertsInWater(){return false;}
    @Override protected boolean isSunSensitive(){return !isBaby();}
    @Override protected void randomizeReinforcementsChance(){getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE).setBaseValue(0);}
    @Override protected ResourceKey<LootTable> getDefaultLootTable(){return ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.withDefaultNamespace("entities/zombie"));}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recentlyHit){
        if(level.random.nextInt(10)-looting(source)<=4)spawnAtLocation(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:brain"))),1.5f);
        super.dropCustomDeathLoot(level,source,recentlyHit);
    }
    static int looting(DamageSource source){return source.getEntity() instanceof LivingEntity living?EnchantmentHelper.getItemEnchantmentLevel(living.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING),living.getMainHandItem()):0;}
}
