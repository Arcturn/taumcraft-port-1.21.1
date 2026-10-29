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
            "minecraft:map","minecraft:map","thaumcraft:leather","minecraft:leather","thaumcraft:thaumium_pick","thaumcraft:thaumium_pickaxe",
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
            if(id.equals("oredict:chest"))return new LegacyRequirement(source,count,"minecraft:chest",s->s.is(net.minecraft.world.item.Items.CHEST));
            if(id.equals("minecraft:dye"))id=meta==15?"minecraft:bone_meal":meta==0?"minecraft:ink_sac":"";
            else if(id.equals("thaumcraft:ingot"))id=switch(meta){case 0->"thaumcraft:ingot_thaumium";case 1->"thaumcraft:ingot_void";case 2->"thaumcraft:ingot_brass";default->"";};
            else if(id.equals("thaumcraft:metal"))id=switch(meta){case 2->"thaumcraft:metal_brass";case 3->"thaumcraft:metal_thaumium";default->"";};
            else if(id.equals("thaumcraft:nugget")&&meta==9)id="thaumcraft:nugget_quartz";
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
        return stack.getItem() instanceof CrystalItem?id+"/"+CrystalItem.aspect(stack).id():id;
    }
    private static LegacyRequirement unsupported(String source) {return new LegacyRequirement(source,1,"unsupported:"+source,s->false);}
}
