package thaumcraft.port.auromancy.effect;

import java.util.UUID;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Original 3x3 progressing tunnel, with the removed state and restoration timer persisted. */
public final class HoleBlockEntity extends BlockEntity {
    private BlockState oldState=Blocks.AIR.defaultBlockState();
    private int countdown,maximum=120,count;
    private Direction direction;
    private UUID owner;
    public HoleBlockEntity(BlockPos pos,BlockState state){super(FocusEffectContent.HOLE_ENTITY.get(),pos,state);}
    public void initialize(BlockState state,int duration,int count,Direction direction,UUID owner){oldState=state;maximum=duration;this.count=count;this.direction=direction;this.owner=owner;setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public BlockState storedState(){return oldState;}
    public int elapsedTicks(){return countdown;}
    public int remainingTicks(){return Math.max(0,maximum-countdown);}
    public void tick() {
        if(level==null)return;
        if(level.isClientSide){FocusEffectVisuals.holeAmbient.accept(this);return;}
        ServerLevel world=(ServerLevel)level;
        if(countdown==0&&count>1&&direction!=null) {
            var entity=owner==null?null:world.getEntity(owner);LivingEntity caster=entity instanceof LivingEntity living?living:null;
            if(owner==null||caster!=null) {
                for(int a=-1;a<=1;a++)for(int b=-1;b<=1;b++) {
                    if(a==0&&b==0)continue;
                    BlockPos next=switch(direction.getAxis()){case X->worldPosition.offset(0,a,b);case Y->worldPosition.offset(a,0,b);case Z->worldPosition.offset(a,b,0);};
                    FocusWorldEffects.createHole(world,next,null,1,maximum,caster);
                }
                if(!FocusWorldEffects.createHole(world,worldPosition.relative(direction.getOpposite()),direction,count-1,maximum,caster))count=0;
            }
        }
        countdown++;if(countdown%20==0)setChanged();
        if(countdown>=maximum)world.setBlock(worldPosition,oldState,3);
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.put("old_state",NbtUtils.writeBlockState(oldState));tag.putInt("countdown",countdown);tag.putInt("maximum",maximum);tag.putInt("count",count);tag.putInt("direction",direction==null?-1:direction.ordinal());if(owner!=null)tag.putUUID("owner",owner);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);oldState=NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK),tag.getCompound("old_state"));countdown=Math.max(0,tag.getInt("countdown"));maximum=Math.max(1,tag.getInt("maximum"));count=Math.max(0,Math.min(128,tag.getInt("count")));int side=tag.getInt("direction");direction=side>=0&&side<6?Direction.values()[side]:null;owner=tag.hasUUID("owner")?tag.getUUID("owner"):null;}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
