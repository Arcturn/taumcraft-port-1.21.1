package thaumcraft.port.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

public final class MagicPlantBlock extends BushBlock {
    @Override public com.mojang.serialization.MapCodec<MagicPlantBlock> codec(){return simpleCodec(p->new MagicPlantBlock(kind,p));}
    private final String kind;
    public MagicPlantBlock(String kind,Properties p){super(p);this.kind=kind;}
    @Override protected boolean mayPlaceOn(BlockState state,BlockGetter level,BlockPos pos){
        return super.mayPlaceOn(state,level,pos)||kind.equals("cinderpearl")&&(state.is(BlockTags.SAND)||state.is(BlockTags.TERRACOTTA)||state.is(Blocks.CLAY));
    }
    @Override protected void entityInside(BlockState s,Level l,BlockPos p,Entity e){
        if(!l.isClientSide&&kind.equals("vishroom")&&e instanceof LivingEntity living&&l.random.nextInt(5)==0)
            living.addEffect(new MobEffectInstance(MobEffects.CONFUSION,200));
    }
    @Override public void animateTick(BlockState s,Level l,BlockPos p,RandomSource r){
        if(r.nextInt(5)==0)l.addParticle(kind.equals("cinderpearl")?ParticleTypes.FLAME:kind.equals("vishroom")?ParticleTypes.WITCH:ParticleTypes.END_ROD,
            p.getX()+.5+r.nextGaussian()*.1,p.getY()+.7,p.getZ()+.5+r.nextGaussian()*.1,0,.01,0);
    }
}
