package thaumcraft.port.data;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.items.ItemStackHandler;
import thaumcraft.port.research.*;

/** Registered components, tags, class inheritance and saved flags; no synthetic Minecraft implementation. */
public final class ScanFixtures {
    public static int validate(RegistryAccess registries) {
        int checks=0;
        var mappings=ResearchCatalog.read("/thaumcraft-port/scan-keys.json");
        var enchantments=registries.lookupOrThrow(Registries.ENCHANTMENT);
        for(var entry:mappings.getAsJsonObject("enchantments").entrySet()) {
            var enchantment=enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,ResourceLocation.parse(entry.getKey())));
            String expected="!"+entry.getValue().getAsString();
            var sword=new ItemStack(Items.DIAMOND_SWORD);sword.enchant(enchantment,1);
            require(ScanDiscoveries.itemFlags(sword).equals(Set.of(expected)),"Original ordinary enchantment key: "+entry.getKey());checks++;
            var book=new ItemStack(Items.ENCHANTED_BOOK);var stored=new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            stored.set(enchantment,1);book.set(DataComponents.STORED_ENCHANTMENTS,stored.toImmutable());
            require(ScanDiscoveries.itemFlags(book).equals(Set.of(expected)),"Original stored enchantment key: "+entry.getKey());checks++;
            var loaded=ItemStack.parseOptional(registries,(net.minecraft.nbt.CompoundTag)book.save(registries));
            require(ScanDiscoveries.itemFlags(loaded).equals(Set.of(expected)),"Saved stored enchantments retain discovery: "+entry.getKey());checks++;
            var old=new ResearchState();old.flag("!enchantment."+ResourceLocation.parse(entry.getKey()).getPath());
            require(ResearchCodec.read(ResearchCodec.write(old)).knows(expected),"Old enchantment flags migrate: "+entry.getKey());checks++;
        }
        for(var entry:mappings.getAsJsonObject("effects").entrySet()) {
            var effect=BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(entry.getKey())).orElseThrow();
            String expected="!"+entry.getValue().getAsString();
            require(ScanDiscoveries.effectKey(effect).equals(expected),"Original active-effect discovery: "+entry.getKey());checks++;
            for(var container:List.of(Items.POTION,Items.SPLASH_POTION,Items.LINGERING_POTION,Items.TIPPED_ARROW)) {
                var stack=new ItemStack(container);
                stack.set(DataComponents.POTION_CONTENTS,new PotionContents(Optional.empty(),Optional.empty(),List.of(new MobEffectInstance(effect,200,2))));
                require(ScanDiscoveries.itemFlags(stack).equals(Set.of(expected)),"Custom potion contents: "+container+" "+entry.getKey());checks++;
            }
            var old=new ResearchState();old.flag("!potion."+ResourceLocation.parse(entry.getKey()).getPath());
            require(ResearchCodec.read(ResearchCodec.write(old)).knows(expected),"Old potion flags migrate: "+entry.getKey());checks++;
        }
        require(ScanDiscoveries.effectKey(MobEffects.SLOW_FALLING).equals("!effect.minecraft.slow_falling"),"New effects retain their namespaced translation key");checks++;
        var speed=PotionContents.createItemStack(Items.POTION,Potions.SWIFTNESS);
        require(ScanDiscoveries.itemFlags(speed).equals(Set.of("!effect.moveSpeed")),"Base potion effects are scanned as well as custom effects");checks++;
        var noEffects=PotionContents.createItemStack(Items.POTION,Potions.WATER);
        require(ScanDiscoveries.itemFlags(noEffects).isEmpty(),"Water cannot reveal a fabricated potion effect");checks++;
        for(String material:List.of("iron","brass","thaumium","void")) {
            var plate=BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:plate_"+material));
            require(plate!=Items.AIR&&ScanDiscoveries.itemFlags(new ItemStack(plate)).contains("f_MAT"+material.toUpperCase(Locale.ROOT)),"Plate discovery via real common tags: "+material);checks++;
        }
        require(ScanDiscoveries.itemFlags(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:brain")))).contains("f_BRAIN"),"Zombie brain reveals the original focus discovery");checks++;
        require(!ScanDiscoveries.itemFlags(new ItemStack(Items.IRON_NUGGET)).contains("f_MATIRON"),"Unregistered iron forms must not grant the plate/ore/ingot discovery");checks++;
        require(!ScanDiscoveries.itemFlags(new ItemStack(Items.CLAY)).contains("f_MATCLAY"),"Original Material.CLAY check applies to blocks, not arbitrary item materials");checks++;
        require(ScanDiscoveries.blockFlags(Blocks.CLAY.defaultBlockState(),new ItemStack(Items.CLAY)).contains("f_MATCLAY"),"Placed clay material");checks++;
        for(var color:DyeColor.values()) {
            var block=BuiltInRegistries.BLOCK.get(ResourceLocation.withDefaultNamespace(color.getName()+"_terracotta"));
            require(ScanDiscoveries.blockFlags(block.defaultBlockState(),new ItemStack(block)).contains("f_MATCLAY"),"All placed stained terracotta variants: "+color);checks++;
        }
        require(ScanDiscoveries.blockFlags(Blocks.NETHER_PORTAL.defaultBlockState(),ItemStack.EMPTY).contains("f_TELEPORT"),"Portal discovery does not require an item or aspects");checks++;
        require(thaumcraft.port.research.Scanner.blockFallback(Blocks.WATER.defaultBlockState()).is(Items.WATER_BUCKET),"Water scan uses the original bucket fallback");checks++;
        require(thaumcraft.port.research.Scanner.blockFallback(Blocks.LAVA.defaultBlockState()).is(Items.LAVA_BUCKET),"Lava scan uses the original bucket fallback");checks++;
        require(thaumcraft.port.research.Scanner.blockFallback(Blocks.OAK_FENCE.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED,true)).is(Items.OAK_FENCE),"Waterlogged blocks do not become bucket scans");checks++;

        Map<Class<? extends Entity>,String> types=new LinkedHashMap<>();
        types.put(net.minecraft.world.entity.monster.CaveSpider.class,"f_SPIDER");
        types.put(net.minecraft.world.entity.animal.SnowGolem.class,"f_golem");
        types.put(net.minecraft.world.entity.animal.IronGolem.class,"f_golem");
        types.put(net.minecraft.world.entity.projectile.SpectralArrow.class,"f_arrow");
        types.put(net.minecraft.world.entity.projectile.SmallFireball.class,"f_fireball");
        types.put(net.minecraft.world.entity.projectile.LargeFireball.class,"f_fireball");
        types.put(net.minecraft.world.entity.projectile.DragonFireball.class,"f_fireball");
        types.put(net.minecraft.world.entity.projectile.WitherSkull.class,"f_fireball");
        types.put(net.minecraft.world.entity.projectile.LlamaSpit.class,"f_spit");
        for(var entry:types.entrySet()) {
            require(ScanDiscoveries.entityTypeFlags(entry.getKey(),ResourceLocation.parse("fixture:renamed_entity")).contains(entry.getValue()),"Inherited entity discovery independent of registry name: "+entry.getKey().getSimpleName());checks++;
        }
        require(!ScanDiscoveries.entityTypeFlags(net.minecraft.world.entity.projectile.Snowball.class,ResourceLocation.parse("minecraft:snowball")).contains("f_fireball"),"Not every projectile is a fireball");checks++;

        var inventory=new ItemStackHandler(205);
        for(int i=0;i<102;i++)inventory.setStackInSlot(i*2,new ItemStack(Items.DIAMOND,64));
        var visited=new ArrayList<ItemStack>();
        int scanned=thaumcraft.port.research.Scanner.visitInventory(inventory,visited::add);
        require(scanned==100&&visited.size()==100,"Inventory limit counts occupied slots and skips empty slots");checks++;
        require(visited.stream().allMatch(s->s.getCount()==64)&&inventory.getStackInSlot(202).getCount()==64,"Scanning never extracts, splits or consumes inventory items");checks++;
        return checks;
    }

    private static void require(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    private ScanFixtures(){}
}
