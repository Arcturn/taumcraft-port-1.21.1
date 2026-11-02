package thaumcraft.port.artifice.construct;

import net.minecraft.core.*;
import net.minecraft.sounds.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;

public final class ConstructPlacerItem extends Item {
    public enum Kind { BASIC, ADVANCED, BORE }
    private final Kind kind;
    public ConstructPlacerItem(Properties properties,Kind kind){super(properties);this.kind=kind;}
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){return place(context);}
    @Override public InteractionResult useOn(UseOnContext context){return place(context);}
    private InteractionResult place(UseOnContext context){
        var level=context.getLevel();var player=context.getPlayer();var face=context.getClickedFace();if(player==null||face==Direction.DOWN)return InteractionResult.PASS;
        BlockPos pos=context.getClickedPos();if(!level.getBlockState(pos).canBeReplaced())pos=pos.relative(face);
        if(!player.mayUseItemAt(pos,face,context.getItemInHand())||!level.mayInteract(player,pos)||!level.getBlockState(pos).canBeReplaced()||!level.getBlockState(pos.above()).canBeReplaced()||!level.getEntities(null,new AABB(pos).expandTowards(0,1,0)).isEmpty())return InteractionResult.PASS;
        if(!level.isClientSide){
            OwnedConstruct construct=switch(kind){case BASIC->ConstructContent.BASIC.get().create(level);case ADVANCED->ConstructContent.ADVANCED.get().create(level);case BORE->ConstructContent.BORE.get().create(level);};
            if(construct==null)return InteractionResult.FAIL;
            construct.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);construct.setOwnerUUID(player.getUUID());construct.setValidSpawn();
            if(construct instanceof ArcaneBoreEntity bore)bore.setFacing(player.getDirection());
            level.removeBlock(pos,false);level.removeBlock(pos.above(),false);level.addFreshEntity(construct);level.playSound(null,pos,SoundEvents.ARMOR_STAND_PLACE,SoundSource.BLOCKS,.75f,.8f);context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
