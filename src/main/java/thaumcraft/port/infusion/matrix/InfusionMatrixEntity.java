package thaumcraft.port.infusion.matrix;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import thaumcraft.port.alchemy.material.TallowCandleBlock;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.world.AuraSystem;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.essentia.EssentiaStreamPayload;
import thaumcraft.port.infusion.recipe.InfusionRecipes;
import thaumcraft.port.infusion.structure.*;
import thaumcraft.port.research.ResearchProgress;
import thaumcraft.port.research.ResearchStorage;
import thaumcraft.port.world.WorldContent;
import thaumcraft.port.world.WorldEntities;

/** The BETA26 ritual state machine. Input, output and costs are frozen when the caster starts it. */
public final class InfusionMatrixEntity extends BlockEntity {
    public final List<BlockPos> pedestals=new ArrayList<>(),problemBlocks=new ArrayList<>();
    public boolean active,crafting,checkSurroundings=true;
    public float stability,startUp;
    public int craftCount;
    private float replenish,costMultiplier=1;
    private int cycleTime=10,countDelay=5,count,itemCount,instability,experience;
    private final LinkedHashMap<Aspect,Integer> essentia=new LinkedHashMap<>();
    private final List<ItemStack> ingredients=new ArrayList<>();
    private ItemStack input=ItemStack.EMPTY,output=ItemStack.EMPTY;
    private UUID owner;
    private final MatrixSources sources=new MatrixSources();

    public InfusionMatrixEntity(BlockPos pos,BlockState state){super(MatrixContent.ENTITY.get(),pos,state);}
    public Map<Aspect,Integer> remainingEssentia(){var remaining=new LinkedHashMap<Aspect,Integer>();essentia.forEach((a,n)->{if(n>0)remaining.put(a,n);});return Collections.unmodifiableMap(remaining);}
    public List<ItemStack> remainingIngredients(){return ingredients.stream().map(ItemStack::copy).toList();}
    public int cycleTime(){return cycleTime;}
    public int instability(){return instability;}
    public float costMultiplier(){return costMultiplier;}
    public float stabilityReplenish(){return replenish;}
    // The source computes stabilityCap / 2 with integer arithmetic: 25 / 2 == 12.
    public String stabilityName(){return stability>12?"VERY_STABLE":stability>=0?"STABLE":stability>-25?"UNSTABLE":"VERY_UNSTABLE";}
    public float lossPerCycle(){return instability/(stability>12?5f:stability>=0?6f:stability>-25?7f:8f);}
    public boolean validLocation(){
        if(level==null||!(level.getBlockState(worldPosition.below(2)).getBlock() instanceof PedestalBlock))return false;
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})if(!(level.getBlockState(worldPosition.offset(x,-2,z)).getBlock() instanceof PillarBlock))return false;
        return true;
    }
    public boolean activateOrStart(ServerPlayer player){
        if(level==null||level.isClientSide)return false;
        if(active)return !crafting&&startCrafting(player);
        if(!validLocation())return false;
        active=true;scanSurroundings();sound("craftstart",.5f,1);sync();return true;
    }
    public boolean startCrafting(ServerPlayer player){
        if(!(level instanceof ServerLevel server)||crafting)return false;
        if(!validLocation()){active=false;sync();return false;}
        scanSurroundings();
        if(!(level.getBlockEntity(worldPosition.below(2)) instanceof PedestalEntity central)||central.getItem(0).isEmpty())return false;
        var centralItem=central.getItem(0).copy();
        List<ItemStack> components=new ArrayList<>();
        for(BlockPos pos:pedestals)if(level.getBlockEntity(pos) instanceof PedestalEntity pedestal&&!pedestal.getItem(0).isEmpty())components.add(pedestal.getItem(0).copy());
        if(components.isEmpty())return false;
        for(var holder:server.getRecipeManager().getAllRecipesFor(InfusionRecipes.TYPE.get())){
            var recipe=holder.value();
            if(!recipe.matches(player,centralItem,components))continue;
            var result=recipe.result(player,centralItem,components);
            if(result.isEmpty())return false;
            input=centralItem;output=result.copy();ingredients.clear();ingredients.addAll(components);
            instability=recipe.instability(player,input,components);experience=recipe.experience(player,input,components);
            essentia.clear();costMultiplier=Math.max(.5f,costMultiplier);
            recipe.aspects(player,input,components).forEach((aspect,amount)->{int n=(int)(amount*costMultiplier);if(n>0)essentia.put(aspect,n);});
            owner=player.getUUID();crafting=true;itemCount=0;
            sound("craftstart",.5f,1);sync();return true;
        }
        return false;
    }
    public void tick(){
        if(level==null)return;
        count++;
        if(checkSurroundings){checkSurroundings=false;scanSurroundings();}
        if(level.isClientSide){thaumcraft.port.client.InfusionMatrixEffects.tick(this);return;}
        if(count%(crafting?20:100)==0&&!validLocation()){active=false;sync();return;}
        if(active&&!crafting&&stability<25&&count%Math.max(5,countDelay)==0){stability=Math.min(25,stability+Math.max(.1f,replenish));sync();}
        if(active&&crafting&&count%Math.max(1,countDelay)==0)craftCycle();
    }
    public void craftCycle(){
        if(!(level instanceof ServerLevel server)||!crafting)return;
        stability=Mth.clamp(stability-server.random.nextFloat()*lossPerCycle()+replenish,-100,25);
        boolean valid=level.getBlockEntity(worldPosition.below(2)) instanceof PedestalEntity central&&!central.getItem(0).isEmpty()&&ItemStack.isSameItemSameComponents(central.getItem(0),input);
        if(!valid||stability<0&&server.random.nextInt(1500)<=Math.abs(stability)){
            instabilityEvent(server.random.nextInt(24));
            stability+=5+server.random.nextFloat()*5;discoverInstability();sync();
            if(valid)return;
        }
        if(!valid){crafting=false;essentia.clear();instability=0;ingredients.clear();output=ItemStack.EMPTY;itemCount=0;sound("craftfail",1,.6f);sync();return;}
        if(experience>0){
            List<Player> targets=server.getEntitiesOfClass(Player.class,nearby());
            for(Player player:targets)if(player.isCreative()||player.experienceLevel>0){
                if(!player.isCreative())player.giveExperienceLevels(-1);
                experience--;player.hurt(server.damageSources().magic(),server.random.nextInt(2));
                MatrixEffectPayload.send(server,worldPosition,Vec3.atCenterOf(worldPosition),player.getId(),MatrixEffectPayload.SOURCE);
                server.playSound(null,player.blockPosition(),SoundEvents.EXPERIENCE_ORB_PICKUP,SoundSource.PLAYERS,1,2+server.random.nextFloat()*.4f);
                countDelay=cycleTime;sync();return;
            }
            if(!targets.isEmpty()&&server.random.nextInt(3)==0)addRandomEssentia();
            sync();return;
        }
        countDelay=Math.max(1,cycleTime/2);
        if(essentia.values().stream().anyMatch(amount->amount>0)){
            for(var entry:essentia.entrySet())if(entry.getValue()>0){
                var source=sources.providing(server,worldPosition,entry.getKey());
                if(source!=null&&source.source().takeRemote(entry.getKey(),1,false)==1){
                    entry.setValue(entry.getValue()-1);EssentiaStreamPayload.send(server,source.pos(),worldPosition,entry.getKey());sync();return;
                }
                stability-=.25f;
            }
            checkSurroundings=true;sync();return;
        }
        if(!ingredients.isEmpty()){
            for(int index=0;index<ingredients.size();index++){
                for(BlockPos pos:pedestals)if(level.getBlockEntity(pos) instanceof PedestalEntity pedestal&&!pedestal.getItem(0).isEmpty()&&ItemStack.isSameItemSameComponents(pedestal.getItem(0),ingredients.get(index))){
                    if(itemCount==0){itemCount=5;MatrixEffectPayload.send(server,worldPosition,Vec3.atLowerCornerOf(pos),0,MatrixEffectPayload.SOURCE);}
                    else if(itemCount--<=1){pedestal.setItem(0,pedestal.getItem(0).getCraftingRemainingItem().copy());pedestal.setChanged();level.sendBlockUpdated(pos,pedestal.getBlockState(),pedestal.getBlockState(),2);ingredients.remove(index);}
                    sync();return;
                }
                if(server.random.nextInt(index+1)==0)addRandomEssentia();
            }
            sync();return;
        }
        finish();
    }
    private void addRandomEssentia(){
        if(essentia.isEmpty())return;
        var keys=new ArrayList<>(essentia.keySet());Aspect aspect=keys.get(level.random.nextInt(keys.size()));essentia.compute(aspect,(a,n)->n==Integer.MAX_VALUE?n:n+1);stability-=.25f;
    }
    private void finish(){
        crafting=false;
        if(level.getBlockEntity(worldPosition.below(2)) instanceof PedestalEntity pedestal){
            var result=output.copy();var previous=pedestal.getItem(0);
            if(previous.isDamageableItem()&&previous.isDamaged()&&result.isDamageableItem()&&!result.isDamaged())result.setDamageValue((int)(result.getMaxDamage()*(previous.getDamageValue()/(float)previous.getMaxDamage())));
            pedestal.setInventorySlotContentsFromInfusion(0,result);
            ServerPlayer player=owner==null?null:((ServerLevel)level).getServer().getPlayerList().getPlayer(owner);
            if(player!=null)EventHooks.firePlayerCraftingEvent(player,result,new SimpleContainer(0));
            level.blockEvent(pedestal.getBlockPos(),pedestal.getBlockState().getBlock(),12,0);sound("wand",.5f,1);
        }
        essentia.clear();ingredients.clear();instability=0;experience=0;output=ItemStack.EMPTY;input=ItemStack.EMPTY;owner=null;sync();
    }
    /** Public for parity checks and stabilizer integration, with the original 24-way distribution. */
    public void instabilityEvent(int event){
        if(!(level instanceof ServerLevel server))return;
        switch(event){
            case 0,1,2,3->ejectItem(0);
            case 4,5,6->warp();
            case 7,8,9->zap(false);
            case 10,11->zap(true);
            case 12,13->ejectItem(1);
            case 14,15->ejectItem(2);
            case 16->ejectItem(3);
            case 17->ejectItem(4);
            case 18,19->harm(false);
            case 20,21->ejectItem(5);
            case 22->harm(true);
            case 23->server.explode(null,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,1.5f+server.random.nextFloat(),Level.ExplosionInteraction.BLOCK);
            default->throw new IllegalArgumentException("Unknown instability event "+event);
        }
    }
    private void zap(boolean all){
        for(LivingEntity entity:level.getEntitiesOfClass(LivingEntity.class,nearby())){
            entity.hurt(level.damageSources().magic(),4+level.random.nextInt(4));arc(entity.getBoundingBox().getCenter(),entity.getId());if(!all)break;
        }
    }
    private void harm(boolean all){
        for(LivingEntity entity:level.getEntitiesOfClass(LivingEntity.class,nearby())){
            boolean taint=level.random.nextBoolean();var effect=new MobEffectInstance(taint?WorldEntities.TAINT:WorldEntities.VIS_EXHAUST,taint?120:2400,0,!taint,true);
            if(!taint)effect.getCures().clear();entity.addEffect(effect);if(!all)break;
        }
    }
    private void warp(){
        var players=level.getEntitiesOfClass(ServerPlayer.class,nearby());if(players.isEmpty())return;
        var player=players.get(level.random.nextInt(players.size()));var storage=ResearchStorage.get(((ServerLevel)level).getServer());var state=storage.player(player.getUUID());
        if(level.random.nextFloat()<.25f)state.warp(state.permanentWarp(),state.normalWarp()+1,state.temporaryWarp());
        else state.warp(state.permanentWarp(),state.normalWarp(),state.temporaryWarp()+2+level.random.nextInt(4));
        storage.setDirty();ResearchProgress.sync(player);
    }
    public void discoverInstability(){
        if(!(level instanceof ServerLevel server))return;
        var storage=ResearchStorage.get(server.getServer());
        for(ServerPlayer player:server.getEntitiesOfClass(ServerPlayer.class,nearby()))if(storage.player(player.getUUID()).flag("!INSTABILITY")){
            storage.setDirty();ResearchProgress.sync(player);player.displayClientMessage(Component.translatable("got.instability").withStyle(ChatFormatting.DARK_PURPLE),true);
        }
    }
    public void ejectItem(int type){
        if(!(level instanceof ServerLevel server)||pedestals.isEmpty())return;
        for(int attempt=0;attempt<25;attempt++){
            BlockPos pos=pedestals.get(server.random.nextInt(pedestals.size()));
            if(!(server.getBlockEntity(pos) instanceof PedestalEntity pedestal)||pedestal.getItem(0).isEmpty())continue;
            BlockPos protection=pedestal.findInstabilityMitigator();
            if(protection!=null&&server.getBlockEntity(protection) instanceof StabilizerEntity stabilizer&&stabilizer.mitigate(5+server.random.nextInt(6))){
                server.blockEvent(pos,pedestal.getBlockState().getBlock(),5,0);arc(Vec3.atCenterOf(pos.above()),0);
                MatrixEffectPayload.send(server,pos.above(),Vec3.atCenterOf(protection),0,MatrixEffectPayload.ARC);return;
            }
            if(type<=3||type==5)Containers.dropContents(server,pos,pedestal);
            pedestal.setItem(0,ItemStack.EMPTY);pedestal.setChanged();server.sendBlockUpdated(pos,pedestal.getBlockState(),pedestal.getBlockState(),2);
            if(type==1||type==3){server.setBlockAndUpdate(pos.above(),WorldContent.block("flux_goo").defaultBlockState());server.playSound(null,pos,SoundEvents.LAVA_EXTINGUISH,SoundSource.BLOCKS,.3f,1);}
            else if(type==2||type==4)AuraSystem.pollute(server,pos,5+server.random.nextInt(5),true);
            else if(type==5)server.explode(null,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,1,Level.ExplosionInteraction.BLOCK);
            server.blockEvent(pos,pedestal.getBlockState().getBlock(),11,0);arc(Vec3.atCenterOf(pos.above()),0);return;
        }
    }
    private void arc(Vec3 to,int entity){MatrixEffectPayload.send((ServerLevel)level,worldPosition,to,entity,MatrixEffectPayload.ARC);}
    private AABB nearby(){return new AABB(worldPosition).inflate(10);}
    private void sound(String key,float volume,float pitch){level.playSound(null,worldPosition,PortSounds.get(key),SoundSource.BLOCKS,volume,pitch);}

    /** Same 17×11×17 scan, opposite pairs, diminishing returns, pillar bonuses and four boosters. */
    public void scanSurroundings(){
        if(level==null)return;
        pedestals.clear();problemBlocks.clear();cycleTime=10;replenish=0;costMultiplier=1;
        Set<BlockPos> stabilizers=new LinkedHashSet<>();Map<Object,Integer> pairCounts=new HashMap<>();
        for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)if(x!=0||z!=0)for(int y=-3;y<=7;y++){
            BlockPos pos=worldPosition.offset(x,-y,z);if(!level.hasChunkAt(pos))continue;
            Block block=level.getBlockState(pos).getBlock();
            if(block instanceof PedestalBlock)pedestals.add(pos);
            if(block instanceof AbstractSkullBlock||block instanceof TallowCandleBlock||block instanceof InfusionStabilizer stabilizer&&stabilizer.canStabilise(level,pos))stabilizers.add(pos);
        }
        while(!stabilizers.isEmpty()){
            BlockPos first=stabilizers.iterator().next(),second=new BlockPos(worldPosition.getX()*2-first.getX(),first.getY(),worldPosition.getZ()*2-first.getZ());
            Block a=level.getBlockState(first).getBlock(),b=level.getBlockState(second).getBlock();
            float amountA=a instanceof InfusionStabilizer s?s.stabilization(level,first):.1f;
            float amountB=b instanceof InfusionStabilizer s?s.stabilization(level,second):.1f;
            // 1.12 skull variants were one block distinguished by tile data, so retain that equivalence.
            boolean same=a==b||a instanceof AbstractSkullBlock&&b instanceof AbstractSkullBlock;
            if(same&&amountA==amountB){
                if(a instanceof InfusionStabilizer s&&s.symmetryPenalty(level,first,second)){replenish-=s.penalty(level,first);problemBlocks.add(first);}
                else {Object key=a instanceof AbstractSkullBlock?AbstractSkullBlock.class:a;int seen=pairCounts.getOrDefault(key,0);replenish+=(float)(amountA*Math.pow(.75,seen));pairCounts.put(key,seen+1);}
            }else{replenish-=Math.max(amountA,amountB);problemBlocks.add(first);}
            stabilizers.remove(first);stabilizers.remove(second);
        }
        boolean ancient=true,eldritch=true;
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1}){
            Block pillar=level.getBlockState(worldPosition.offset(x,-2,z)).getBlock();ancient&=pillar==StructureContent.PILLAR_ANCIENT.get();eldritch&=pillar==StructureContent.PILLAR_ELDRITCH.get();
        }
        if(ancient){cycleTime--;costMultiplier-=.1f;replenish-=.1f;}
        if(eldritch){cycleTime-=3;costMultiplier+=.05f;replenish+=.2f;}
        int[] xs={-1,1,1,-1},zs={-1,-1,1,1};
        for(int index=0;index<4;index++){
            Block boost=level.getBlockState(worldPosition.offset(xs[index],-3,zs[index])).getBlock();
            if(boost==StructureContent.MATRIX_SPEED.get()){cycleTime--;costMultiplier+=.01f;}
            if(boost==StructureContent.MATRIX_COST.get()){cycleTime++;costMultiplier-=.02f;}
        }
        countDelay=Math.max(1,cycleTime/2);
        for(BlockPos pos:pedestals){Block block=level.getBlockState(pos).getBlock();if(block==StructureContent.PEDESTAL_ELDRITCH.get())costMultiplier+=.0025f;if(block==StructureContent.PEDESTAL_ANCIENT.get())costMultiplier-=.01f;}
    }
    public void sync(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){var tag=new CompoundTag();writeState(tag);return tag;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    private void writeState(CompoundTag tag){
        tag.putBoolean("Active",active);tag.putBoolean("Crafting",crafting);tag.putFloat("Stability",stability);tag.putInt("Instability",instability);tag.putFloat("Replenish",replenish);
        var aspects=new CompoundTag();essentia.forEach((aspect,n)->aspects.putInt(aspect.id(),n));tag.put("Essentia",aspects);
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);writeState(tag);
        if(!input.isEmpty())tag.put("Input",input.save(registries));if(!output.isEmpty())tag.put("Output",output.save(registries));
        var items=new ListTag();for(ItemStack item:ingredients)items.add(item.save(registries));tag.put("Ingredients",items);
        if(owner!=null)tag.putUUID("Owner",owner);tag.putInt("Experience",experience);tag.putInt("ItemCount",itemCount);tag.putInt("Count",count);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);active=tag.getBoolean("Active");crafting=tag.getBoolean("Crafting");stability=Mth.clamp(tag.getFloat("Stability"),-100,35);instability=tag.getInt("Instability");replenish=tag.getFloat("Replenish");
        essentia.clear();var aspects=tag.getCompound("Essentia");for(String key:aspects.getAllKeys())Aspect.byId(key).ifPresent(aspect->essentia.put(aspect,Math.max(0,aspects.getInt(key))));
        if(level==null||!level.isClientSide){
            input=ItemStack.parseOptional(registries,tag.getCompound("Input"));output=ItemStack.parseOptional(registries,tag.getCompound("Output"));ingredients.clear();
            for(var entry:tag.getList("Ingredients",10)){var item=ItemStack.parseOptional(registries,(CompoundTag)entry);if(!item.isEmpty())ingredients.add(item);}
            owner=tag.hasUUID("Owner")?tag.getUUID("Owner"):null;experience=Math.max(0,tag.getInt("Experience"));itemCount=Mth.clamp(tag.getInt("ItemCount"),0,5);count=Math.max(0,tag.getInt("Count"));
            if(tag.contains("Ingredients")&&crafting&&(input.isEmpty()||output.isEmpty())){crafting=false;essentia.clear();instability=0;}
        }
        checkSurroundings=true;
    }
}
