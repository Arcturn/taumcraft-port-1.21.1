package thaumcraft.port.golem.builder.client;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.golem.*;
import thaumcraft.port.golem.builder.*;

/** Source 208x224 press atlas, selector coordinates, trait icons and component grid. */
public final class GolemBuilderScreen extends AbstractContainerScreen<GolemBuilderMenu> {
 private static final ResourceLocation TEX=ResourceLocation.parse("thaumcraft:textures/gui/gui_golembuilder.png"),BASE=ResourceLocation.parse("thaumcraft:textures/gui/gui_base.png");
 private record Hover(int x,int y,ItemStack item,String name,String description){}
 private final List<Hover> hovers=new ArrayList<>();
 public GolemBuilderScreen(GolemBuilderMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=208;imageHeight=224;}
 private void icon(GuiGraphics g,ResourceLocation image,int x,int y,int color,String name,String description){g.setColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,1);g.blit(image,leftPos+x-8,topPos+y-8,16,16,0,0,16,16,16,16);g.setColor(1,1,1,1);hovers.add(new Hover(leftPos+x-8,topPos+y-8,ItemStack.EMPTY,name,description));}
 @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
  hovers.clear();g.blit(TEX,leftPos,topPos,0,0,imageWidth,imageHeight);var p=menu.selected();var b=menu.builder;
  for(int[] r:new int[][]{{12,12},{12,60},{108,12},{108,36},{108,60}})g.blit(TEX,leftPos+r[0],topPos+r[1],228,124,24,24);
  if(menu.optionMask(0)!=0)icon(g,ResourceLocation.parse("thaumcraft:textures/items/golem.png"),24,24,p.getMaterial().itemColor,p.getMaterial().getLocalizedName(),p.getMaterial().getLocalizedDescription());
  if(menu.optionMask(1)!=0)icon(g,p.getHead().icon,120,24,0xffffff,p.getHead().getLocalizedName(),p.getHead().getLocalizedDescription());
  if(menu.optionMask(2)!=0)icon(g,p.getArms().icon,120,48,0xffffff,p.getArms().getLocalizedName(),p.getArms().getLocalizedDescription());
  if(menu.optionMask(3)!=0)icon(g,p.getLegs().icon,120,72,0xffffff,p.getLegs().getLocalizedName(),p.getLegs().getLocalizedDescription());
  if(menu.optionMask(4)!=0&&!p.getAddon().key.equals("NONE"))icon(g,p.getAddon().icon,24,72,0xffffff,p.getAddon().getLocalizedName(),p.getAddon().getLocalizedDescription());
  int[][] buttons={{101,19},{129,19},{5,19},{33,19},{101,43},{129,43},{101,67},{129,67},{5,67},{33,67}};
  for(int i=0;i<buttons.length;i++){int category=switch(i/2){case 0->1;case 1->0;default->i/2;};if(Integer.bitCount(menu.optionMask(category))<=1)continue;g.setColor(1,1,1,b.busy()?.35f:1);g.blit(BASE,leftPos+buttons[i][0],topPos+buttons[i][1],(i&1)==0?20:30,0,10,10);g.setColor(1,1,1,1);}
  var traits=p.getTraits().toArray(EnumGolemTrait[]::new);int yy=traits.length<=4?(traits.length-1)%4*8:24,xx=(traits.length-1)/4%4*8;
  for(int i=0;i<traits.length;i++){var t=traits[i];icon(g,t.icon,72+(i/4)*16-xx,48+(i%4)*16-yy,0xffffff,t.getLocalizedName(),t.getLocalizedDescription());}
  var parts=p.generateComponents();icon(g,ResourceLocation.parse("thaumcraft:textures/aspects/machina.png"),152,24,Aspect.MECHANISM.color(),"Machina",Component.translatable("tc.aspect.machina").getString());
  g.drawString(font,Integer.toString(p.constructionCost()),leftPos+162-font.width(Integer.toString(p.constructionCost())),topPos+24,0xffffff,false);
  for(int i=0;i<parts.length;i++){int column=(i+1)/4,row=(i+1)%4,x=leftPos+144+column*16,y=topPos+16+row*16;if(!menu.hasComponent(i)){g.setColor(1,1,1,.5f);g.blit(TEX,x,y,240,0,16,16);g.setColor(1,1,1,1);}g.renderItem(parts[i],x,y);g.renderItemDecorations(font,parts[i],x,y);hovers.add(new Hover(x,y,parts[i],"",""));}
  int hp=10+p.getMaterial().healthMod,armor=p.getMaterial().armor;if(p.hasTrait(EnumGolemTrait.FRAGILE))hp=(int)(hp*.75);if(p.hasTrait(EnumGolemTrait.ARMORED))armor=(int)Math.max(armor*1.5,armor+1);if(p.hasTrait(EnumGolemTrait.FRAGILE))armor=(int)(armor*.75);
  double damage=p.hasTrait(EnumGolemTrait.FIGHTER)?p.getMaterial().damage:0;if(p.hasTrait(EnumGolemTrait.BRUTAL))damage=Math.max(damage*1.5,damage+1);
  g.drawCenteredString(font,Float.toString(hp/2f),leftPos+48,topPos+108,0xffffff);g.drawCenteredString(font,Float.toString(armor/2f),leftPos+72,topPos+108,0xffffff);g.drawCenteredString(font,Float.toString((float)damage/2),leftPos+97,topPos+108,0xffffff);
  g.blit(TEX,leftPos+120,topPos+104,216,64,24,16);if(b.busy()||!menu.allFound())g.blit(TEX,leftPos+120,topPos+104,216,40,24,16);
  if(b.cost>0&&b.maxCost>0)g.blit(TEX,leftPos+145,topPos+89,209,89,(int)(46*(1-b.cost/(float)b.maxCost)),6);
 }
 @Override protected void renderLabels(GuiGraphics g,int mx,int my){}
 @Override public void render(GuiGraphics g,int mx,int my,float partial){super.render(g,mx,my,partial);renderTooltip(g,mx,my);for(var hover:hovers)if(mx>=hover.x&&mx<hover.x+16&&my>=hover.y&&my<hover.y+16){if(!hover.item.isEmpty())g.renderTooltip(font,hover.item,mx,my);else{var lines=new ArrayList<net.minecraft.util.FormattedCharSequence>();lines.add(Component.literal(hover.name).getVisualOrderText());if(!hover.description.isEmpty())lines.addAll(font.split(Component.literal(hover.description).withStyle(net.minecraft.ChatFormatting.BLUE,net.minecraft.ChatFormatting.ITALIC),220));g.renderTooltip(font,lines,mx,my);}break;}}
 @Override public boolean mouseClicked(double x,double y,int button){if(button==0&&!menu.builder.busy()&&minecraft!=null&&minecraft.gameMode!=null){int[][] buttons={{101,19},{129,19},{5,19},{33,19},{101,43},{129,43},{101,67},{129,67},{5,67},{33,67}};for(int i=0;i<buttons.length;i++)if(x>=leftPos+buttons[i][0]&&x<leftPos+buttons[i][0]+10&&y>=topPos+buttons[i][1]&&y<topPos+buttons[i][1]+10){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,i);return true;}if(x>=leftPos+120&&x<leftPos+144&&y>=topPos+104&&y<topPos+120&&menu.allFound()){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,99);return true;}}return super.mouseClicked(x,y,button);}
}
