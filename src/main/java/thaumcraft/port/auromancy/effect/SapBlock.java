package thaumcraft.port.auromancy.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

public final class SapBlock extends Block {
    public static final TagKey<EntityType<?>> ELDRITCH=TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.parse("thaumcraft:eldritch_mobs"));
    public SapBlock(Properties properties){super(properties);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Shapes.empty();}
    @Override protected void entityInside(BlockState state,Level level,BlockPos pos,Entity entity) {
        if(!level.isClientSide&&entity instanceof LivingEntity living&&!entity.getType().is(ELDRITCH)&&!living.hasEffect(MobEffects.WITHER)) {
            living.addEffect(new MobEffectInstance(MobEffects.WITHER,40,0,true,true));
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,40,1,true,true));
            living.addEffect(new MobEffectInstance(MobEffects.HUNGER,40,1,true,true));
        }
    }
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){level.removeBlock(pos,false);}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){FocusEffectVisuals.sapAmbient.accept(level,pos);}
}
