package thaumcraft.port.artifice.device;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import net.neoforged.neoforge.capabilities.Capabilities;

public final class ArtificeDeviceBlock extends Block implements EntityBlock {
    public enum Kind { LEVITATOR, REDSTONE_RELAY, ARCANE_EAR, ARCANE_EAR_TOGGLE, DIOPTRA, PATTERN_CRAFTER, VIS_GENERATOR }
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public static final BooleanProperty ENABLED=BooleanProperty.create("enabled");
    public final Kind kind;
    public ArtificeDeviceBlock(Kind kind,Properties properties) {
        super(properties.noOcclusion().sound(kind==Kind.REDSTONE_RELAY||kind==Kind.VIS_GENERATOR?SoundType.WOOD:kind==Kind.PATTERN_CRAFTER?SoundType.METAL:kind==Kind.DIOPTRA?SoundType.STONE:SoundType.WOOD));this.kind=kind;
        registerDefaultState(stateDefinition.any().setValue(FACING,kind==Kind.REDSTONE_RELAY||kind==Kind.PATTERN_CRAFTER?Direction.NORTH:Direction.UP)
                .setValue(ENABLED,!isEar()&&kind!=Kind.REDSTONE_RELAY));
    }
    public boolean isEar(){return kind==Kind.ARCANE_EAR||kind==Kind.ARCANE_EAR_TOGGLE;}
    private boolean redstoneDisables(){return kind==Kind.LEVITATOR||kind==Kind.VIS_GENERATOR||kind==Kind.PATTERN_CRAFTER;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,ENABLED);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new ArtificeDeviceEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return t==ArtificeDevices.ENTITY.get()?(ll,p,ss,be)->((ArtificeDeviceEntity)be).tick():null;}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        Direction f=placementFacing(c);
        if(c.getPlayer()!=null&&c.getPlayer().isShiftKeyDown())f=f.getOpposite();
        if(isEar())f=c.getClickedFace();
        if(kind==Kind.REDSTONE_RELAY)f=c.getPlayer()!=null&&c.getPlayer().isShiftKeyDown()?c.getHorizontalDirection():c.getHorizontalDirection().getOpposite();
        if(kind==Kind.PATTERN_CRAFTER)f=c.getHorizontalDirection();
        if(kind==Kind.DIOPTRA)f=Direction.UP;
        if(kind==Kind.VIS_GENERATOR)for(Direction d:Direction.values()) {
            var energy=c.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK,c.getClickedPos().relative(d),d.getOpposite());
            if(energy!=null&&energy.canReceive()){f=d;break;}
        }
        BlockState s=defaultBlockState().setValue(FACING,f);
        if(redstoneDisables())s=s.setValue(ENABLED,!c.getLevel().hasNeighborSignal(c.getClickedPos()));
        return s.canSurvive(c.getLevel(),c.getClickedPos())?s:null;
    }
    private static Direction placementFacing(BlockPlaceContext context){
        var player=context.getPlayer();if(player==null)return context.getNearestLookingDirection().getOpposite();
        BlockPos pos=context.getClickedPos();
        if(Math.abs(player.getX()-pos.getX()-.5)<2&&Math.abs(player.getZ()-pos.getZ()-.5)<2){
            double eye=player.getY()+player.getEyeHeight();if(eye-pos.getY()>2)return Direction.UP;if(pos.getY()>eye)return Direction.DOWN;
        }
        return player.getDirection().getOpposite();
    }
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){
        if(isEar()){Direction d=s.getValue(FACING);return l.getBlockState(p.relative(d.getOpposite())).isFaceSturdy(l,p.relative(d.getOpposite()),d);}
        return kind!=Kind.REDSTONE_RELAY||l.getBlockState(p.below()).isFaceSturdy(l,p.below(),Direction.UP);
    }
    @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moving){
        if(!old.is(this)&&!l.isClientSide){
            if(kind==Kind.REDSTONE_RELAY){if(shouldPower(l,p,s))l.scheduleTick(p,this,1);notifyOutput(l,p,s);}
            else neighborChanged(s,l,p,this,p,false);
        }
    }
    @Override protected void neighborChanged(BlockState s,Level l,BlockPos p,Block n,BlockPos from,boolean moving){
        if(l.isClientSide)return;
        if(!s.canSurvive(l,p)){l.destroyBlock(p,true);return;}
        if(isEar()&&l.getBlockEntity(p) instanceof ArtificeDeviceEntity be)be.updateTone();
        if(redstoneDisables()){boolean on=!l.hasNeighborSignal(p);if(on!=s.getValue(ENABLED))l.setBlock(p,s.setValue(ENABLED,on),3);}
        if(kind==Kind.REDSTONE_RELAY)updateRelay(l,p,s);
    }
    public void updateRelay(Level l,BlockPos p,BlockState s){
        if(shouldPower(l,p,s)!=s.getValue(ENABLED)&&!l.getBlockTicks().hasScheduledTick(p,this)) {
            Direction out=s.getValue(FACING).getOpposite();BlockState next=l.getBlockState(p.relative(out));
            net.minecraft.world.ticks.TickPriority priority=s.getValue(ENABLED)?net.minecraft.world.ticks.TickPriority.VERY_HIGH:net.minecraft.world.ticks.TickPriority.HIGH;
            if(next.getBlock() instanceof DiodeBlock&&next.getValue(BlockStateProperties.HORIZONTAL_FACING)!=out)priority=net.minecraft.world.ticks.TickPriority.EXTREMELY_HIGH;
            l.scheduleTick(p,this,2,priority);
        }
    }
    public boolean shouldPower(Level l,BlockPos p,BlockState s){
        Direction d=s.getValue(FACING);BlockPos input=p.relative(d);int power=l.getSignal(input,d);
        BlockState wire=l.getBlockState(input);if(wire.is(Blocks.REDSTONE_WIRE))power=Math.max(power,wire.getValue(RedStoneWireBlock.POWER));
        int threshold=l.getBlockEntity(p) instanceof ArtificeDeviceEntity be?be.input:1;return power>=threshold;
    }
    @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource random){
        if(kind!=Kind.REDSTONE_RELAY)return;boolean power=shouldPower(l,p,s);
        if(s.getValue(ENABLED)&&!power){l.setBlock(p,s.setValue(ENABLED,false),3);notifyOutput(l,p,s);}
        else if(!s.getValue(ENABLED)){l.setBlock(p,s.setValue(ENABLED,true),3);notifyOutput(l,p,s);if(!power)l.scheduleTick(p,this,2);}
    }
    public static void notifyOutput(Level l,BlockPos p,BlockState s){l.updateNeighborsAt(p,s.getBlock());l.updateNeighborsAt(p.relative(s.getValue(FACING).getOpposite()),s.getBlock());}
    @Override protected boolean isSignalSource(BlockState s){return isEar()||kind==Kind.REDSTONE_RELAY;}
    @Override protected int getSignal(BlockState s,BlockGetter l,BlockPos p,Direction d){
        if(!s.getValue(ENABLED))return 0;if(isEar())return 15;
        return kind==Kind.REDSTONE_RELAY&&d==s.getValue(FACING)&&l.getBlockEntity(p) instanceof ArtificeDeviceEntity be?be.output:0;
    }
    @Override protected int getDirectSignal(BlockState s,BlockGetter l,BlockPos p,Direction d){return getSignal(s,l,p,d);}
    @Override protected boolean hasAnalogOutputSignal(BlockState s){return kind==Kind.DIOPTRA;}
    @Override protected int getAnalogOutputSignal(BlockState s,Level l,BlockPos p){return l.getBlockEntity(p) instanceof ArtificeDeviceEntity be?be.comparator():0;}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){return interact(s,l,p,player,hit);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        return interact(s,l,p,player,hit).consumesAction()?ItemInteractionResult.sidedSuccess(l.isClientSide):ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    private InteractionResult interact(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(!(l.getBlockEntity(p) instanceof ArtificeDeviceEntity be))return InteractionResult.PASS;
        if(isEar()){if(!l.isClientSide){be.note=(be.note+1)%25;be.sync();be.playNote();}return InteractionResult.sidedSuccess(l.isClientSide);}
        if(kind==Kind.DIOPTRA){if(!l.isClientSide)l.setBlock(p,s.cycle(ENABLED),3);return InteractionResult.sidedSuccess(l.isClientSide);}
        int knob=knobAt(s,p,player);if(knob<0)return InteractionResult.PASS;
        if(kind==Kind.REDSTONE_RELAY&&!player.getAbilities().mayBuild)return InteractionResult.PASS;
        if(!l.isClientSide){
            if(kind==Kind.LEVITATOR){be.range=(be.range+1)%4;be.rangeActual=0;player.displayClientMessage(net.minecraft.network.chat.Component.translatable("tc.levitator",be.rangeLimit(),be.liftCost()),false);}
            if(kind==Kind.PATTERN_CRAFTER)be.pattern=(be.pattern+1)%10;
            if(kind==Kind.REDSTONE_RELAY){if(knob==0)be.output=be.output%15+1;else be.input=be.input%15+1;updateRelay(l,p,s);notifyOutput(l,p,s);}
            be.sync();l.playSound(null,p,thaumcraft.port.content.PortSounds.get("key"),SoundSource.BLOCKS,.5f,1);
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }
    /** Source controls protrude from the body, so ray-pick their own cuboids before the body. */
    public int knobAt(BlockState s,BlockPos p,Player player){
        Vec3 start=player.getEyePosition(),end=start.add(player.getLookAngle().scale(player.blockInteractionRange()));
        AABB[] boxes=knobs(s);for(int i=0;i<boxes.length;i++)if(boxes[i].move(p).clip(start,end).isPresent())return i;return -1;
    }
    public AABB[] knobs(BlockState s){
        Direction d=s.getValue(FACING);
        if(kind==Kind.LEVITATOR)return new AABB[]{rotateUp(new AABB(.375,.0625,.375,.625,.125,.625),d)};
        if(kind==Kind.PATTERN_CRAFTER)return new AABB[]{rotateSouth(new AABB(.375,.125,.125,.625,.375,.25),d)};
        if(kind==Kind.REDSTONE_RELAY)return new AABB[]{rotateSouth(new AABB(.125,.0625,.125,.375,.25,.375),d),rotateSouth(new AABB(.375,.0625,.625,.625,.25,.875),d)};
        return new AABB[0];
    }
    public static AABB rotateSouth(AABB box,Direction d){return transform(box,d,false);}
    public static AABB rotateUp(AABB box,Direction d){return transform(box,d,true);}
    private static AABB transform(AABB b,Direction d,boolean up){
        double minX=2,minY=2,minZ=2,maxX=-1,maxY=-1,maxZ=-1;
        for(int i=0;i<8;i++){double x=((i&1)==0?b.minX:b.maxX)-.5,y=((i&2)==0?b.minY:b.maxY)-.5,z=((i&4)==0?b.minZ:b.maxZ)-.5;
            Vec3 v=up?switch(d){case UP->new Vec3(x,y,z);case DOWN->new Vec3(x,-y,-z);case NORTH->new Vec3(x,z,-y);case SOUTH->new Vec3(x,-z,y);case WEST->new Vec3(-y,x,z);case EAST->new Vec3(y,-x,z);}:switch(d){case NORTH->new Vec3(-x,y,-z);case WEST->new Vec3(-z,y,x);case EAST->new Vec3(z,y,-x);default->new Vec3(x,y,z);};
            minX=Math.min(minX,v.x+.5);minY=Math.min(minY,v.y+.5);minZ=Math.min(minZ,v.z+.5);maxX=Math.max(maxX,v.x+.5);maxY=Math.max(maxY,v.y+.5);maxZ=Math.max(maxZ,v.z+.5);
        }return new AABB(minX,minY,minZ,maxX,maxY,maxZ);
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext context){
        if(context instanceof EntityCollisionContext ec&&ec.getEntity() instanceof Player player){int knob=knobAt(s,p,player);if(knob>=0)return Shapes.create(knobs(s)[knob]);}
        return body(s);
    }
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return body(s);}
    private VoxelShape body(BlockState s){
        Direction d=s.getValue(FACING);
        if(isEar())return Shapes.create(rotateUp(new AABB(.125,0,.125,.875,.375,.875),d));
        if(kind==Kind.REDSTONE_RELAY)return box(0,0,0,16,2,16);
        if(kind==Kind.LEVITATOR)return Shapes.create(rotateUp(new AABB(0,.125,0,1,1,1),d));
        if(kind==Kind.VIS_GENERATOR)return Shapes.create(rotateUp(new AABB(.25,0,.25,.75,.875,.75),d));
        return Shapes.block();
    }
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){if(level.getBlockEntity(pos) instanceof ArtificeDeviceEntity be)ArtificeDevices.clientAnimate.accept(be);}
    @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
    @Override protected boolean triggerEvent(BlockState s,Level l,BlockPos p,int id,int value){return l.getBlockEntity(p) instanceof ArtificeDeviceEntity be&&be.triggerEvent(id,value);}
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){super.onRemove(s,l,p,next,moving);if(!s.is(next.getBlock())&&(isEar()||kind==Kind.REDSTONE_RELAY))notifyOutput(l,p,s);}
}
