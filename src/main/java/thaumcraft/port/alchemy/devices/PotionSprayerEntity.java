package thaumcraft.port.alchemy.devices;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.port.aspect.*;
import thaumcraft.port.essentia.*;

public final class PotionSprayerEntity extends EssentiaEntity implements Container,MenuProvider {
    public static final int DATA_COUNT=3+2*Aspect.values().length;
    private final NonNullList<ItemStack> items=NonNullList.withSize(1,ItemStack.EMPTY);
    public final SprayerState store=new SprayerState();
    public int color=0x333333,venting;
    private int counter;
    private boolean activated;
    private Aspect currentSuction;
    public final ContainerData data=new ContainerData(){
        public int get(int index){
            if(index==0)return store.charges();if(index==1)return color&65535;if(index==2)return color>>>16;
            int ordinal=(index-3)/2;if(ordinal<0||ordinal>=Aspect.values().length)return 0;
            Aspect aspect=Aspect.values()[ordinal];return (index-3)%2==0?store.recipe().getOrDefault(aspect,0):store.progress().getOrDefault(aspect,0);
        }
        public void set(int index,int value){}public int getCount(){return DATA_COUNT;}
    };
    public PotionSprayerEntity(BlockPos pos,BlockState state){super(AlchemyDevices.SPRAYER_ENTITY.get(),pos,state);}
    public Direction facing(){return getBlockState().getValue(PotionSprayerBlock.FACING);}
    public static boolean validPotion(ItemStack stack){
        if(!(stack.is(Items.POTION)||stack.is(Items.SPLASH_POTION)||stack.is(Items.LINGERING_POTION)))return false;
        var contents=stack.get(DataComponents.POTION_CONTENTS);if(contents==null||contents.potion().isEmpty())return false;
        var potion=contents.potion().get();return !potion.is(Potions.WATER)&&!potion.is(Potions.MUNDANE)&&!potion.is(Potions.THICK)&&!potion.is(Potions.AWKWARD);
    }
    /** ConfigAspects.getPotionAspects excludes the five aspects added only to the potion item itself. */
    public static Map<Aspect,Integer> potionRecipe(ItemStack stack,Level level){
        if(!validPotion(stack))return Map.of();
        var costs=new LinkedHashMap<>(PotionAspects.base(stack,level.potionBrewing(),reagent->RecipeAspects.get(reagent,level)));
        Aspect carrier=stack.is(Items.POTION)?Aspect.WATER:stack.is(Items.SPLASH_POTION)?Aspect.AVERSION:Aspect.ENERGY;
        int remaining=costs.getOrDefault(carrier,0)-5;if(remaining>0)costs.put(carrier,remaining);else costs.remove(carrier);
        return SprayerState.cull(costs);
    }
    @Override public void tick(){
        if(level==null)return;
        if(level.isClientSide){DeviceVisuals.sprayerTick.accept(this);return;}
        if(++counter%5==0){
            currentSuction=null;
            if(items.getFirst().isEmpty()||store.charges()>=8)return;
            currentSuction=store.needed();
            if(store.complete())sync();else if(currentSuction!=null)pullIngredient();
        }
        boolean powered=level.hasNeighborSignal(worldPosition);
        if(powered){
            if(!activated&&validPotion(items.getFirst())&&store.spray()){
                applyPotion();
                level.playSound(null,worldPosition,SoundEvents.LAVA_EXTINGUISH,SoundSource.BLOCKS,.25f,2.6f+(level.random.nextFloat()-level.random.nextFloat())*.8f);
                level.blockEvent(worldPosition,getBlockState().getBlock(),0,0);sync();
            }
            activated=true;
        }else activated=false;
    }
    private void applyPotion(){
        var contents=items.getFirst().get(DataComponents.POTION_CONTENTS);if(contents==null)return;
        var area=new AABB(worldPosition.relative(facing(),2)).inflate(1);
        for(var entity:level.getEntitiesOfClass(LivingEntity.class,area,e->e.isAlive()&&e.isAffectedByPotions()))for(var effect:contents.getAllEffects()){
            var type=effect.getEffect();
            if(type.value().isInstantenous())type.value().applyInstantenousEffect(null,null,entity,effect.getAmplifier(),1);
            else entity.addEffect(new MobEffectInstance(type,effect.getDuration(),effect.getAmplifier()));
        }
    }
    private void pullIngredient(){
        for(int y=0;y<=1;y++)for(Direction side:Direction.values())if(side!=facing()){
            var source=EssentiaEntity.at(level,worldPosition.above(y).relative(side),side.getOpposite());
            if(source!=null&&EssentiaTransport.pull(this,side,source)>0)return;
        }
    }
    private void recalculate(){
        if(!(level instanceof ServerLevel server))return;
        var stack=items.getFirst();
        store.select(potionRecipe(stack,server));
        var contents=stack.get(DataComponents.POTION_CONTENTS);
        color=contents!=null&&contents.potion().isPresent()?PotionContents.getColor(contents.potion().get())&0xffffff:0x333333;
        currentSuction=null;sync();
    }
    public boolean connects(Direction side){return side!=facing();}
    public boolean input(Direction side){return connects(side);}
    public boolean output(Direction side){return false;}
    public Aspect aspect(Direction side){return null;}
    public int amount(Direction side){return 0;}
    public Aspect suctionType(Direction side){return currentSuction;}
    public int suction(Direction side){return currentSuction==null?0:128;}
    public int minimumSuction(){return 0;}
    public int insert(Aspect aspect,int amount,Direction side,boolean simulate){int accepted=connects(side)?store.insert(aspect,amount,simulate):0;if(accepted>0&&!simulate)sync();return accepted;}
    public int extract(Aspect aspect,int amount,Direction side,boolean simulate){return 0;}
    public int getContainerSize(){return 1;}
    public boolean isEmpty(){return items.getFirst().isEmpty();}
    public ItemStack getItem(int slot){return slot==0?items.getFirst():ItemStack.EMPTY;}
    public ItemStack removeItem(int slot,int amount){if(slot!=0)return ItemStack.EMPTY;var result=ContainerHelper.removeItem(items,slot,amount);if(!result.isEmpty())recalculate();return result;}
    public ItemStack removeItemNoUpdate(int slot){if(slot!=0)return ItemStack.EMPTY;var result=ContainerHelper.takeItem(items,slot);if(!result.isEmpty())recalculate();return result;}
    public void setItem(int slot,ItemStack stack){if(slot!=0)return;items.set(0,stack);stack.limitSize(1);recalculate();setChanged();}
    public boolean stillValid(Player player){return Container.stillValidBlockEntity(this,player);}
    public boolean canPlaceItem(int slot,ItemStack stack){return slot==0&&validPotion(stack);}
    public int getMaxStackSize(){return 1;}
    public void clearContent(){items.set(0,ItemStack.EMPTY);recalculate();}
    public Component getDisplayName(){return getBlockState().getBlock().getName();}
    public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new PotionSprayerMenu(id,inventory,this,data);}
    @Override public boolean triggerEvent(int id,int parameter){if(id>=0){venting=15;return true;}return super.triggerEvent(id,parameter);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registry){
        super.saveAdditional(tag,registry);ContainerHelper.saveAllItems(tag,items,registry);tag.putInt("Charges",store.charges());tag.putInt("Color",color);tag.putBoolean("Activated",activated);
        var recipe=new CompoundTag();store.recipe().forEach((aspect,amount)->recipe.putInt(aspect.id(),amount));tag.put("Recipe",recipe);
        var progress=new CompoundTag();store.progress().forEach((aspect,amount)->progress.putInt(aspect.id(),amount));tag.put("Progress",progress);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registry){
        super.loadAdditional(tag,registry);items.clear();ContainerHelper.loadAllItems(tag,items,registry);color=tag.getInt("Color")&0xffffff;activated=tag.getBoolean("Activated");
        var recipe=new LinkedHashMap<Aspect,Integer>();var progress=new EnumMap<Aspect,Integer>(Aspect.class);
        for(Aspect aspect:Aspect.values()){int amount=tag.getCompound("Recipe").getInt(aspect.id());if(amount>0)recipe.put(aspect,amount);progress.put(aspect,tag.getCompound("Progress").getInt(aspect.id()));}
        store.restore(recipe,progress,tag.getInt("Charges"));currentSuction=null;
    }
}
