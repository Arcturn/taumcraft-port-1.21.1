package thaumcraft.port.infusion.structure;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

public final class InlayBlock extends Block implements InfusionStabilizer {
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 15);
    public enum Connection implements StringRepresentable { NONE, SIDE, EXT; @Override public String getSerializedName() { return name().toLowerCase(java.util.Locale.ROOT); } }
    public static final EnumProperty<Connection> NORTH = EnumProperty.create("north", Connection.class), EAST = EnumProperty.create("east", Connection.class), SOUTH = EnumProperty.create("south", Connection.class), WEST = EnumProperty.create("west", Connection.class);
    public static final Map<Direction, EnumProperty<Connection>> CONNECTIONS = Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST);
    public InlayBlock(Properties properties) {
        super(properties); registerDefaultState(stateDefinition.any().setValue(CHARGE, 0).setValue(NORTH, Connection.NONE).setValue(EAST, Connection.NONE).setValue(SOUTH, Connection.NONE).setValue(WEST, Connection.NONE));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(CHARGE, NORTH, EAST, SOUTH, WEST); }
    public static BlockState connections(LevelReader level, BlockPos pos, BlockState state) {
        for (Direction face : Direction.Plane.HORIZONTAL) {
            BlockPos adjacent = pos.relative(face);
            BlockState other = level.hasChunkAt(adjacent) ? level.getBlockState(adjacent) : Blocks.AIR.defaultBlockState();
            state = state.setValue(CONNECTIONS.get(face), other.getBlock() instanceof StabilizerBlock ? Connection.EXT : InlayNetwork.isConductor(other) ? Connection.SIDE : Connection.NONE);
        }
        return state;
    }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) { return level.getBlockState(pos.below()).isSolidRender(level, pos.below()); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return connections(context.getLevel(), context.getClickedPos(), defaultBlockState()); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean north=state.getValue(NORTH)!=Connection.NONE, east=state.getValue(EAST)!=Connection.NONE;
        boolean south=state.getValue(SOUTH)!=Connection.NONE, west=state.getValue(WEST)!=Connection.NONE;
        // A lone attachment extends its line across the complete block, as in TC6's AABB table.
        boolean n=north || south&&!east&&!west, s=south || north&&!east&&!west;
        boolean e=east || west&&!north&&!south, w=west || east&&!north&&!south;
        double minX=w?0:3, maxX=e?16:13, minZ=n?0:3, maxZ=s?16:13;
        return box(minX, 0, minZ, maxX, 1, maxZ);
    }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moving) { InlayNetwork.update(level, pos); }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (!level.isClientSide && !state.canSurvive(level, pos)) level.destroyBlock(pos, true);
        else InlayNetwork.update(level, pos);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        super.onRemove(state, level, pos, next, moving);
        if (!state.is(next.getBlock())) for (Direction face : Direction.Plane.HORIZONTAL) InlayNetwork.update(level, pos.relative(face));
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int charge = state.getValue(CHARGE);
        if (charge > 0 && random.nextInt(20 - charge) == 0) StructureEffects.ambient.accept(level, pos, charge, false);
    }
    @Override public float stabilization(Level level, BlockPos pos) { return .025f; }
}
