package thaumcraft.port.infusion.tools;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;

/** The Earthmover's architect placement is distinct from DESTRUCTIVE's mining effect. */
public final class ElementalShovelItem extends ShovelItem {
    public ElementalShovelItem(Properties properties) { super(InfusionTools.ELEMENTAL,properties); }
    public static int orientation(ItemStack stack) {
        return Math.floorMod(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getByte("or"),3);
    }
    public static void cycleOrientation(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA,stack,tag -> tag.putByte("or",(byte)((orientation(stack)+1)%3)));
    }
    public static List<BlockPos> plane(ItemStack stack,BlockPos source,Direction face,Direction facing) {
        var positions=new ArrayList<BlockPos>(9);
        int orientation=orientation(stack);
        for (int a=-1;a<=1;a++) for (int b=-1;b<=1;b++) {
            int x=0,y=0,z=0;
            if (orientation==1) {
                y=b;
                if (face.getAxis()==Direction.Axis.Y) {
                    if (facing.getAxis()==Direction.Axis.Z) x=a;else z=a;
                } else if (face.getAxis()==Direction.Axis.Z) z=a;else x=a;
            } else if (orientation==2) {
                if (face.getAxis()==Direction.Axis.Y) {
                    y=b;
                    if (facing.getAxis()==Direction.Axis.Z) x=a;else z=a;
                } else { z=b;x=a; }
            } else if (face.getAxis()==Direction.Axis.Y) { x=a;z=b; }
            else if (face.getAxis()==Direction.Axis.Z) { x=a;y=b; }
            else { z=a;y=b; }
            positions.add(source.relative(face).offset(x,y,z));
        }
        return positions;
    }
    public static List<BlockPos> preview(ItemStack stack,Level level,BlockPos source,Direction face,Player player) {
        if (!player.isShiftKeyDown() || level.getBlockEntity(source)!=null) return List.of();
        BlockState state=level.getBlockState(source);
        return plane(stack,source,face,player.getDirection()).stream().filter(pos -> canPlace(level,pos,placedState(level,pos,state),player,face,stack)).toList();
    }
    private static boolean canPlace(Level level,BlockPos pos,BlockState state,Player player,Direction face,ItemStack shovel) {
        return !state.isAir() && !state.hasBlockEntity() && !(state.getBlock() instanceof FlowerPotBlock) && !level.isOutsideBuildHeight(pos)
                && level.getWorldBorder().isWithinBounds(pos) && level.hasChunkAt(pos)
                && level.getBlockState(pos).canBeReplaced() && state.canSurvive(level,pos)
                && level.isUnobstructed(state,pos,CollisionContext.of(player))
                && level.mayInteract(player,pos) && player.mayUseItemAt(pos,face,shovel);
    }
    private static BlockState placedState(Level level,BlockPos pos,BlockState source) {
        if (!source.hasProperty(BlockStateProperties.WATERLOGGED)) return source;
        boolean water=level.getFluidState(pos).is(FluidTags.WATER)&&level.getFluidState(pos).isSource();
        if (source.hasProperty(SlabBlock.TYPE)&&source.getValue(SlabBlock.TYPE)==SlabType.DOUBLE) water=false;
        return source.setValue(BlockStateProperties.WATERLOGGED,water);
    }
    private static int materialCount(BlockState state) {
        if (state.hasProperty(SlabBlock.TYPE)&&state.getValue(SlabBlock.TYPE)==SlabType.DOUBLE) return 2;
        for (var property:state.getProperties()) if (property instanceof IntegerProperty integer
                && java.util.Set.of("candles","pickles","eggs","layers").contains(property.getName())) return state.getValue(integer);
        return 1;
    }
    private static boolean matchesMaterial(ItemStack material,BlockState source) {
        if (!material.is(source.getBlock().asItem()) || material.isEmpty()) return false;
        // Before flattening TC6 required an exact block metadata match. A plain item must
        // not clone harvested growth, stored bonemeal, anchor charges or filled cauldrons.
        if (source.getBlock() instanceof AbstractCauldronBlock && !source.is(Blocks.CAULDRON)) return false;
        if (source.getBlock() instanceof FlowerPotBlock pot && pot.getPotted()!=Blocks.AIR) return false;
        if (source.getBlock() instanceof CandleCakeBlock) return false;
        BlockState paid=source.getBlock().defaultBlockState();
        var properties=material.get(DataComponents.BLOCK_STATE);
        if (properties!=null) paid=properties.apply(paid);
        for (var property:source.getProperties()) if (property instanceof IntegerProperty integer
                && !java.util.Set.of("candles","pickles","eggs","layers","power","distance","moisture").contains(property.getName())
                && !source.getValue(integer).equals(paid.getValue(integer))) return false;
        for (var property:source.getProperties()) if (property instanceof BooleanProperty bool
                && java.util.Set.of("berries","can_summon").contains(property.getName())
                && !source.getValue(bool).equals(paid.getValue(bool))) return false;
        return true;
    }
    private static boolean inventoryPayment(Player player,BlockState state,boolean consume) {
        int needed=materialCount(state),available=0;
        for (int i=0;i<player.getInventory().getContainerSize();i++) {
            var material=player.getInventory().getItem(i);
            if (matchesMaterial(material,state)) available+=material.getCount();
        }
        if (available<needed) return false;
        if (consume) for (int i=0;i<player.getInventory().getContainerSize()&&needed>0;i++) {
            var material=player.getInventory().getItem(i);
            if (!matchesMaterial(material,state)) continue;
            int paid=Math.min(needed,material.getCount());material.shrink(paid);needed-=paid;
        }
        return true;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        Player player=context.getPlayer();
        if (player==null) return InteractionResult.PASS;
        Level level=context.getLevel();
        BlockPos source=context.getClickedPos();
        BlockState state=level.getBlockState(source);
        if (state.hasBlockEntity() || level.getBlockEntity(source)!=null || state.isAir()) return InteractionResult.FAIL;
        // The authoritative server owns placement, inventory payment, protection hooks and durability.
        if (level.isClientSide) return InteractionResult.SUCCESS;
        boolean placed=false;
        for (BlockPos pos:plane(context.getItemInHand(),source,context.getClickedFace(),player.getDirection())) {
            if (context.getItemInHand().isEmpty()) break;
            BlockState place=placedState(level,pos,state);
            if (!player.getAbilities().instabuild && !inventoryPayment(player,place,false)) {
                if (!state.is(Blocks.GRASS_BLOCK)) continue;
                place=Blocks.DIRT.defaultBlockState();
                if (!inventoryPayment(player,place,false)) continue;
            }
            if (!canPlace(level,pos,place,player,context.getClickedFace(),context.getItemInHand())) continue;
            BlockSnapshot snapshot=BlockSnapshot.create(level.dimension(),level,pos);
            if (!level.setBlock(pos,place,2)) continue;
            if (EventHooks.onBlockPlace(player,snapshot,context.getClickedFace())) {
                snapshot.restore();
                continue;
            }
            level.updateNeighborsAt(pos,place.getBlock());
            if (!player.getAbilities().instabuild) inventoryPayment(player,place,true);
            var sound=place.getSoundType(level,pos,player);
            level.playSound(null,pos,sound.getPlaceSound(),SoundSource.BLOCKS,.6f,.9f+level.random.nextFloat()*.2f);
            context.getItemInHand().hurtAndBreak(1,player,LivingEntity.getSlotForHand(context.getHand()));
            ElementalToolPayload.placement(level,pos,context.getClickedFace());
            placed=true;
        }
        if (placed) { player.getInventory().setChanged();player.swing(context.getHand(),true); }
        return placed ? InteractionResult.SUCCESS:InteractionResult.FAIL;
    }
}
