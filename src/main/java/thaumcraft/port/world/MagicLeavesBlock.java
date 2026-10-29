package thaumcraft.port.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class MagicLeavesBlock extends LeavesBlock {
    private final boolean silver;
    public MagicLeavesBlock(boolean silver,Properties p){super(p);this.silver=silver;}
    @Override protected boolean isRandomlyTicking(BlockState state){return silver&&!state.getValue(PERSISTENT)||super.isRandomlyTicking(state);}
    @Override protected void randomTick(BlockState s,ServerLevel l,BlockPos p,RandomSource r){
        boolean decays=decaying(s);super.randomTick(s,l,p,r);
        if(silver&&!decays&&!s.getValue(PERSISTENT)&&WorldConfig.AURA.get()){
            var a=AuraSystem.get(l,p);if(a!=null&&a.vis()<a.base())AuraSystem.add(l,p,.01f,false);
        }
    }
    public int getFlammability(BlockState s,BlockGetter l,BlockPos p,Direction f){return 60;}
    public int getFireSpreadSpeed(BlockState s,BlockGetter l,BlockPos p,Direction f){return 30;}
}
