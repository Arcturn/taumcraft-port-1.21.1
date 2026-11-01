package thaumcraft.port.research;

import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import thaumcraft.port.content.CrystalItem;

/** Conservative conversion: unsupported legacy metadata/NBT never becomes a free requirement. */
public record LegacyRequirement(String source,int count,String craftKey,Predicate<ItemStack> matches) {
    private static final Map<String,String> IDS=Map.of("minecraft:noteblock","minecraft:note_block","minecraft:web","minecraft:cobweb",
            "minecraft:map","minecraft:map","thaumcraft:leather","minecraft:leather",
            "thaumcraft:nitor_silver","thaumcraft:nitor_light_gray");
    public static LegacyRequirement parse(String source) {
        try {
            var parts=source.split(";",4);String id=IDS.getOrDefault(parts[0],parts[0]);
            int count=parts.length>1?Integer.parseInt(parts[1]):1,meta=parts.length>2?Integer.parseInt(parts[2]):0;
            if(count<1||count>64)return unsupported(source);
            if(id.endsWith(":enchanted_placeholder")&&parts.length==4){
                var m=Pattern.compile("id:(\\d+)s?,lvl:(\\d+)s?").matcher(parts[3]);var needed=new java.util.HashMap<String,Integer>();
                while(m.find()){String ench=switch(Integer.parseInt(m.group(1))){case 0->"protection";case 16->"sharpness";case 33->"silk_touch";case 35->"fortune";default->"";};if(ench.isEmpty())return unsupported(source);needed.put(ench,Integer.parseInt(m.group(2)));}
                if(needed.isEmpty())return unsupported(source);
                return new LegacyRequirement(source,count,"enchantment:"+needed,s->{var actual=s.getOrDefault(s.is(net.minecraft.world.item.Items.ENCHANTED_BOOK)?net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS:net.minecraft.core.component.DataComponents.ENCHANTMENTS,net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);return needed.entrySet().stream().allMatch(n->actual.entrySet().stream().anyMatch(a->a.getKey().unwrapKey().map(k->k.location().toString().equals("minecraft:"+n.getKey())).orElse(false)&&a.getIntValue()>=n.getValue()));});
            }
            // Forge's original "chest" ore entry includes ordinary, trapped and ender
            // chests; use its modern common tag so other mods retain that integration.
            if(id.equals("oredict:chest"))return new LegacyRequirement(source,count,"minecraft:chest",s->s.is(net.neoforged.neoforge.common.Tags.Items.CHESTS));
            if(id.equals("minecraft:dye"))id=meta==15?"minecraft:bone_meal":meta==0?"minecraft:ink_sac":"";
            else if(id.equals("thaumcraft:ingot"))id=switch(meta){case 0->"thaumcraft:ingot_thaumium";case 1->"thaumcraft:ingot_void";case 2->"thaumcraft:ingot_brass";default->"";};
            else if(id.equals("thaumcraft:baubles"))id=switch(meta){case 0->"thaumcraft:amulet_mundane";case 1->"thaumcraft:ring_mundane";case 2->"thaumcraft:girdle_mundane";case 3->"thaumcraft:ring_apprentice";case 4->"thaumcraft:amulet_fancy";case 5->"thaumcraft:ring_fancy";case 6->"thaumcraft:girdle_fancy";default->"";};
            else if(id.equals("thaumcraft:stone_arcane"))id="thaumcraft:arcane_stone";
            else if(id.equals("thaumcraft:stone_arcane_brick"))id="thaumcraft:arcane_brick";
            // The old smelter pages retain the pre-flattening metal ID. Their
            // descriptions and matching plate requirements identify thaumium/void.
            else if(id.equals("thaumcraft:metal"))id=switch(meta){case 2->"thaumcraft:metal_thaumium";case 3->"thaumcraft:metal_void";default->"";};
            else if(id.equals("thaumcraft:nugget"))id=switch(meta){case 0->"minecraft:iron_nugget";case 5->"thaumcraft:nugget_quicksilver";case 6->"thaumcraft:nugget_thaumium";case 7->"thaumcraft:nugget_void";case 8->"thaumcraft:nugget_brass";case 9->"thaumcraft:nugget_quartz";case 10->"thaumcraft:rareearth";default->"";};
            else if(id.equals("thaumcraft:cluster"))id=switch(meta){case 0->"thaumcraft:cluster_iron";case 1->"thaumcraft:cluster_gold";case 2->"thaumcraft:cluster_copper";case 3->"thaumcraft:cluster_tin";case 4->"thaumcraft:cluster_silver";case 5->"thaumcraft:cluster_lead";case 6->"thaumcraft:cluster_cinnabar";case 7->"thaumcraft:cluster_quartz";default->"";};
            else if(id.equals("thaumcraft:chunk"))id=meta>=0&&meta<6?"thaumcraft:chunk_"+thaumcraft.port.infusion.recipe.InfusionFood.KINDS.get(meta):"";
            else if(id.equals("thaumcraft:nitor")&&meta==4)id="thaumcraft:nitor_yellow";
            else if(id.equals("thaumcraft:plate"))id=switch(meta){case 0->"thaumcraft:plate_brass";case 1->"thaumcraft:plate_iron";case 2->"thaumcraft:plate_thaumium";case 3->"thaumcraft:plate_void";default->"";};
            else if(id.equals("thaumcraft:phial")&&meta==1&&parts.length==4){
                var matcher=Pattern.compile("key:['\"]([a-z]+)['\"]").matcher(parts[3]);if(!matcher.find())return unsupported(source);
                String aspect=matcher.group(1);return new LegacyRequirement(source,count,id+"/"+aspect,s->s.is(thaumcraft.port.essentia.EssentiaContent.PHIAL.get())&&thaumcraft.port.essentia.EssentiaItem.aspect(s)!=null&&thaumcraft.port.essentia.EssentiaItem.aspect(s).id().equals(aspect));
            }
            else if(id.equals("thaumcraft:phial")&&meta==0&&parts.length<4)return new LegacyRequirement(source,count,id,s->s.is(thaumcraft.port.essentia.EssentiaContent.PHIAL.get())&&thaumcraft.port.essentia.EssentiaItem.aspect(s)==null);
            else if(id.equals("thaumcraft:curio")&&meta>=0&&meta<7)id="thaumcraft:curio_"+thaumcraft.port.theory.TheoryCards.CURIOS.get(meta);
            else if(id.equals("thaumcraft:celestial_notes")&&meta>=0&&meta<13){final int kind=meta;return new LegacyRequirement(source,count,id+"/"+meta,s->s.is(thaumcraft.port.content.PortContent.CELESTIAL_NOTES.get())&&s.getOrDefault(thaumcraft.port.content.PortContent.CELESTIAL_KIND.get(),0)==kind);}
            else if(meta!=0)return unsupported(source);
            if(parts.length==4) {
                if(id.equals("thaumcraft:verdant_charm")){
                    var matcher=Pattern.compile("type:(\\d+)[bs]?").matcher(parts[3]);if(!matcher.find())return unsupported(source);
                    int type=Integer.parseInt(matcher.group(1));if(type>2)return unsupported(source);
                    var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
                    return new LegacyRequirement(source,count,id+"/"+type,s->s.is(item)&&thaumcraft.port.infusion.recipe.InfusionAugments.type(s)==type);
                }
                if(!id.equals("thaumcraft:crystal_essence"))return unsupported(source);
                var matcher=Pattern.compile("key:['\"]([a-z]+)['\"]").matcher(parts[3]);if(!matcher.find())return unsupported(source);
                String aspect=matcher.group(1);
                return new LegacyRequirement(source,count,id+"/"+aspect,s->s.getItem() instanceof CrystalItem&&CrystalItem.aspect(s).id().equals(aspect));
            }
            var location=ResourceLocation.tryParse(id);
            if(location==null||!BuiltInRegistries.ITEM.containsKey(location))return unsupported(source);
            var item=BuiltInRegistries.ITEM.get(location);if(item==net.minecraft.world.item.Items.AIR)return unsupported(source);
            return new LegacyRequirement(source,count,id,s->s.is(item));
        } catch(RuntimeException e) {return unsupported(source);}
    }
    public static String craftKey(ItemStack stack) {
        String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        if(stack.is(thaumcraft.port.essentia.EssentiaContent.PHIAL.get())&&thaumcraft.port.essentia.EssentiaItem.aspect(stack)!=null)return id+"/"+thaumcraft.port.essentia.EssentiaItem.aspect(stack).id();
        if(id.equals("thaumcraft:verdant_charm"))return id+"/"+thaumcraft.port.infusion.recipe.InfusionAugments.type(stack);
        return stack.getItem() instanceof CrystalItem?id+"/"+CrystalItem.aspect(stack).id():id;
    }
    private static LegacyRequirement unsupported(String source) {return new LegacyRequirement(source,1,"unsupported:"+source,s->false);}
}
