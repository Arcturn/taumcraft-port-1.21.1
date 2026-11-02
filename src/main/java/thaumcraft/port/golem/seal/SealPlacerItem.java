package thaumcraft.port.golem.seal;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
public final class SealPlacerItem extends Item {
    private final String key;
    public SealPlacerItem(Properties properties,String key){super(properties);this.key=key.contains(":")?key:"thaumcraft:"+key;}
    public String sealKey(){return key;}
    public static ItemStack getSealStack(String key){var id=ResourceLocation.tryParse(key);if(id==null)return ItemStack.EMPTY;return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),"seal_"+id.getPath())));}
    @Override public boolean doesSneakBypassUse(ItemStack stack,net.minecraft.world.level.LevelReader level,net.minecraft.core.BlockPos pos,net.minecraft.world.entity.player.Player player){return true;}
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){
        var player=context.getPlayer();if(player==null||player.isShiftKeyDown()||key.endsWith(":blank"))return InteractionResult.PASS;
        var level=context.getLevel();var pos=context.getClickedPos();var face=context.getClickedFace();
        if(!player.mayUseItemAt(pos,face,stack)||!level.mayInteract(player,pos))return InteractionResult.FAIL;
        var seal=SealHandler.createSeal(key);if(seal==null||!seal.canPlaceAt(level,pos,face)||SealHandler.getSealEntity(level,new SealPos(pos,face))!=null)return InteractionResult.FAIL;
        if(!level.isClientSide&&SealHandler.addSealEntity(level,pos,face,seal,player)&&!player.isCreative())stack.shrink(1);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
