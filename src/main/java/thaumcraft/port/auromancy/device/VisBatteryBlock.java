package thaumcraft.port.auromancy.device;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import thaumcraft.port.world.AuraSystem;
import thaumcraft.port.world.WorldConfig;

/** BETA26's ten-point aura buffer. Charge is world block state, not an item component. */
public final class VisBatteryBlock extends Block {
    public static final IntegerProperty CHARGE=IntegerProperty.create("charge",0,10);

    public VisBatteryBlock(Properties properties) {
        super(properties.randomTicks().lightLevel(state->state.getValue(CHARGE)));
        registerDefaultState(stateDefinition.any().setValue(CHARGE,0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(CHARGE);}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){updateCharge(state,level,pos,random);}
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){updateCharge(state,level,pos,random);}
    public void updateCharge(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if(!WorldConfig.AURA.get())return;
        int charge=state.getValue(CHARGE);
        if(level.hasNeighborSignal(pos)) {
            if(charge>0){AuraSystem.add(level,pos,1,false);changeCharge(level,pos,state,charge-1,5);}
            return;
        }
        var aura=AuraSystem.get(level,pos);if(aura==null)return;
        if(charge<10&&aura.vis()>aura.base()*.9&&aura.vis()>1) {
            if(AuraSystem.drain(level,pos,1,false,false)==1)changeCharge(level,pos,state,charge+1,100+random.nextInt(100));
        } else if(charge>0&&aura.vis()<aura.base()*.75) {
            AuraSystem.add(level,pos,1,false);changeCharge(level,pos,state,charge-1,20+random.nextInt(20));
        }
    }
    private void changeCharge(ServerLevel level,BlockPos pos,BlockState state,int charge,int delay) {
        level.setBlockAndUpdate(pos,state.setValue(CHARGE,charge));
        level.updateNeighbourForOutputSignal(pos,this);
        level.scheduleTick(pos,this,delay);
    }
    @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block neighbor,BlockPos neighborPos,boolean moving) {
        if(!level.isClientSide&&level.hasNeighborSignal(pos))level.scheduleTick(pos,this,1);
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state){return true;}
    @Override protected int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos){return state.getValue(CHARGE);}
}
