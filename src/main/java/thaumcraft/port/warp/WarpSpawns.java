package thaumcraft.port.warp;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.AABB;
import thaumcraft.port.warp.entity.WarpEntities;
import thaumcraft.port.world.WorldEntities;

/** Original 50-attempt warp encounters, constrained to already loaded chunks. */
public final class WarpSpawns {
    public static void spawnGuardian(ServerPlayer player){
        var level=player.serverLevel();var guardian=WarpEntities.ELDRITCH_GUARDIAN.get().create(level);if(guardian==null)return;
        for(int attempt=0;attempt<50;attempt++){
            var pos=candidate(player);guardian.setPos(pos.getX(),pos.getY(),pos.getZ());
            if(!clear(level,guardian,pos.below(),false))continue;
            guardian.setTarget(player);level.addFreshEntity(guardian);break;
        }
    }
    public static void suddenlySpiders(ServerPlayer player,int warp,boolean real){
        var level=player.serverLevel();
        for(int count=0;count<Math.min(50,Math.max(0,warp));count++){
            var spider=WarpEntities.MIND_SPIDER.get().create(level);if(spider==null)continue;
            for(int attempt=0;attempt<50;attempt++){
                var pos=candidate(player);spider.setPos(pos.getX(),pos.getY(),pos.getZ());
                if(!clear(level,spider,pos.below(),false))continue;
                spider.setTarget(player);if(!real){spider.setViewer(player.getGameProfile().getName());spider.setHarmless(true);}
                level.addFreshEntity(spider);break;
            }
        }
        message(player,"warp.text.7");
    }
    public static void spawnPortal(ServerPlayer player){
        var level=player.serverLevel();var portal=WorldEntities.PORTAL.get().create(level);if(portal==null)return;
        for(int attempt=0;attempt<50;attempt++){
            var pos=candidate(player);portal.setPos(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5);
            if(!clear(level,portal,pos.below(),true))continue;
            portal.finalizeSpawn(level,level.getCurrentDifficultyAt(portal.blockPosition()),MobSpawnType.EVENT,null);
            if(level.addFreshEntity(portal))message(player,"warp.text.16");break;
        }
    }
    private static BlockPos candidate(ServerPlayer player){
        var random=player.level().random;var origin=player.blockPosition();
        return origin.offset((7+random.nextInt(18))*(random.nextInt(3)-1),(7+random.nextInt(18))*(random.nextInt(3)-1),(7+random.nextInt(18))*(random.nextInt(3)-1));
    }
    private static boolean clear(ServerLevel level,Mob entity,BlockPos ground,boolean normalCube){
        var box=entity.getBoundingBox();
        if(ground.getY()<level.getMinBuildHeight()||box.maxY>level.getMaxBuildHeight()||!level.getWorldBorder().isWithinBounds(box))return false;
        // Collision and liquid checks read neighbouring block cells as well as the support block.
        if(!loaded(level,box.inflate(1))||!level.hasChunkAt(ground))return false;
        var state=level.getBlockState(ground);if(normalCube?!state.isRedstoneConductor(level,ground):!state.isSolidRender(level,ground))return false;
        return level.isUnobstructed(entity)&&level.noCollision(entity,box)&&!level.containsAnyLiquid(box);
    }
    private static boolean loaded(ServerLevel level,AABB bounds){
        int minX=BlockPos.containing(bounds.minX,bounds.minY,bounds.minZ).getX()>>4,maxX=BlockPos.containing(bounds.maxX,bounds.maxY,bounds.maxZ).getX()>>4;
        int minZ=BlockPos.containing(bounds.minX,bounds.minY,bounds.minZ).getZ()>>4,maxZ=BlockPos.containing(bounds.maxX,bounds.maxY,bounds.maxZ).getZ()>>4;
        for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)if(!level.getChunkSource().hasChunk(x,z))return false;
        return true;
    }
    private static void message(ServerPlayer player,String key){player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE,ChatFormatting.ITALIC),true);}
    private WarpSpawns(){}
}
