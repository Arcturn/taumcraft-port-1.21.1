package thaumcraft.port.world;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class PortalFeature extends Feature<NoneFeatureConfiguration>{
    public PortalFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){
        var l=c.level();var r=c.random();
        if(!WorldConfig.PORTALS.get()||l.getLevel().dimension()!=Level.OVERWORLD||c.chunkGenerator() instanceof net.minecraft.world.level.levelgen.FlatLevelSource||r.nextInt(500)!=0)return false;
        int x=(c.origin().getX()&~15)+8,z=(c.origin().getZ()&~15)+8;var p=new BlockPos(x,l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z),z);
        var ground=l.getBlockState(p.below());if(!(ground.is(BlockTags.DIRT)||ground.is(BlockTags.BASE_STONE_OVERWORLD)||ground.is(BlockTags.SAND)||ground.is(Blocks.SNOW_BLOCK)))return false;
        for(var q:BlockPos.betweenClosed(p.offset(-1,0,-1),p.offset(1,2,1)))if(!l.ensureCanWrite(q)||!l.isEmptyBlock(q))return false;
        var portal=WorldEntities.PORTAL.get().create(l.getLevel());if(portal==null)return false;portal.moveTo(x+.5,p.getY(),z+.5,r.nextFloat()*360,0);return l.addFreshEntity(portal);
    }
}
