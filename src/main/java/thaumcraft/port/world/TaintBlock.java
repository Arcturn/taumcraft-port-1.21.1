package thaumcraft.port.world;

import java.util.List;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.CrystalItem;

/** Common solid taint rules; fibre and logs have their own source-specific states. */
public class TaintBlock extends Block {
    protected final String kind;
    public TaintBlock(String kind,Properties properties){super(properties.randomTicks());this.kind=kind;}
    public BlockState attached(BlockGetter level,BlockPos pos){return defaultBlockState();}
    public void die(Level level,BlockPos pos,BlockState state){
        level.setBlockAndUpdate(pos,switch(kind){case "taint_crust","taint_geyser","taint_log"->WorldContent.block("flux_goo").defaultBlockState();case "taint_soil"->Blocks.DIRT.defaultBlockState();case "taint_rock"->WorldContent.block("stone_porous").defaultBlockState();default->Blocks.AIR.defaultBlockState();});
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){randomTick(state,level,pos,random);}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(!WorldConfig.TAINT.get()||!TaintSystem.neighboursLoaded(level,pos))return;
        if(!TaintSystem.nearby(level,pos)&&random.nextInt(10)==0){die(level,pos,state);return;}
        if(kind.equals("taint_rock"))TaintSystem.spread(level,pos,false);
        else if(kind.equals("taint_crust")&&TaintSystem.onlyAdjacentToTaint(level,pos)){
            if(FallingTaint.startFall(level,pos,pos))return;
            if(level.isEmptyBlock(pos.above())){
                var side=Direction.from2DDataValue(random.nextInt(4));boolean unsupportedTower=true;
                for(int depth=1;depth<4;depth++){
                    var below=pos.below(depth);var adjacent=below.relative(side);
                    if(!level.hasChunkAt(below)||!level.hasChunkAt(adjacent)||!level.isEmptyBlock(adjacent)||!level.getBlockState(below).is(this)){unsupportedTower=false;break;}
                }
                if(unsupportedTower)FallingTaint.startFall(level,pos,pos.relative(side));
            }
        }
    }
    @Override public void stepOn(Level level,BlockPos pos,BlockState state,Entity entity){
        if(!kind.equals("taint_log")&&!level.isClientSide&&entity instanceof LivingEntity living&&!living.getType().is(EntityTypeTags.UNDEAD)&&level.random.nextInt(kind.equals("taint_fibre")?750:250)==0)living.addEffect(new MobEffectInstance(WorldEntities.TAINT,200));
        super.stepOn(level,pos,state,entity);
    }
    @Override protected boolean triggerEvent(BlockState state,Level level,BlockPos pos,int event,int parameter){
        if(event==1){if(level.isClientSide)level.playLocalSound(pos,SoundEvents.CHORUS_FLOWER_DEATH,SoundSource.BLOCKS,.1f,.9f+level.random.nextFloat()*.2f,false);return true;}return super.triggerEvent(state,level,pos,event,parameter);
    }
    protected static int enchantmentLevel(ServerLevel level,ItemStack tool,net.minecraft.resources.ResourceKey<Enchantment> enchantment){return EnchantmentHelper.getItemEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment),tool);}
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder params){
        if(kind.equals("taint_rock")){
            var tool=params.getOptionalParameter(LootContextParams.TOOL);if(tool==null)tool=ItemStack.EMPTY;
            if(enchantmentLevel(params.getLevel(),tool,Enchantments.SILK_TOUCH)==0)return params.getLevel().random.nextInt(15)+enchantmentLevel(params.getLevel(),tool,Enchantments.FORTUNE)>13?List.of(CrystalItem.stack(Aspect.FLUX)):List.of();
        }
        return super.getDrops(state,params);
    }
}
