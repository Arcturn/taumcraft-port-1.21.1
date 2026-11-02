package thaumcraft.port.artifice.construct;

import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.crafting.content.CraftingContent;
import thaumcraft.port.infusion.recipe.InfusionAugments;
import thaumcraft.port.infusion.armor.InfusionEnchantments;
import thaumcraft.port.world.AuraSystem;

/** Aura-fed rotating spiral scanner; the installed pick defines radius, depth and mining modifiers. */
@EventBusSubscriber(modid="thaumcraft")
public final class ArcaneBoreEntity extends OwnedConstruct {
    private static final EntityDataAccessor<Direction> FACING=SynchedEntityData.defineId(ArcaneBoreEntity.class,EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Boolean> ACTIVE=SynchedEntityData.defineId(ArcaneBoreEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> CHARGE=SynchedEntityData.defineId(ArcaneBoreEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Optional<BlockPos>> TARGET=SynchedEntityData.defineId(ArcaneBoreEntity.class,EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Integer> DELAY=SynchedEntityData.defineId(ArcaneBoreEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SEQUENCE=SynchedEntityData.defineId(ArcaneBoreEntity.class,EntityDataSerializers.INT);
    private static final ThreadLocal<Harvest> HARVEST=new ThreadLocal<>();
    private record Harvest(net.minecraft.server.level.ServerPlayer player,BlockPos pos,List<ItemStack> drops){}
    private BlockPos previousScan;
    private int digDelay,breakCounter,soundTicks;
    public int spiral;
    public float currentRadius;
    public ArcaneBoreEntity(EntityType<? extends ArcaneBoreEntity> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,50).add(Attributes.FOLLOW_RANGE,32);}
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(FACING,Direction.DOWN);builder.define(ACTIVE,false);builder.define(CHARGE,0f);builder.define(TARGET,Optional.empty());builder.define(DELAY,0);builder.define(SEQUENCE,0);}
    public Direction getFacing(){return entityData.get(FACING);}
    public void setFacing(Direction direction){entityData.set(FACING,direction);previousScan=null;setDigTarget(null,0);}
    public boolean isActive(){return entityData.get(ACTIVE);}
    public void setActive(boolean active){entityData.set(ACTIVE,active);}
    public float getCharge(){return entityData.get(CHARGE);}
    public void setCharge(float charge){entityData.set(CHARGE,Math.max(0,charge));}
    public BlockPos digTarget(){return entityData.get(TARGET).orElse(null);}
    @Override protected net.minecraft.network.chat.Component getTypeName(){return net.minecraft.network.chat.Component.translatable("entity.ArcaneBore.name");}
    public int workSequence(){return entityData.get(SEQUENCE);}
    public int digDuration(){return entityData.get(DELAY);}
    public void setDigTarget(BlockPos pos,int delay){entityData.set(TARGET,Optional.ofNullable(pos==null?null:pos.immutable()));entityData.set(DELAY,delay);digDelay=delay;if(pos!=null)entityData.set(SEQUENCE,workSequence()+1);}
    @Override public String placerId(){return "turret_placer_bore";}
    @Override public boolean accepts(ItemStack stack){return stack.getItem() instanceof PickaxeItem||stack.is(ItemTags.PICKAXES)||stack.canPerformAction(net.neoforged.neoforge.common.ItemAbilities.PICKAXE_DIG);}
    public boolean validInventory(){var tool=getMainHandItem();return !tool.isEmpty()&&accepts(tool)&&(!tool.isDamageableItem()||tool.getDamageValue()+1<tool.getMaxDamage());}
    private int augment(String name){return InfusionAugments.getEnchantment(getMainHandItem(),name);}
    private int enchantment(ResourceKey<Enchantment> id){return EnchantmentHelper.getItemEnchantmentLevel(registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(id),getMainHandItem());}
    public int getDigRadius(){return Math.max(2,accepts(getMainHandItem())?getMainHandItem().getEnchantmentValue()/3+augment("destructive")*2:0);}
    public int getDigDepth(){return getDigRadius()*8+augment("burrowing")*16;}
    public int getFortune(){return validInventory()?Math.max(enchantment(Enchantments.FORTUNE),augment("sounding")):0;}
    public int getRefining(){return augment("refining");}
    public boolean hasSilkTouch(){return enchantment(Enchantments.SILK_TOUCH)>0;}
    public int getDigSpeed(BlockState state){return validInventory()?(int)(getMainHandItem().getDestroySpeed(state)/2)+enchantment(Enchantments.EFFICIENCY):0;}
    public void rechargeVis(){if(level() instanceof ServerLevel server&&getCharge()<10)setCharge(getCharge()+AuraSystem.drain(server,blockPosition(),10,false,false));}
    @Override public void tick(){
        super.tick();if(level().isClientSide)return;
        setYRot(yHeadRot);if(tickCount%50==0)heal(1);if(tickCount%10==0)rechargeVis();if(soundTicks>0)soundTicks--;
        var railPos=blockPosition();if(BaseRailBlock.isRail(level().getBlockState(railPos.below())))railPos=railPos.below();var state=level().getBlockState(railPos);
        if(state.is(CraftingContent.ACTIVATOR_RAIL.get()))setActive(!state.getValue(PoweredRailBlock.POWERED));
        else if(!isPassenger())setActive(level().hasNeighborSignal(blockPosition().below()));
        if(validInventory())getMainHandItem().inventoryTick(level(),this,0,true);
        if(!isActive()||!validInventory()){setDigTarget(null,0);getLookControl().setLookAt(getX()+getFacing().getStepX(),getY()+getFacing().getStepY(),getZ()+getFacing().getStepZ(),10,33);return;}
        var target=digTarget();if(target!=null&&getCharge()>=.25f){
            getLookControl().setLookAt(target.getX()+.5,target.getY(),target.getZ()+.5,10,90);
            if(digDelay--<=0){boolean success=mineBlock(target);setDigTarget(null,0);if(success){
                // mineBlock debits only a successful protected harvest; never attempt the source's second removal.
                if(soundTicks==0){soundTicks=24+random.nextInt(2);playSound(PortSounds.get("rumble"),.25f,.9f+random.nextFloat()*.2f);}
            }}
        }
        if(digTarget()==null)findNextBlockToDig();
    }
    /** Two ray traces from the next spiral column and from the actual bore mouth, as in BETA26. */
    public void findNextBlockToDig(){
        int radius=getDigRadius();if(previousScan==null||previousScan.distToCenterSqr(position())>(radius+1)*(radius+1))previousScan=blockPosition();
        BlockPos start=previousScan,end=start.relative(getFacing(),getDigDepth());
        var first=level().clip(new ClipContext(Vec3.atCenterOf(start),Vec3.atCenterOf(end),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        if(first.getType()!=HitResult.Type.MISS){
            Vec3 mouth=new Vec3(getX()+getFacing().getStepX(),getEyeY()+getFacing().getStepY(),getZ()+getFacing().getStepZ());
            var hit=level().clip(new ClipContext(mouth,Vec3.atCenterOf(first.getBlockPos()),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));var pos=hit.getBlockPos();
            if(hit.getType()!=HitResult.Type.MISS&&level().hasChunkAt(pos)&&!pos.equals(blockPosition())&&!pos.equals(blockPosition().below())){
                var block=level().getBlockState(pos);float hardness=block.getDestroySpeed(level(),pos);
                if(hardness>=0&&!block.getCollisionShape(level(),pos).isEmpty()){
                    int speed=getDigSpeed(block);setDigTarget(pos,Math.max(1,Math.max(10-speed,(int)(hardness*2)-speed*2)));return;
                }
            }
        }
        BlockPos next=start;
        // Source increments until a distinct column; cap only protects malformed external pickaxes with extreme enchantability.
        for(int attempt=0;next.equals(start)&&attempt<4096;attempt++){
            if(Math.abs(currentRadius)>radius)currentRadius=radius;
            spiral+=(int)(3+Math.max(0,(10-Math.abs(currentRadius))*2));
            if(spiral>=360){spiral-=360;currentRadius++;if(currentRadius>radius||currentRadius<-radius)currentRadius=0;}
            Vec3 offset=new Vec3(0,currentRadius,0).zRot(spiral/180f*(float)Math.PI).yRot((float)Math.PI/2*getFacing().getStepX()).xRot((float)Math.PI/2*getFacing().getStepY());
            // The original casts X/Z toward zero before adding .5, including negative coordinates.
            next=BlockPos.containing((int)getX()+.5+getFacing().getStepX()+offset.x,getEyeY()+getFacing().getStepY()+offset.y,(int)getZ()+.5+getFacing().getStepZ()+offset.z);
        }
        previousScan=next;
    }
    /** Harvest with vanilla/NeoForge permissions and drop hooks, without spending one tool durability per block. */
    public boolean mineBlock(BlockPos pos){
        if(!(level() instanceof ServerLevel server)||getCharge()<.25f||!validInventory()||!server.hasChunkAt(pos))return false;
        var state=server.getBlockState(pos);if(state.isAir()||state.getDestroySpeed(server,pos)<0)return false;
        var profile=new GameProfile(getOwnerUUID()==null?UUID.nameUUIDFromBytes("FakeThaumcraftBore".getBytes(java.nio.charset.StandardCharsets.UTF_8)):getOwnerUUID(),"FakeThaumcraftBore");
        var fake=FakePlayerFactory.get(server,profile);fake.setPos(position());fake.setYRot(getYRot());fake.setXRot(getXRot());fake.setGameMode(GameType.SURVIVAL);
        ItemStack miningTool=getMainHandItem().copy();var data=miningTool.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();data.remove("infench");miningTool.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        if(!hasSilkTouch()&&getFortune()>0)miningTool.enchant(registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE),getFortune());
        fake.setItemInHand(InteractionHand.MAIN_HAND,miningTool);fake.setShiftKeyDown(true);
        List<ItemStack> drops=new ArrayList<>();boolean broken;
        Set<UUID> existingItems=new HashSet<>();for(var item:server.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1.5)))existingItems.add(item.getUUID());
        HARVEST.set(new Harvest(fake,pos,drops));try{broken=server.mayInteract(fake,pos)&&fake.gameMode.destroyBlock(pos);}finally{HARVEST.remove();fake.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);}
        if(!broken)return false;
        // Keep the source fallback for modded block inventory drops, but never steal pre-existing player items.
        for(var entity:server.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(1.5),item->!existingItems.contains(item.getUUID()))){drops.add(entity.getItem().copy());entity.discard();}
        for(var drop:drops){
            if(!hasSilkTouch()&&getRefining()>0&&random.nextFloat()<(getRefining()+1)*.125f){var refined=InfusionEnchantments.refined(drop);if(!refined.isEmpty())drop=refined;}
            eject(drop);
        }
        // Deliberate BETA26 bug fix: retain the documented quarter-vis cost as a float.
        // Its byte cast discarded fractional reserves, while the second removal often avoided charging entirely.
        setCharge(Math.max(0,getCharge()-.25f));
        if(++breakCounter>=50){breakCounter-=50;getMainHandItem().hurtAndBreak(1,this,EquipmentSlot.MAINHAND);}
        return true;
    }
    @SubscribeEvent public static void captureHarvest(BlockDropsEvent event){var harvest=HARVEST.get();if(harvest!=null&&event.getBreaker()==harvest.player&&event.getPos().equals(harvest.pos)){for(var drop:event.getDrops())harvest.drops.add(drop.getItem().copy());event.getDrops().clear();}}
    private void eject(ItemStack stack){
        if(stack.isEmpty())return;Direction output=getFacing().getOpposite();
        for(var direction:Direction.values())if(level().getCapability(Capabilities.ItemHandler.BLOCK,blockPosition().relative(direction),direction.getOpposite())!=null){output=direction;break;}
        var inventory=level().getCapability(Capabilities.ItemHandler.BLOCK,blockPosition().relative(output),output.getOpposite());
        if(inventory!=null)stack=ItemHandlerHelper.insertItemStacked(inventory,stack,false);if(stack.isEmpty())return;
        BlockPos origin=blockPosition();if(level().getBlockState(origin.relative(output)).isSolidRender(level(),origin.relative(output)))origin=origin.relative(output.getOpposite());
        Vec3 position=new Vec3(origin.getX()+.5+output.getStepX(),origin.getY()+output.getStepY(),origin.getZ()+.5+output.getStepZ());
        var drop=new ItemEntity(level(),position.x,position.y,position.z,stack);drop.setDeltaMovement(output.getStepX()*.3,output.getStepY()*.3,output.getStepZ()*.3);drop.setDefaultPickUpDelay();level().addFreshEntity(drop);
    }
    @Override public boolean hurt(DamageSource source,float amount){
        if(source.getEntity() instanceof LivingEntity owner&&isOwner(owner)){
            Direction direction=owner.getDirection().getOpposite();double dx=Math.abs(owner.getX()-getX()),dz=Math.abs(owner.getZ()-getZ());
            if(dx<2&&dz<2){double eye=owner.getEyeY();if(eye-getY()>2)direction=Direction.UP;else if(getY()-eye>0)direction=Direction.DOWN;}
            if(direction!=Direction.DOWN)setFacing(direction);return false;
        }
        setYRot(getYRot()+(float)random.nextGaussian()*45);setXRot(getXRot()+(float)random.nextGaussian()*20);return super.hurt(source,amount);
    }
    @Override public void move(MoverType type,Vec3 movement){super.move(type,movement.multiply(.2,1,.2));}
    @Override public int getMaxHeadXRot(){return 10;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putFloat("charge",getCharge());tag.putByte("faceing",(byte)getFacing().get3DDataValue());tag.putBoolean("active",isActive());tag.putInt("BreakCounter",breakCounter);tag.putInt("Spiral",spiral);tag.putFloat("Radius",currentRadius);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setCharge(tag.getFloat("charge"));setFacing(Direction.from3DDataValue(tag.getByte("faceing")));setActive(tag.getBoolean("active"));breakCounter=Math.clamp(tag.getInt("BreakCounter"),0,49);spiral=tag.getInt("Spiral");currentRadius=tag.getFloat("Radius");}
    @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean hit){
        super.dropCustomDeathLoot(level,source,hit);float bonus=looting(source)*.15f;
        String[] parts={"mind_clockwork","morphic_resonator","crystal_aer","crystal_terra","mechanism_simple","plate_brass","plank_greatwood"};
        for(int i=0;i<parts.length;i++)if(random.nextFloat()<(i<4?.2f:.5f)+bonus)spawnAtLocation(item(parts[i]),.5f);
    }
}
