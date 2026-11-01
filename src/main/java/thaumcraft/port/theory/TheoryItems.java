package thaumcraft.port.theory;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import thaumcraft.port.aspect.*;
import thaumcraft.port.content.*;
import thaumcraft.port.essentia.*;
import thaumcraft.port.research.*;

public final class TheoryItems {
    // ContainerResearchTable uses InventoryUtils with ore matching enabled, even for
    // concrete card stacks. Keep the old named groups specific: a copper ingot is not
    // an iron ingot, whereas another wool or nitor colour is a valid substitute.
    private static final Map<String,String> ORIGINAL_EQUIVALENTS=Map.ofEntries(
        Map.entry("minecraft:white_wool","minecraft:wool"),
        Map.entry("thaumcraft:nitor_yellow","thaumcraft:nitor"),
        Map.entry("minecraft:iron_ingot","c:ingots/iron"),
        Map.entry("minecraft:gold_ingot","c:ingots/gold"),
        Map.entry("thaumcraft:ingot_thaumium","c:ingots/thaumium"),
        Map.entry("thaumcraft:ingot_brass","c:ingots/brass"),
        Map.entry("thaumcraft:amber","c:gems/amber"),
        Map.entry("minecraft:diamond","c:gems/diamond"),
        Map.entry("minecraft:emerald","c:gems/emerald"),
        Map.entry("minecraft:quartz","c:gems/quartz"),
        Map.entry("minecraft:redstone","c:dusts/redstone"),
        Map.entry("minecraft:glowstone_dust","c:dusts/glowstone"),
        Map.entry("minecraft:brick","c:bricks/normal"),
        Map.entry("minecraft:gunpowder","c:gunpowders"),
        Map.entry("minecraft:leather","c:leathers"),
        Map.entry("minecraft:feather","c:feathers"),
        Map.entry("minecraft:egg","c:eggs"));
    public static ItemStack stack(TheoryCards.Requirement q){
        if(q.item().equals("thaumcraft:celestial_notes"))return CelestialNotesItem.stack(q.kind());
        if(!q.aspect().isEmpty()){var a=Aspect.byId(q.aspect()).orElseThrow();if(q.item().equals("thaumcraft:crystal_essence"))return CrystalItem.stack(a);if(q.item().equals("thaumcraft:phial"))return EssentiaItem.phial(a);}
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(q.item())));
    }
    public static boolean matches(ItemStack stack,TheoryCards.Requirement q){
        if(stack.isEmpty())return false;
        if(!BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(q.item())){
            String equivalent=ORIGINAL_EQUIVALENTS.get(q.item());
            if(!q.aspect().isEmpty()||q.kind()>=0||equivalent==null||!stack.is(TagKey.create(Registries.ITEM,ResourceLocation.parse(equivalent))))return false;
        }
        if(stack.isDamageableItem()&&stack.getDamageValue()!=0)return false;
        if(q.kind()>=0&&stack.getOrDefault(PortContent.CELESTIAL_KIND.get(),0)!=q.kind())return false;
        if(!q.aspect().isEmpty())return q.item().equals("thaumcraft:crystal_essence")?CrystalItem.aspect(stack).id().equals(q.aspect()):EssentiaItem.aspect(stack)!=null&&EssentiaItem.aspect(stack).id().equals(q.aspect());
        return true;
    }
    /** Reserve distinct units even when two requirements refer to the same component aspect. */
    public static int[] reserve(Container inventory,List<TheoryCards.Requirement> required){
        int[] used=new int[inventory instanceof net.minecraft.world.entity.player.Inventory?36:inventory.getContainerSize()],take=new int[used.length];
        for(var q:required){boolean found=false;for(int i=0;i<used.length;i++)if(inventory.getItem(i).getCount()>used[i]&&matches(inventory.getItem(i),q)){used[i]++;if(q.consumed())take[i]++;found=true;break;}if(!found)return null;}return take;
    }
    public static Random random(ServerPlayer player){var source=player.getRandom();return new Random(){
        public int nextInt(int bound){return source.nextInt(bound);}public float nextFloat(){return source.nextFloat();}public boolean nextBoolean(){return source.nextBoolean();}public long nextLong(){return source.nextLong();}
    };}
    public static TheoryCards.Context context(ServerPlayer p){return new TheoryCards.Context(){
        public ResearchState research(){return ResearchStorage.get(p.getServer()).player(p.getUUID());}
        public int levels(){return p.experienceLevel;}
        public List<String> curios(){var ids=new ArrayList<String>();for(var stack:p.getInventory().items)if(stack.getItem() instanceof ResearchCurioItem)ids.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());return ids;}
        public int aspectTotal(String id){return RecipeAspects.get(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id))),p.serverLevel()).values().stream().mapToInt(Integer::intValue).sum();}
    };}
    private TheoryItems(){}
}
