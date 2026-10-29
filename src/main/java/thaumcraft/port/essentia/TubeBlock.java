package thaumcraft.port.essentia;

import java.util.EnumMap;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.*;

public final class TubeBlock extends Block implements EntityBlock {
    public enum Kind{NORMAL,FILTER,RESTRICT,ONEWAY,VALVE,BUFFER}
    public final Kind kind;
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public static final EnumMap<Direction,BooleanProperty> SIDES=new EnumMap<>(Direction.class);
    static{for(Direction d:Direction.values())SIDES.put(d,BooleanProperty.create(d.getName()));}
    public TubeBlock(Kind kind,Properties p){super(p);this.kind=kind;BlockState s=stateDefinition.any().setValue(FACING,Direction.NORTH);for(var prop:SIDES.values())s=s.setValue(prop,false);registerDefaultState(s);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);for(var prop:SIDES.values())b.add(prop);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getNearestLookingDirection());}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return kind==Kind.BUFFER?new BufferEntity(p,s):new TubeEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return t==EssentiaContent.TUBE_ENTITY.get()?(w,p,st,e)->((TubeEntity)e).tick():null;}
    public void refresh(Level l,BlockPos p){
        if(l.isClientSide||!(l.getBlockEntity(p) instanceof TubeEntity be))return;
        BlockState old=l.getBlockState(p),next=old;
        for(Direction d:Direction.values()){var other=be.neighbour(d);next=next.setValue(SIDES.get(d),be.connects(d)&&other!=null&&other.connects(d.getOpposite()));}
        if(old!=next)l.setBlock(p,next,3);
    }
    @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block b,BlockPos source,boolean moving){refresh(l,p);}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,net.minecraft.world.entity.LivingEntity entity,ItemStack item){refresh(l,p);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        VoxelShape shape=kind==Kind.BUFFER?box(4,4,4,12,12,12):box(6,6,6,10,10,10);
        for(Direction d:Direction.values())if(s.getValue(SIDES.get(d)))shape=Shapes.or(shape,switch(d){case DOWN->box(6,0,6,10,6,10);case UP->box(6,10,6,10,16,10);case NORTH->box(6,6,0,10,10,6);case SOUTH->box(6,6,10,10,10,16);case WEST->box(0,6,6,6,10,10);case EAST->box(10,6,6,16,10,10);});return shape;
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack held,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(!(l.getBlockEntity(p) instanceof TubeEntity be))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(held.is(PortContent.THAUMOMETER.get())){
            if(!l.isClientSide){if(be instanceof BufferEntity buffer){if(player.isShiftKeyDown())buffer.choke(hit.getDirection());else buffer.toggle(hit.getDirection());player.displayClientMessage(Component.literal(Aspect.describe(buffer.contents.values())+" | "+buffer.suction(hit.getDirection())),true);}else if(player.isShiftKeyDown()){be.toggle(hit.getDirection());l.playSound(null,p,PortSounds.get("tool"),SoundSource.BLOCKS,.5f,1);}else describe(be,player);}
            return ItemInteractionResult.sidedSuccess(l.isClientSide);
        }
        if(kind==Kind.FILTER&&(held.is(EssentiaContent.LABEL.get())||held.is(EssentiaContent.PHIAL.get())||held.is(PortContent.CRYSTAL_ESSENCE.get()))){
            Aspect a=held.is(PortContent.CRYSTAL_ESSENCE.get())?CrystalItem.aspect(held):EssentiaItem.aspect(held);
            if(a==null||(be.tank.amount()>0&&a!=be.tank.aspect()))return ItemInteractionResult.FAIL;
            if(!l.isClientSide){be.tank.filter(a);be.sync();}return ItemInteractionResult.sidedSuccess(l.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(!l.isClientSide&&l.getBlockEntity(p) instanceof TubeEntity be){
            if(kind==Kind.VALVE){be.allowFlow=!be.allowFlow;be.suction=0;be.sync();l.playSound(null,p,PortSounds.get("squeek"),SoundSource.BLOCKS,.7f,1);}
            else if(kind==Kind.FILTER&&player.isShiftKeyDown()){be.tank.filter(null);be.sync();}else describe(be,player);
        }return InteractionResult.sidedSuccess(l.isClientSide);
    }
    private static void describe(TubeEntity be,Player p){if(be instanceof BufferEntity buffer){p.displayClientMessage(Component.literal(Aspect.describe(buffer.contents.values())+" / 10 | Bellows: "+buffer.bellows),true);return;}p.displayClientMessage(Component.literal("↓ "+be.suction+" "+(be.suctionType==null?"—":be.suctionType.displayName())+" | "+(be.tank.aspect()==null?"—":be.tank.aspect().displayName())+" × "+be.tank.amount()+(be.venting>0?" !":"")),true);}
}
