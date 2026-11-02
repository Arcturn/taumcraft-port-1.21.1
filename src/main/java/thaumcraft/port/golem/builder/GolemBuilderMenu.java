package thaumcraft.port.golem.builder;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import thaumcraft.port.golem.*;
import thaumcraft.port.golem.part.*;
import thaumcraft.port.research.ResearchStorage;

/** Selection and crafting are server-owned; vanilla container IDs reject stale button packets. */
public final class GolemBuilderMenu extends AbstractContainerMenu {
 private static final Map<Player,Long> LAST_SELECTION=Collections.synchronizedMap(new WeakHashMap<>());
 public final GolemBuilderEntity builder;private final Player player;private long selected;private int availability;private final int[] optionMasks=new int[5];
 public GolemBuilderMenu(int id,Inventory inventory,BlockPos pos){this(id,inventory,(GolemBuilderEntity)inventory.player.level().getBlockEntity(pos));}
 public GolemBuilderMenu(int id,Inventory inventory,GolemBuilderEntity builder){
  super(GolemContent.BUILDER_MENU.get(),id);this.builder=builder;player=inventory.player;
  if(player instanceof ServerPlayer server){refreshChoices(server);selected=builder.busy()?builder.golem:LAST_SELECTION.getOrDefault(player,0L);try{GolemProperties.fromLong(selected);}catch(IllegalArgumentException e){selected=0;}for(int category=0;category<5;category++){int current=(int)(selected >>> (category*8)&255);if(optionMasks[category]!=0&&(optionMasks[category]&(1<<current))==0&&!builder.busy())setSelection(category,Integer.numberOfTrailingZeros(optionMasks[category]));}}
  addSlot(new Slot(builder,0,160,104){@Override public boolean mayPlace(ItemStack stack){return false;}});
  for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,24+col*18,142+row*18));
  for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,24+col*18,200));
  for(int i=0;i<3;i++){final int part=i;addDataSlot(new DataSlot(){public int get(){return (int)(selected >>> (part*16)&65535);}public void set(int value){selected=selected&~(65535L<<(part*16))|((long)(value&65535)<<(part*16));}});}
  addDataSlot(new DataSlot(){public int get(){return builder.cost;}public void set(int value){builder.cost=value;}});
  addDataSlot(new DataSlot(){public int get(){return builder.maxCost;}public void set(int value){builder.maxCost=value;}});
  addDataSlot(new DataSlot(){public int get(){return availability;}public void set(int value){availability=value;}});
  for(int i=0;i<5;i++){final int category=i;addDataSlot(new DataSlot(){public int get(){return optionMasks[category];}public void set(int value){optionMasks[category]=value;}});}
 }
 public GolemProperties selected(){try{return GolemProperties.fromLong(selected);}catch(IllegalArgumentException e){return new GolemProperties();}}
 public int optionMask(int category){return optionMasks[category];}public boolean hasComponent(int index){return (availability&(1<<index))!=0;}
 public boolean allFound(){var parts=selected().generateComponents();return (availability&((1<<parts.length)-1))==((1<<parts.length)-1)&&Arrays.stream(optionMasks).allMatch(mask->mask!=0);}
 private void setSelection(int category,int id){selected=selected&~(255L<<(category*8))|((long)id<<(category*8));if(player instanceof ServerPlayer)LAST_SELECTION.put(player,selected);}
 private void refreshChoices(ServerPlayer p){var research=ResearchStorage.get(p.getServer()).player(p.getUUID());
  for(int category=0;category<5;category++){optionMasks[category]=0;String[][] keys=switch(category){case 0->Arrays.stream(GolemMaterial.getMaterials()).map(v->v.research).toArray(String[][]::new);case 1->Arrays.stream(GolemHead.getHeads()).map(v->v.research).toArray(String[][]::new);case 2->Arrays.stream(GolemArm.getArms()).map(v->v.research).toArray(String[][]::new);case 3->Arrays.stream(GolemLeg.getLegs()).map(v->v.research).toArray(String[][]::new);default->Arrays.stream(GolemAddon.getAddons()).map(v->v.research).toArray(String[][]::new);};
   for(int i=0;i<keys.length;i++)if(Arrays.stream(keys[i]).allMatch(research::knows))optionMasks[category]|=1<<i;
  }
 }
 @Override public void broadcastChanges(){if(player instanceof ServerPlayer server){refreshChoices(server);availability=0;var found=builder.checkCraft(selected,player);for(int i=0;i<found.length;i++)if(found[i])availability|=1<<i;}super.broadcastChanges();}
 @Override public boolean clickMenuButton(Player p,int button){if(!(p instanceof ServerPlayer server)||p!=player||p.containerMenu!=this||!stillValid(p)||builder.busy())return false;
  if(button==99){boolean result=builder.startCraft(selected,server);broadcastChanges();return result;}
  if(button<0||button>9)return false;int category=switch(button/2){case 0->1;case 1->0;default->button/2;};refreshChoices(server);int mask=optionMasks[category];if(mask==0)return false;
  int current=(int)(selected >>> (category*8)&255),direction=(button&1)==0?-1:1;for(int i=0;i<32;i++){current=Math.floorMod(current+direction,32);if((mask&(1<<current))!=0){setSelection(category,current);broadcastChanges();return true;}}return false;
 }
 @Override public boolean stillValid(Player p){return builder!=null&&builder.stillValid(p);}
 @Override public ItemStack quickMoveStack(Player p,int index){if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();
  if(index==0){if(!moveItemStackTo(stack,1,37,true))return ItemStack.EMPTY;}else if(index<28){if(!moveItemStackTo(stack,28,37,false))return ItemStack.EMPTY;}else if(!moveItemStackTo(stack,1,28,false))return ItemStack.EMPTY;
  if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return copy;
 }
}
