package thaumcraft.port.world;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;
public final class TaintSystem {
    public static boolean nearby(ServerLevel l,BlockPos p){return !l.getEntitiesOfClass(TaintSeed.class,new AABB(p).inflate(32),e->e.isAlive()&&e.blockPosition().distSqr(p)<=1024).isEmpty();}
    public static boolean tainted(net.minecraft.world.level.block.state.BlockState s){return s.getBlock() instanceof TaintBlock;}
    public static void spread(ServerLevel l,BlockPos p,boolean boosted){
        if(!WorldConfig.TAINT.get()||!l.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)||!l.hasChunkAt(p)||!nearby(l,p))return;
        var a=AuraSystem.get(l,p);float saturation=a==null||a.base()==0?0:a.flux()/a.base();
        if(!boosted&&l.random.nextFloat()>.001+saturation*2)return;
        var q=p.offset(l.random.nextInt(3)-1,l.random.nextInt(3)-1,l.random.nextInt(3)-1);
        if(q.equals(p)||!l.hasChunkAt(q)||!l.getFluidState(q).isEmpty())return;
        var s=l.getBlockState(q);if(s.getDestroySpeed(l,q)<0||s.getDestroySpeed(l,q)>10||s.hasBlockEntity())return;
        int touching=0,solid=0,hemmed=0;for(var d:Direction.values()){var n=q.relative(d);if(!l.hasChunkAt(n))continue;var ns=l.getBlockState(n);if(tainted(ns)){touching++;hemmed++;}else if(ns.isAir())hemmed--;else if(ns.isFaceSturdy(l,n,d.getOpposite()))solid++;else if(ns.getFluidState().isEmpty())hemmed--;}
        String replacement=null;
        if((s.canBeReplaced()||s.is(BlockTags.LEAVES))&&solid>0)replacement="taint_fibre";
        else if(hemmed>0&&s.getDestroySpeed(l,q)<5&&!tainted(s)){
            if(s.is(BlockTags.LOGS))replacement="taint_log";
            else if(s.is(BlockTags.DIRT)||s.is(BlockTags.SAND)||s.is(Blocks.GRAVEL)||s.is(Blocks.CLAY))replacement="taint_soil";
            else if(s.is(BlockTags.BASE_STONE_OVERWORLD)||s.is(BlockTags.STONE_BRICKS))replacement="taint_rock";
            else if(s.is(BlockTags.PLANKS)||s.is(BlockTags.WOOL))replacement="taint_crust";
        }
        if(replacement!=null){l.setBlockAndUpdate(q,replacement.equals("taint_fibre")?((TaintBlock)WorldContent.block(replacement)).attached(l,q):WorldContent.block(replacement).defaultBlockState());AuraSystem.drain(l,q,.01f,true,false);}
        else if((s.is(WorldContent.block("taint_soil"))||s.is(WorldContent.block("taint_rock")))&&l.isEmptyBlock(q.above())&&a!=null&&a.flux()>=5&&l.random.nextFloat()<.33){
            boolean edge=!l.getEntitiesOfClass(TaintSeed.class,new AABB(q).inflate(25.6),e->e.isAlive()&&e.blockPosition().distSqr(q)<=655.36).isEmpty();
            if(!edge){var seed=WorldEntities.SEED.get().create(l);if(seed!=null){seed.moveTo(q.getX()+.5,q.getY()+1,q.getZ()+.5,0,0);if(l.noCollision(seed)&&l.addFreshEntity(seed))AuraSystem.drain(l,q,5,true,false);}}
        }
    }
}
