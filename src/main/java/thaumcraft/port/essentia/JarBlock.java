package thaumcraft.port.essentia;

import java.util.List;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.*;
import thaumcraft.port.world.AuraSystem;

public final class JarBlock extends Block implements EntityBlock {
    public JarBlock(Properties p){super(p);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return box(3,0,3,13,14,13);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new JarEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return t==EssentiaContent.JAR_ENTITY.get()?(w,p,st,e)->((JarEntity)e).tick():null;}
    @Override protected List<ItemStack> getDrops(BlockState s,LootParams.Builder params){
        var drops=new java.util.ArrayList<>(super.getDrops(s,params));
        if(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof JarEntity be&&!be.alembic){
            boolean dropped=false;for(var stack:drops)if(stack.is(asItem())){be.savePortable(stack,params.getLevel().registryAccess());dropped=true;}
            if(dropped&&be.braced)drops.add(new ItemStack(EssentiaContent.BRACE.get()));
        }
        return drops;
    }
    @Override public ItemStack getCloneItemStack(LevelReader l,BlockPos p,BlockState s){var stack=new ItemStack(this);if(l.getBlockEntity(p) instanceof JarEntity be&&!be.alembic)be.savePortable(stack,l.registryAccess());return stack;}
    @Override public void setPlacedBy(Level l,BlockPos p,BlockState s,net.minecraft.world.entity.LivingEntity entity,ItemStack stack){
        if(!l.isClientSide&&entity!=null&&l.getBlockEntity(p) instanceof JarEntity be){be.labelFacing=entity.getDirection().getOpposite();be.sync();}
    }
    private static void exchange(Player player,InteractionHand hand,ItemStack held,ItemStack result){
        held.shrink(1);if(held.isEmpty())player.setItemInHand(hand,result);else if(!player.getInventory().add(result))player.drop(result,false);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack held,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(!(l.getBlockEntity(p) instanceof JarEntity be))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(held.is(EssentiaContent.BRACE.get())){
            if(be.alembic||be.braced)return ItemInteractionResult.FAIL;
            if(!l.isClientSide&&be.attachBrace()){held.shrink(1);l.playSound(null,p,PortSounds.get("key"),SoundSource.BLOCKS,1,1);}
            return ItemInteractionResult.sidedSuccess(l.isClientSide);
        }
        if(held.is(EssentiaContent.PHIAL.get())){
            Aspect a=EssentiaItem.aspect(held);
            boolean taking=a==null;
            boolean valid=taking?be.tank.amount()>=10:!be.alembic&&be.tank.amount()<=240&&be.fill(a,10,true)==10;
            if(!valid)return ItemInteractionResult.FAIL;
            if(!l.isClientSide){
                if(taking){a=be.tank.aspect();be.tank.extract(a,10,false);exchange(player,hand,held,EssentiaItem.phial(a));}
                else {be.fill(a,10,false);exchange(player,hand,held,EssentiaItem.phial(null));}
                be.sync();l.playSound(null,p,SoundEvents.BOTTLE_FILL,SoundSource.BLOCKS,.25f,1);
            }
            return ItemInteractionResult.sidedSuccess(l.isClientSide);
        }
        if(held.is(EssentiaContent.LABEL.get())&&be.tank.filter()==null){
            if(be.alembic&&!hit.getDirection().getAxis().isHorizontal())return ItemInteractionResult.FAIL;
            Aspect a=EssentiaItem.aspect(held);if(a==null)a=be.tank.aspect();
            if(a==null||(be.tank.amount()>0&&a!=be.tank.aspect()))return ItemInteractionResult.FAIL;
            if(!l.isClientSide){be.labelFacing=be.alembic?hit.getDirection():player.getDirection().getOpposite();be.tank.filter(a);l.updateNeighborsAt(p,s.getBlock());if(!player.isCreative())held.shrink(1);be.sync();l.playSound(null,p,PortSounds.get("page"),SoundSource.BLOCKS,.4f,1);}
            return ItemInteractionResult.sidedSuccess(l.isClientSide);
        }
        if(held.is(PortContent.THAUMOMETER.get())){describe(be,player);return ItemInteractionResult.sidedSuccess(l.isClientSide);}
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(l.getBlockEntity(p) instanceof JarEntity be&&!l.isClientSide){
            if(player.isShiftKeyDown()){
                if(be.tank.filter()!=null&&hit.getDirection()==be.labelFacing){be.tank.filter(null);l.updateNeighborsAt(p,s.getBlock());popResource(l,p,new ItemStack(EssentiaContent.LABEL.get()));l.playSound(null,p,PortSounds.get("page"),SoundSource.BLOCKS,.4f,1);}
                else {if(l instanceof ServerLevel server)AuraSystem.add(server,p,be.tank.amount(),true);be.tank.restore(null,0,be.tank.filter());l.playSound(null,p,PortSounds.get("spill"),SoundSource.BLOCKS,.5f,1);}
                be.sync();
            }else describe(be,player);
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    private static void describe(JarEntity be,Player p){if(be.getLevel()!=null&&!be.getLevel().isClientSide)p.displayClientMessage(Component.literal((be.tank.aspect()==null?"—":be.tank.aspect().displayName())+": "+be.tank.amount()+" / "+be.tank.capacity+"  ↓ "+be.suction(Direction.UP)+(be.tank.filter()==null?"":"  ["+be.tank.filter().displayName()+"]")+(be.braced?"  [brace]":"")),true);}
}
