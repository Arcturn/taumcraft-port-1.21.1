package thaumcraft.port.machine;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import thaumcraft.port.aspect.*;
import thaumcraft.port.content.*;
import thaumcraft.port.network.WorldFxPayload;
import thaumcraft.port.research.*;
import thaumcraft.port.world.AuraSystem;

public final class CrucibleEntity extends BlockEntity {
    public final CrucibleState state=new CrucibleState();
    private int lastWater=-1,lastHeat=-1,lastTotal=-1;
    public CrucibleEntity(BlockPos pos,BlockState block){super(MachineContent.CRUCIBLE_ENTITY.get(),pos,block);}
    public static void tick(Level world,BlockPos pos,BlockState block,CrucibleEntity be){
        if(!(world instanceof ServerLevel level))return;
        var below=level.getBlockState(pos.below());
        boolean heated=below.is(BlockTags.FIRE)||below.getFluidState().is(FluidTags.LAVA)||below.is(Blocks.MAGMA_BLOCK)||
            (below.getBlock() instanceof CampfireBlock&&below.getValue(CampfireBlock.LIT))||below.getBlock() instanceof NitorBlock;
        boolean changed=be.state.tick(heated);
        if(be.state.spillDue()&&be.state.total()>0){AuraSystem.add(level,pos,be.state.spill(level.random.nextInt()),true);changed=true;}
        if(be.state.boiling()&&level.getGameTime()%5==0) {
            for(var item:level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).deflate(.1).move(0,.2,0),e->e.isAlive()&&!e.getTags().contains("thaumcraft_crucible_output"))) {
                ServerPlayer player=item.getOwner() instanceof ServerPlayer p?p:null;
                ItemStack stack=item.getItem();int limit=Math.min(64,stack.getCount());boolean consumed=false;
                for(int i=0;i<limit&&be.state.boiling();i++) {
                    var recipe=level.getRecipeManager().getRecipesFor(MachineContent.CRUCIBLE_RECIPE.get(),new SingleRecipeInput(stack),level).stream()
                        .map(net.minecraft.world.item.crafting.RecipeHolder::value)
                        .filter(r->player!=null&&ResearchStorage.get(player.getServer()).player(player.getUUID()).knows(r.research())&&be.state.contains(r.aspects())).findFirst();
                    if(recipe.isPresent()&&be.state.craft(recipe.get().aspects())) {
                        var output=recipe.get().result().copy();var spawned=new ItemEntity(level,pos.getX()+.5,pos.getY()+1.1,pos.getZ()+.5,output);
                        spawned.setDeltaMovement((level.random.nextFloat()-.5)*.05,.15,(level.random.nextFloat()-.5)*.05);
                        spawned.addTag("thaumcraft_crucible_output");spawned.setDefaultPickUpDelay();level.addFreshEntity(spawned);
                        ResearchProgress.craft(player,output);WorldFxPayload.send(level,0,Vec3.atCenterOf(pos),Vec3.atCenterOf(pos),0xffffff);
                    }else if(!be.state.dissolve(RecipeAspects.get(stack,level)))break;
                    stack.shrink(1);consumed=true;
                }
                if(stack.isEmpty())item.discard();else if(consumed)item.setItem(stack);
                if(consumed){changed=true;level.playSound(null,pos,PortSounds.get("bubble"),SoundSource.BLOCKS,.2f,1+level.random.nextFloat()*.4f);}
            }
            if(level.getGameTime()%10==0)level.sendParticles(PortParticles.get("rune"),pos.getX()+.5,pos.getY()+.8,pos.getZ()+.5,2,.2,0,.2,.01);
        }
        if(changed)be.setChanged();
        if(be.lastWater!=be.state.water()||be.lastTotal!=be.state.total()||be.lastHeat/10!=be.state.heat()/10){
            be.lastWater=be.state.water();be.lastTotal=be.state.total();be.lastHeat=be.state.heat();level.sendBlockUpdated(pos,block,block,Block.UPDATE_CLIENTS);
        }
    }
    public void sync(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),Block.UPDATE_CLIENTS);}
    public void empty(){if(level instanceof ServerLevel server){AuraSystem.add(server,worldPosition,state.empty(),true);server.playSound(null,worldPosition,PortSounds.get("spill"),SoundSource.BLOCKS,.3f,1);sync();}}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);tag.putInt("water",state.water());tag.putInt("heat",state.heat());tag.putInt("timer",state.timer());
        var aspects=new CompoundTag();state.aspects().forEach((a,n)->aspects.putInt(a.id(),n));tag.put("aspects",aspects);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);var values=new EnumMap<Aspect,Integer>(Aspect.class);var aspects=tag.getCompound("aspects");
        aspects.getAllKeys().forEach(k->Aspect.byId(k).ifPresent(a->values.put(a,aspects.getInt(k))));
        state.restore(tag.getInt("water"),tag.getInt("heat"),tag.contains("timer")?tag.getInt("timer"):-100,values);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
