package thaumcraft.port.boss;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredItem;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.content.PortSounds;

/** TC6 weighted loot pools shared by champion and boss treasure bags. */
public final class BossLoot {
    public static final DeferredItem<LootBagItem> COMMON=bag("common",0),UNCOMMON=bag("uncommon",1),RARE=bag("rare",2);
    private static DeferredItem<LootBagItem> bag(String name,int quality){return PortContent.ITEMS.registerItem("loot_bag_"+name,p->new LootBagItem(quality,p.stacksTo(16).rarity(quality==2?Rarity.RARE:quality==1?Rarity.UNCOMMON:Rarity.COMMON)));}
    public static void register(){}
    public static ItemStack bag(int quality){return new ItemStack((quality==2?RARE:quality==1?UNCOMMON:COMMON).get());}
    public static ItemStack lootBag(int quality){return bag(quality);}
    private record Entry(ItemStack stack,int weight){}
    private static ItemStack item(String path){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:"+path)));}
    private static void add(List<Entry> pool,ItemStack stack,int weight){if(!stack.isEmpty())pool.add(new Entry(stack,weight));}
    private static List<Entry> pool(int quality){
        var pool=new ArrayList<Entry>();
        add(pool,new ItemStack(Items.GOLD_NUGGET,quality+1),2500-quality*250);
        add(pool,item("salis_mundus"),3*(quality+1));
        for(var item:List.of(Items.CHORUS_FRUIT,Items.COMPASS,Items.COOKIE))add(pool,new ItemStack(item),5);
        if(quality==0)add(pool,pearl(7),1);
        else if(quality==1){add(pool,pearl(7),3);add(pool,pearl(6),1);}
        else{add(pool,pearl(5),9);add(pool,pearl(3),3);add(pool,pearl(0),1);add(pool,new ItemStack(Items.NETHER_STAR),1);}
        add(pool,new ItemStack(Items.DIAMOND),quality==0?10:50);add(pool,new ItemStack(Items.EMERALD),quality==0?15:75);
        add(pool,new ItemStack(Items.GOLD_INGOT),100);add(pool,new ItemStack(Items.ENDER_PEARL),100);
        if(quality>0)add(pool,item("amulet_vis_found"),6);
        if(quality==0)for(var id:List.of("amulet_mundane","ring_mundane","girdle_mundane"))add(pool,item(id),10);
        if(quality==1)for(var id:List.of("amulet_fancy","ring_fancy","girdle_fancy"))add(pool,item(id),5);
        if(quality==2)add(pool,item("ring_apprentice"),5);
        add(pool,new ItemStack(Items.EXPERIENCE_BOTTLE),5<<quality);add(pool,new ItemStack(Items.ENCHANTED_GOLDEN_APPLE),quality+1);add(pool,new ItemStack(Items.GOLDEN_APPLE),3*(quality+1));add(pool,new ItemStack(Items.BOOK),10);
        for(var potion:BuiltInRegistries.POTION.holders().toList()){
            for(var bottle:quality==0?List.of(Items.POTION,Items.SPLASH_POTION):List.of(Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION)){
                var stack=new ItemStack(bottle);stack.set(DataComponents.POTION_CONTENTS,new PotionContents(potion));add(pool,stack,2);
            }
        }
        return pool;
    }
    private static ItemStack pearl(int damage){var stack=item("primordial_pearl");stack.setDamageValue(damage);return stack;}
    public static ItemStack generateLoot(ServerLevel level,int quality,RandomSource random){
        quality=Math.clamp(quality,0,2);
        if(quality>0&&random.nextFloat()<.025f*quality)return gear(level,quality,random);
        var pool=pool(quality);int roll=random.nextInt(pool.stream().mapToInt(Entry::weight).sum());
        for(var entry:pool){roll-=entry.weight;if(roll<0){var stack=entry.stack.copy();return stack.is(Items.BOOK)?enchant(level,stack,quality,random):stack;}}
        return new ItemStack(Items.GOLD_NUGGET,quality+1);
    }
    private static ItemStack gear(ServerLevel level,int rarity,RandomSource random){
        int quality=random.nextInt(2);for(float chance:new float[]{.2f,.15f,.1f,.095f,.095f})if(random.nextFloat()<chance)quality++;
        int slot=random.nextInt(5);String[] parts={"sword","boots","legs","chest","helm"};ItemStack stack;
        if(quality==4||quality==6)stack=item((quality==4?"thaumium_":"void_")+parts[slot]);
        else{
            Item[][] sets={{Items.WOODEN_SWORD,Items.LEATHER_BOOTS,Items.LEATHER_LEGGINGS,Items.LEATHER_CHESTPLATE,Items.LEATHER_HELMET},
                    {Items.STONE_SWORD,Items.GOLDEN_BOOTS,Items.GOLDEN_LEGGINGS,Items.GOLDEN_CHESTPLATE,Items.GOLDEN_HELMET},
                    {Items.GOLDEN_SWORD,Items.CHAINMAIL_BOOTS,Items.CHAINMAIL_LEGGINGS,Items.CHAINMAIL_CHESTPLATE,Items.CHAINMAIL_HELMET},
                    {Items.IRON_SWORD,Items.IRON_BOOTS,Items.IRON_LEGGINGS,Items.IRON_CHESTPLATE,Items.IRON_HELMET},
                    {Items.DIAMOND_SWORD,Items.DIAMOND_BOOTS,Items.DIAMOND_LEGGINGS,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_HELMET}};
            stack=new ItemStack(sets[quality==5?4:quality][slot]);
        }
        if(stack.isEmpty())return generateLoot(level,rarity,random);
        if(stack.isDamageableItem())stack.setDamageValue(random.nextInt(1+stack.getMaxDamage()/6));
        return random.nextInt(4)<rarity?enchant(level,stack,rarity,random):stack;
    }
    private static ItemStack enchant(ServerLevel level,ItemStack stack,int quality,RandomSource random){return EnchantmentHelper.enchantItem(random,stack,(int)(5+quality*.75f*random.nextInt(18)),level.registryAccess(),level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.ON_RANDOM_LOOT));}
    public static final class LootBagItem extends Item {
        private final int quality;
        public LootBagItem(int quality,Properties properties){super(properties);this.quality=quality;}
        public int quality(){return quality;}
        @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
            var held=player.getItemInHand(hand);
            if(level instanceof ServerLevel server){
                int count=8+level.random.nextInt(5);
                for(int i=0;i<count;i++)server.addFreshEntity(new ItemEntity(level,player.getX(),player.getY(),player.getZ(),generateLoot(server,quality,level.random)));
                player.playSound(PortSounds.get("coins"),.75f,1);held.shrink(1);
            }
            return InteractionResultHolder.sidedSuccess(held,level.isClientSide);
        }
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag){super.appendHoverText(stack,context,tooltip,flag);tooltip.add(Component.translatable("tc.lootbag"));}
    }
    private BossLoot(){}
}
