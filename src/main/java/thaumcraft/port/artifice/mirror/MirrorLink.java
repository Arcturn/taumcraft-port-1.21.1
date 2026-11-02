package thaumcraft.port.artifice.mirror;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/** A destination survives item placement, chunk saves and changes of dimension. */
public record MirrorLink(ResourceKey<Level> dimension, BlockPos pos) {
    private static final String KEY="MirrorLink";
    public MirrorLink { pos=pos.immutable(); }
    public CompoundTag save(){var tag=new CompoundTag();tag.putString("dimension",dimension.location().toString());tag.putLong("pos",pos.asLong());return tag;}
    public static MirrorLink read(CompoundTag tag){
        var id=ResourceLocation.tryParse(tag.getString("dimension"));
        return id==null||!tag.contains("pos",net.minecraft.nbt.Tag.TAG_LONG)?null:new MirrorLink(ResourceKey.create(Registries.DIMENSION,id),BlockPos.of(tag.getLong("pos")));
    }
    public static MirrorLink get(ItemStack stack){return read(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound(KEY));}
    public static void set(ItemStack stack,MirrorLink link){CustomData.update(DataComponents.CUSTOM_DATA,stack,t->{if(link==null)t.remove(KEY);else t.put(KEY,link.save());});}
    public static void tooltip(ItemStack stack,List<Component> lines){var link=get(stack);if(link!=null)lines.add(Component.translatable("tc.handmirrorlinkedto").append(" "+link.pos.getX()+","+link.pos.getY()+","+link.pos.getZ()+" in "+link.dimension.location()).withStyle(net.minecraft.ChatFormatting.GRAY));}
}
