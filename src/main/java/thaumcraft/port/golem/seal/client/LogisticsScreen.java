package thaumcraft.port.golem.seal.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.port.client.BookAssets;
import thaumcraft.port.golem.seal.*;

/** BETA26's 9x9 provider browser and requested-quantity slider. */
public final class LogisticsScreen extends AbstractContainerScreen<LogisticsMenu> {
    private static final ResourceLocation TEX=ResourceLocation.parse("thaumcraft:textures/gui/gui_logistics.png"),BASE=ResourceLocation.parse("thaumcraft:textures/gui/gui_base.png");
    private EditBox search;
    private ItemStack selection=ItemStack.EMPTY;
    private int selected=-1,count=1,drag;
    public LogisticsScreen(LogisticsMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=215;imageHeight=215;}
    @Override protected void init(){super.init();search=new EditBox(font,leftPos+143,topPos+196,55,9,Component.translatable("tc.logistics.search"));search.setMaxLength(40);search.setBordered(false);search.setValue(menu.search);search.setResponder(value->send(-1,0,0,value));addRenderableWidget(search);}
    private void send(int slot,int amount,int row,String query){PacketDistributor.sendToServer(new SealPayloads.LogisticsAction(menu.containerId,slot,amount,row,query,menu.revision));}
    private void scroll(int row){send(-1,0,Math.clamp(row,0,menu.end),search.getValue());}
    private int slot(double x,double y){int rx=(int)x-leftPos-19,ry=(int)y-topPos-19;if(rx<0||ry<0||rx>=171||ry>=171||rx%19>=16||ry%19>=16)return -1;int id=rx/19+ry/19*9;return id<menu.page.size()?id:-1;}
    private void locateSelection(){if(selection.isEmpty()){selected=-1;return;}selected=-1;for(int i=0;i<menu.page.size();i++)if(ItemStack.isSameItemSameComponents(selection,menu.page.get(i).stack())){selected=i;count=Math.clamp(count,1,Math.max(1,menu.page.get(i).count()));break;}}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        locateSelection();BookAssets.prepareImage(g);g.blit(TEX,leftPos,topPos,0,0,imageWidth,imageHeight);
        if(selected>=0)g.blit(TEX,leftPos+17+selected%9*19,topPos+17+selected/9*19,222,46,20,20);
        for(int i=0;i<menu.page.size();i++){var entry=menu.page.get(i);int x=leftPos+19+i%9*19,y=topPos+19+i/9*19;g.renderItem(entry.stack(),x,y);g.renderItemDecorations(font,entry.stack(),x,y,entry.count()==1?"":""+entry.count());}
        g.blit(BASE,leftPos+195,topPos+16,67,0,10,10);g.blit(BASE,leftPos+195,topPos+180,67,10,10,10);
        g.pose().pushPose();g.pose().translate(leftPos+198,topPos+28,0);g.pose().scale(1,149f/32,1);g.blit(BASE,0,0,240,176,4,32);g.pose().popPose();int thumb=topPos+28+(menu.end==0?0:Math.round(menu.start*141f/menu.end));g.blit(BASE,leftPos+196,thumb,20,20,8,8);
        if(selected>=0){g.blit(BASE,leftPos+13,topPos+195,0,0,10,10);g.blit(BASE,leftPos+57,topPos+195,10,0,10,10);g.blit(BASE,leftPos+24,topPos+198,208,176,32,4);int maximum=menu.page.get(selected).count();int tick=leftPos+24+(maximum<=1?0:Math.round((count-1)*24f/(maximum-1)));g.blit(BASE,tick,topPos+196,20,20,8,8);g.drawCenteredString(font,""+count,leftPos+83,topPos+196,0xff333333);g.blit(BASE,leftPos+96,topPos+194,37,82,40,13);g.pose().pushPose();g.pose().translate(leftPos+116,topPos+200,0);g.pose().scale(.5f,.5f,1);g.drawCenteredString(font,Component.translatable("tc.logistics.request"),0,-4,mx>=leftPos+96&&mx<leftPos+136&&my>=topPos+194&&my<topPos+207?0xffffa0:0xffffff);g.pose().popPose();}
        if(!search.isFocused()&&search.getValue().isEmpty())g.drawString(font,Component.translatable("tc.logistics.search"),leftPos+146,topPos+197,0xff222222,false);
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){}
    @Override public void render(GuiGraphics g,int x,int y,float partial){super.render(g,x,y,partial);int i=slot(x,y);if(i>=0)g.renderTooltip(font,menu.page.get(i).stack(),x,y);else if(selected>=0&&x>=leftPos+96&&x<leftPos+136&&y>=topPos+194&&y<topPos+207)g.renderTooltip(font,Component.translatable("tc.logistics.request"),x,y);}
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==0){int i=slot(x,y);if(i>=0){selected=i;selection=menu.page.get(i).stack().copy();count=1;return true;}double rx=x-leftPos,ry=y-topPos;
            if(rx>=195&&rx<205&&ry>=16&&ry<26){scroll(menu.start-1);return true;}if(rx>=195&&rx<205&&ry>=180&&ry<190){scroll(menu.start+1);return true;}
            if(rx>=196&&rx<204&&ry>=28&&ry<177){drag=1;slide(x,y);return true;}
            if(selected>=0){if(rx>=13&&rx<23&&ry>=195&&ry<205){count=Math.max(1,count-(hasShiftDown()?10:1));return true;}if(rx>=57&&rx<67&&ry>=195&&ry<205){count=Math.min(menu.page.get(selected).count(),count+(hasShiftDown()?10:1));return true;}if(rx>=24&&rx<56&&ry>=196&&ry<204){drag=2;slide(x,y);return true;}if(rx>=96&&rx<136&&ry>=194&&ry<207){send(selected,count,menu.start,search.getValue());return true;}}
        }
        return super.mouseClicked(x,y,button);
    }
    private void slide(double x,double y){if(drag==1)scroll(Math.round((float)Math.clamp((y-topPos-32)/141,0,1)*menu.end));else if(drag==2&&selected>=0)count=1+Math.round((float)Math.clamp((x-leftPos-28)/24,0,1)*(menu.page.get(selected).count()-1));}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(button==0&&drag>0){slide(x,y);return true;}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseReleased(double x,double y,int button){if(button==0&&drag>0){drag=0;return true;}return super.mouseReleased(x,y,button);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){scroll(menu.start+(vertical<0?1:-1));return true;}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(search.isFocused()&&key!=256)return search.keyPressed(key,scan,modifiers);return super.keyPressed(key,scan,modifiers);}
}
