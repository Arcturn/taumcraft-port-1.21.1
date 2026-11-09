package thaumcraft.port.world;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

/** Source taint spread rules, with live seed queries and no forced chunk loads. */
public final class TaintSystem {
    public static final TagKey<Block> CRUST=tag("taint_convert_crust"),SOIL=tag("taint_convert_soil"),ROCK=tag("taint_convert_rock");
    private static TagKey<Block> tag(String name){return TagKey.create(Registries.BLOCK,WorldContent.id(name));}
    public static int radius(){return WorldConfig.TAINT_SPREAD_AREA.get();}
    public static double rate(){return WorldConfig.TAINT_SPREAD_RATE.get()/100.0;}
    public static boolean nearby(ServerLevel level,BlockPos pos){
        int radius=radius();return !level.getEntitiesOfClass(TaintSeed.class,new AABB(pos).inflate(radius),seed->seed.isAlive()&&seed.blockPosition().distSqr(pos)<=radius*radius).isEmpty();
    }
    public static boolean atSeedEdge(ServerLevel level,BlockPos pos){
        int radius=radius();double outer=(double)radius*radius,inner=outer*.64;
        return !level.getEntitiesOfClass(TaintSeed.class,new AABB(pos).inflate(radius),seed->{double distance=seed.blockPosition().distSqr(pos);return seed.isAlive()&&distance>inner&&distance<outer;}).isEmpty();
    }
    public static boolean tainted(BlockState state){return state.getBlock() instanceof TaintBlock||state.getBlock() instanceof TaintFeatureBlock;}
    public static boolean neighboursLoaded(LevelReader level,BlockPos pos){for(var side:Direction.values())if(!level.hasChunkAt(pos.relative(side)))return false;return level.hasChunkAt(pos);}
    /** True if no adjacent non-tainted block provides a solid supporting face. */
    public static boolean onlyAdjacentToTaint(LevelReader level,BlockPos pos){
        if(!neighboursLoaded(level,pos))return false;
        for(var side:Direction.values()){var q=pos.relative(side);var state=level.getBlockState(q);if(!state.isAir()&&!tainted(state)&&state.isFaceSturdy(level,q,side.getOpposite()))return false;}
        return true;
    }
    public static boolean hemmedByTaint(LevelReader level,BlockPos pos){
        if(!neighboursLoaded(level,pos))return false;int count=0;
        for(var side:Direction.values()){var q=pos.relative(side);var state=level.getBlockState(q);if(tainted(state))++count;else if(state.isAir()||(state.getFluidState().isEmpty()&&!state.isFaceSturdy(level,q,side.getOpposite())))--count;}
        return count>0;
    }
    public static void spread(ServerLevel level,BlockPos pos,boolean boosted){
        if(!WorldConfig.TAINT.get()||!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)||!level.hasChunkAt(pos)||!nearby(level,pos))return;
        var aura=AuraSystem.get(level,pos);float saturation=aura==null||aura.base()<=0?0:aura.flux()/aura.base();
        if(!boosted&&(rate()<=0||level.random.nextFloat()>rate()*(.001+saturation*2)))return;
        var target=pos.offset(level.random.nextInt(3)-1,level.random.nextInt(3)-1,level.random.nextInt(3)-1);
        spreadTo(level,pos,target,level.random);
    }
    /** One already-selected adjacent spread attempt, also used by deterministic encounter tests. */
    public static boolean spreadTo(ServerLevel level,BlockPos origin,BlockPos target,RandomSource random){
        if(!WorldConfig.TAINT.get()||!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)||origin.equals(target)||!neighboursLoaded(level,target)||!level.hasChunkAt(origin)||!nearby(level,origin))return false;
        if(Math.abs(target.getX()-origin.getX())>1||Math.abs(target.getY()-origin.getY())>1||Math.abs(target.getZ()-origin.getZ())>1)return false;
        var state=level.getBlockState(target);float hardness=state.getDestroySpeed(level,target);
        if(hardness<0||hardness>10||state.hasBlockEntity()||!state.getFluidState().isEmpty())return false;
        if(state.is(BlockTags.LEAVES)){
            if(random.nextFloat()<.6f)for(var direction:Direction.values())if(level.getBlockState(target.relative(direction)).is(WorldContent.block("taint_log"))){
                return level.setBlockAndUpdate(target,WorldContent.block("taint_feature").defaultBlockState().setValue(TaintFeatureBlock.FACING,direction.getOpposite()));
            }
            return replace(level,target,"taint_fibre",true);
        }
        if((state.isAir()||state.canBeReplaced()||state.getBlock() instanceof BushBlock||state.getBlock() instanceof net.neoforged.neoforge.common.IPlantable)&&!onlyAdjacentToTaint(level,target))return replace(level,target,"taint_fibre",true);
        if(hemmedByTaint(level,target)&&hardness<5&&!tainted(state)){
            if(state.is(BlockTags.LOGS)){
                var converted=WorldContent.block("taint_log").defaultBlockState();if(state.hasProperty(BlockStateProperties.AXIS))converted=converted.setValue(BlockStateProperties.AXIS,state.getValue(BlockStateProperties.AXIS));
                // Logs and leaf-attached features deliberately do not consume flux in BETA26.
                return level.setBlockAndUpdate(target,converted);
            }
            if(state.is(CRUST))return replace(level,target,"taint_crust",true);
            if(state.is(SOIL))return replace(level,target,"taint_soil",true);
            if(state.is(ROCK))return replace(level,target,"taint_rock",true);
        }
        var aura=AuraSystem.get(level,target);
        if((state.is(WorldContent.block("taint_soil"))||state.is(WorldContent.block("taint_rock")))&&level.isEmptyBlock(target.above())&&aura!=null&&aura.flux()>=5&&random.nextFloat()<rate()*.33&&atSeedEdge(level,target)){
            var seed=WorldEntities.SEED.get().create(level);if(seed==null)return false;
            seed.moveTo(target.getX()+.5,target.getY()+1,target.getZ()+.5,random.nextInt(360),0);
            if(seed.checkSpawnRules(level,MobSpawnType.MOB_SUMMONED)&&seed.checkSpawnObstruction(level)&&level.addFreshEntity(seed)){AuraSystem.drain(level,target,5,true,false);return true;}
        }
        return false;
    }
    private static boolean replace(ServerLevel level,BlockPos pos,String id,boolean drain){
        var block=WorldContent.block(id);var state=block instanceof TaintFibreBlock fibre?fibre.attached(level,pos):block.defaultBlockState();
        if(!level.setBlockAndUpdate(pos,state))return false;
        level.scheduleTick(pos,block,1);if(drain)AuraSystem.drain(level,pos,.01f,true,false);return true;
    }
    private TaintSystem(){}
}
