package thaumcraft.port.artifice.mirror;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import thaumcraft.port.content.PortSounds;

public final class HandMirrorItem extends Item {
    public HandMirrorItem(Properties properties){super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));}
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){
        if(!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof MirrorEntity mirror)||mirror.essentia())return InteractionResult.PASS;
        if(!context.getLevel().isClientSide){
            MirrorLink.set(stack,mirror.address());context.getLevel().playSound(null,context.getClickedPos(),PortSounds.get("jar"),SoundSource.BLOCKS,1,2);
            if(context.getPlayer()!=null){context.getPlayer().displayClientMessage(Component.translatable("tc.handmirrorlinked"),false);context.getPlayer().getInventory().setChanged();}
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);if(MirrorLink.get(stack)==null)return InteractionResultHolder.pass(stack);
        if(player instanceof ServerPlayer server){
            if(destination(stack,server,true)==null)return InteractionResultHolder.fail(stack);
            server.openMenu(new SimpleMenuProvider((id,inventory,p)->new HandMirrorMenu(id,inventory,hand),stack.getHoverName()),buffer->buffer.writeEnum(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    public static MirrorEntity destination(ItemStack mirror,ServerPlayer player,boolean clearBroken){
        var link=MirrorLink.get(mirror);if(link==null)return null;var world=player.getServer().getLevel(link.dimension());
        if(world==null||!world.hasChunkAt(link.pos()))return null;
        var target=MirrorEntity.resolve(player.serverLevel(),link);if(target!=null&&!target.essentia())return target;
        if(clearBroken){MirrorLink.set(mirror,null);player.playNotifySound(PortSounds.get("zap"),SoundSource.PLAYERS,1,.8f);player.displayClientMessage(Component.translatable("tc.handmirrorerror"),false);player.getInventory().setChanged();}
        return null;
    }
    public static boolean transport(ItemStack mirror,ItemStack items,ServerPlayer player){
        if(!(mirror.getItem() instanceof HandMirrorItem)||items.isEmpty()||items.getItem() instanceof HandMirrorItem)return false;
        var target=destination(mirror,player,true);if(target==null||!target.enqueue(items))return false;
        player.playNotifySound(SoundEvents.ITEM_PICKUP,SoundSource.PLAYERS,.1f,1);return true;
    }
    @Override public boolean isFoil(ItemStack stack){return MirrorLink.get(stack)!=null||super.isFoil(stack);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flags){super.appendHoverText(stack,context,lines,flags);MirrorLink.tooltip(stack,lines);}
}
