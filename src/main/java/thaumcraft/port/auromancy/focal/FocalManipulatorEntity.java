package thaumcraft.port.auromancy.focal;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.auromancy.AuromancyContent;
import thaumcraft.port.auromancy.device.WorkbenchAura;
import thaumcraft.port.auromancy.focus.FocusPackage;
import thaumcraft.port.auromancy.item.FocusItem;
import thaumcraft.port.content.*;
import thaumcraft.port.research.ResearchStorage;

/** Server-owned focus construction. A paid program is immutable until completion or removal. */
public final class FocalManipulatorEntity extends BlockEntity implements Container,MenuProvider {
    private final NonNullList<ItemStack> items=NonNullList.withSize(1,ItemStack.EMPTY);
    public Map<Integer,FocusElementNode> data=new HashMap<>();
    public String focusName="";
    public float vis,visCost;
    public int xpCost;
    public boolean doGuiReset,doGather;
    public Map<Aspect,Integer> crystalsSync=new LinkedHashMap<>();
    public long revision;
    private FocusPackage paidProgram;
    private ItemStack paidFocus=ItemStack.EMPTY;
    private String paidName="";
    private int ticks;

    public FocalManipulatorEntity(BlockPos pos,BlockState state){super(AuromancyContent.FOCAL_ENTITY.get(),pos,state);}
    public int getMaxComplexity(){return getItem(0).getItem() instanceof FocusItem focus?focus.getMaxComplexity():0;}
    public int getTotalComplexity(){return FocalGraph.complexity(data);}
    public int getContainerSize(){return 1;}
    public boolean isEmpty(){return items.getFirst().isEmpty();}
    public ItemStack getItem(int slot){return slot==0?items.getFirst():ItemStack.EMPTY;}
    public ItemStack removeItem(int slot,int count){if(slot!=0)return ItemStack.EMPTY;var result=ContainerHelper.removeItem(items,slot,count);if(!result.isEmpty())slotChanged();return result;}
    public ItemStack removeItemNoUpdate(int slot){if(slot!=0)return ItemStack.EMPTY;var result=ContainerHelper.takeItem(items,slot);if(!result.isEmpty())slotChanged();return result;}
    public void setItem(int slot,ItemStack stack){if(slot!=0)return;var before=items.getFirst();items.set(0,stack);stack.limitSize(1);if(!ItemStack.isSameItemSameComponents(before,stack)||before.getCount()!=stack.getCount())slotChanged();else setChanged();}
    public void clearContent(){items.set(0,ItemStack.EMPTY);slotChanged();}
    public boolean canPlaceItem(int slot,ItemStack stack){return slot==0&&stack.getItem() instanceof FocusItem;}
    public boolean stillValid(Player player){return Container.stillValidBlockEntity(this,player);}
    public int getMaxStackSize(){return 1;}
    private void slotChanged(){cancel();data.clear();focusName="";doGuiReset=true;revision++;setChanged();}
    private void cancel(){vis=0;visCost=0;xpCost=0;crystalsSync.clear();paidProgram=null;paidFocus=ItemStack.EMPTY;paidName="";}
    public Component getDisplayName(){return Component.translatable("block.thaumcraft.wand_workbench");}
    public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new FocalManipulatorMenu(id,inventory,this);}
    @Override public void setChanged(){super.setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}

    public boolean edit(Map<Integer,FocusElementNode> next,String name){
        if(vis>0||isEmpty()||name.length()>50)return false;
        try{FocalGraph.validateEdit(next);}catch(IllegalArgumentException ex){return false;}
        // The legacy packet decoder inserts into a default HashMap. The map-copy
        // constructor can choose a smaller bucket array and reorder differently
        // configured duplicate nodes, changing their weighted complexity.
        data=new HashMap<>();for(var entry:next.entrySet())data.put(entry.getKey(),entry.getValue());
        focusName=name;revision++;setChanged();return true;
    }
    public boolean startCraft(ServerPlayer player){
        if(vis>0||!(level instanceof ServerLevel)||!stillValid(player)||!player.isAlive()||getMaxComplexity()==0)return false;
        final FocusPackage program;
        try{program=FocalGraph.compile(data);}catch(IllegalArgumentException ex){return false;}
        var research=ResearchStorage.get(player.getServer()).player(player.getUUID());
        if(!program.validate(getMaxComplexity(),research::knows).valid())return false;
        int complexity=program.getComplexity();
        var costs=FocusPackage.crystals(data.values().stream().map(n->n.node).toList());
        if(costs.isEmpty())return false;
        int experience=FocusPackage.experienceCost(complexity);
        if(!player.isCreative()&&player.experienceLevel<experience)return false;
        int[] reservation=reserveCrystals(player.getInventory(),costs);if(reservation==null)return false;
        // All preconditions are checked before either XP or inventory is changed.
        for(int slot=0;slot<reservation.length;slot++)if(reservation[slot]>0)player.getInventory().removeItem(slot,reservation[slot]);
        if(!player.isCreative())player.giveExperienceLevels(-experience);
        player.getInventory().setChanged();paidProgram=program;paidFocus=getItem(0).copy();paidName=focusName;
        vis=visCost=FocusPackage.constructionVis(complexity,getMaxComplexity());xpCost=experience;crystalsSync=new LinkedHashMap<>(costs);
        revision++;setChanged();sound("craftstart",1,1);return true;
    }
    public static int[] reserveCrystals(Inventory inventory,Map<Aspect,Integer> costs){
        int[] take=new int[inventory.getContainerSize()];
        for(var cost:costs.entrySet()){
            int need=cost.getValue();
            // Source InventoryUtils searches mainInventory, including the hotbar.
            // Armor/offhand slots must not silently pay crafting requirements.
            for(int slot=0;slot<inventory.items.size()&&need>0;slot++){
                var stack=inventory.getItem(slot);
                if(stack.getItem() instanceof CrystalItem&&CrystalItem.aspect(stack)==cost.getKey()){
                    int amount=Math.min(need,stack.getCount()-take[slot]);take[slot]+=amount;need-=amount;
                }
            }
            if(need>0)return null;
        }
        return take;
    }
    public static void tick(FocalManipulatorEntity table){
        if(table.level==null)return;
        if(table.level.isClientSide){FocalVisuals.tick.accept(table);return;}
        if(++table.ticks%20!=0||table.vis<=0)return;
        if(table.paidProgram==null||!ItemStack.isSameItemSameComponents(table.getItem(0),table.paidFocus)){
            table.cancel();table.revision++;table.setChanged();table.sound("wandfail",.33f,1);return;
        }
        float drained=WorkbenchAura.drainFocal((ServerLevel)table.level,table.worldPosition,Math.min(20,table.vis));
        if(drained<=0)return;
        table.level.blockEvent(table.worldPosition,table.getBlockState().getBlock(),5,1);
        table.vis=Math.max(0,table.vis-drained);
        if(table.vis<=0)table.finishCraft();else{table.revision++;table.setChanged();}
    }
    private void finishCraft(){
        FocusItem.setPackage(getItem(0),paidProgram);
        if(!paidName.isEmpty())getItem(0).set(DataComponents.CUSTOM_NAME,Component.literal(paidName));else getItem(0).remove(DataComponents.CUSTOM_NAME);
        cancel();data.clear();doGuiReset=true;revision++;setChanged();sound("wand",1,1);
    }
    private void sound(String id,float volume,float pitch){if(level!=null)level.playSound(null,worldPosition,PortSounds.get(id),SoundSource.BLOCKS,volume,pitch);}
    @Override public boolean triggerEvent(int id,int parameter){if(id==5){if(level!=null&&level.isClientSide)FocalVisuals.gather.accept(this,parameter);return true;}if(id==1){doGuiReset=true;return true;}return super.triggerEvent(id,parameter);}

    public static ListTag writeGraph(Map<Integer,FocusElementNode> graph){var list=new ListTag();graph.values().forEach(n->list.add(n.serialize()));return list;}
    public static Map<Integer,FocusElementNode> readGraph(ListTag list){
        if(list.size()>FocusPackage.MAX_NODES)throw new IllegalArgumentException("Too many editor nodes");
        var graph=new HashMap<Integer,FocusElementNode>();
        for(var value:list){var node=new FocusElementNode();node.deserialize((CompoundTag)value);if(graph.put(node.id,node)!=null)throw new IllegalArgumentException("Duplicate editor node");}
        FocalGraph.validateEdit(graph);return graph;
    }
    public CompoundTag editorTag(){var tag=visualTag();tag.put("nodes",writeGraph(data));tag.putString("focusName",focusName);tag.putLong("revision",revision);return tag;}
    private CompoundTag visualTag(){
        var tag=new CompoundTag();tag.putFloat("vis",vis);tag.putFloat("visCost",visCost);tag.putInt("xpCost",xpCost);
        var costs=new CompoundTag();crystalsSync.forEach((a,n)->costs.putInt(a.id(),n));tag.put("crystals",costs);return tag;
    }
    public void readVisual(CompoundTag tag){vis=Math.max(0,tag.getFloat("vis"));visCost=Math.max(0,tag.getFloat("visCost"));xpCost=Math.max(0,tag.getInt("xpCost"));crystalsSync.clear();var costs=tag.getCompound("crystals");for(var key:costs.getAllKeys())Aspect.byId(key).ifPresent(a->crystalsSync.put(a,Math.max(0,costs.getInt(key))));}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);ContainerHelper.saveAllItems(tag,items,registries);tag.merge(editorTag());
        if(paidProgram!=null){tag.put("paidProgram",paidProgram.serialize());tag.put("paidFocus",paidFocus.save(registries));tag.putString("paidName",paidName);}
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);items.clear();ContainerHelper.loadAllItems(tag,items,registries);readVisual(tag);revision=Math.max(0,tag.getLong("revision"));
        focusName=tag.getString("focusName");if(focusName.length()>50)focusName=focusName.substring(0,50);
        data.clear();try{if(tag.contains("nodes"))data=readGraph(tag.getList("nodes",Tag.TAG_COMPOUND));}catch(IllegalArgumentException ignored){}
        paidProgram=null;paidFocus=ItemStack.EMPTY;paidName="";
        if(vis>0&&tag.contains("paidProgram")){
            var program=new FocusPackage();program.deserialize(tag.getCompound("paidProgram"));
            var focus=ItemStack.parseOptional(registries,tag.getCompound("paidFocus"));
            if(program.validate(getMaxComplexity(),null).valid()&&ItemStack.isSameItemSameComponents(focus,getItem(0))){paidProgram=program;paidFocus=focus;paidName=tag.getString("paidName");}
        }
        if(paidProgram==null)cancel();
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){var tag=visualTag();ContainerHelper.saveAllItems(tag,items,registries);return tag;}
    @Override public void handleUpdateTag(CompoundTag tag,HolderLookup.Provider registries){ContainerHelper.loadAllItems(tag,items,registries);readVisual(tag);}
    @Override public void onDataPacket(net.minecraft.network.Connection connection,ClientboundBlockEntityDataPacket packet,HolderLookup.Provider registries){handleUpdateTag(packet.getTag(),registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
