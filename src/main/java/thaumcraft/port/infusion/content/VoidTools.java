package thaumcraft.port.infusion.content;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

public final class VoidTools {
    public static final Tier VOID=tier(150,3,10),PRIMAL=tier(500,4,20);
    public static final TagKey<Block> CRUSHER_MINEABLE=TagKey.create(Registries.BLOCK,ResourceLocation.parse("thaumcraft:mineable/primal_crusher"));
    private static Tier tier(int uses,int damage,int enchantment){return new Tier(){
        public int getUses(){return uses;}public float getSpeed(){return 8;}public float getAttackDamageBonus(){return damage;}
        public int getEnchantmentValue(){return enchantment;}public TagKey<Block> getIncorrectBlocksForDrops(){return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;}
        public Ingredient getRepairIngredient(){return InfusionCraftingContent.voidIngot();}
    };}
    static void repair(ItemStack stack,Level level,Entity entity){
        if(!level.isClientSide&&entity instanceof LivingEntity&&entity.tickCount%20==0&&stack.isDamaged())stack.setDamageValue(stack.getDamageValue()-1);
    }
    private static void weaken(LivingEntity victim){if(!victim.level().isClientSide)victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,80));}
    public static final class Pickaxe extends PickaxeItem implements WarpingGear {
        public Pickaxe(Properties properties){super(VOID,properties);}
        @Override public int gearWarp(ItemStack stack,Player player){return 1;}
        @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){super.inventoryTick(stack,level,entity,slot,selected);repair(stack,level,entity);}
        @Override public boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){weaken(target);return super.hurtEnemy(stack,target,attacker);}
    }
    public static final class Shovel extends ShovelItem implements WarpingGear {
        public Shovel(Properties properties){super(VOID,properties);}
        @Override public int gearWarp(ItemStack stack,Player player){return 1;}
        @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){super.inventoryTick(stack,level,entity,slot,selected);repair(stack,level,entity);}
        @Override public boolean hurtEnemy(ItemStack stack,LivingEntity target,LivingEntity attacker){weaken(target);return super.hurtEnemy(stack,target,attacker);}
    }
    public static final class Crusher extends DiggerItem implements WarpingGear {
        public Crusher(Properties properties){super(PRIMAL,CRUSHER_MINEABLE,properties);}
        @Override public int gearWarp(ItemStack stack,Player player){return 2;}
        @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){super.inventoryTick(stack,level,entity,slot,selected);repair(stack,level,entity);}
        @Override public boolean canPerformAction(ItemStack stack,ItemAbility ability){return ability==ItemAbilities.PICKAXE_DIG||ability==ItemAbilities.SHOVEL_DIG||super.canPerformAction(stack,ability);}
    }
    private VoidTools() { }
}
