package thaumcraft.port.golem.seal.client;

import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import thaumcraft.port.client.BookAssets;
import thaumcraft.port.golem.EnumGolemTrait;
import thaumcraft.port.golem.seal.*;

/** TC6's circular five-category seal editor, original atlas and control positions. */
public final class SealScreen extends AbstractContainerScreen<SealMenu> {
    private static final ResourceLocation TEX=ResourceLocation.parse("thaumcraft:textures/gui/gui_base.png");
    private record Control(int x,int y,int w,int h,int action,Component tooltip) {boolean over(double px,double py){return px>=x&&py>=y&&px<x+w&&py<y+h;}}
    private final List<Control> controls=new ArrayList<>();
    private Component hover;
    public SealScreen(SealMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=176;imageHeight=232;}
    private void sprite(GuiGraphics g,int x,int y,int u,int v,int w,int h){BookAssets.sprite(g,TEX,leftPos+x,topPos+y,u,v,w,h);}
    private void text(GuiGraphics g,Component text,int x,int y,int color){g.drawCenteredString(font,text,leftPos+x,topPos+y,color);}
    private void control(GuiGraphics g,int x,int y,int u,int v,int w,int h,int action,Component label,int mx,int my){sprite(g,x,y,u,v,w,h);var c=new Control(leftPos+x,topPos+y,w,h,action,label);controls.add(c);if(c.over(mx,my))hover=label;}
    private void plus(GuiGraphics g,int x,int y,int action,boolean minus,int mx,int my){control(g,x,y,minus?0:10,0,10,10,action,Component.literal(minus?"−":"+"),mx,my);}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        controls.clear();hover=null;BookAssets.prepareImage(g);sprite(g,8,-8,96,0,160,160);sprite(g,0,143,0,167,176,89);
        text(g,Component.translatable("button.category."+menu.category),88,8,0xffffff);
        float slice=60f/Math.max(1,menu.categories.length),start=-180+(menu.categories.length-1)*slice/2;slice=Math.clamp(slice,12,24);
        for(int i=0;i<menu.categories.length;i++)if(menu.categories.length>1){int x=88+(int)(Math.cos(Math.toRadians(start-i*slice))*86)-8,y=72+(int)(Math.sin(Math.toRadians(start-i*slice))*86)-8;int cat=menu.categories[i];g.setColor(cat==menu.category?1:.7f,cat==menu.category?1:.7f,cat==menu.category?1:.7f,1);control(g,x,y,cat*16,120,16,16,i,Component.translatable("button.category."+cat),mx,my);g.setColor(1,1,1,1);}
        int rx=88+(int)(Math.cos(Math.toRadians(start-menu.categories.length*slice))*86)-8,ry=72+(int)(Math.sin(Math.toRadians(start-menu.categories.length*slice))*86)-8;
        control(g,rx,ry,menu.seal.isRedstoneSensitive()?64:80,136,16,16,menu.seal.isRedstoneSensitive()?28:27,Component.translatable(menu.seal.isRedstoneSensitive()?"golem.prop.redon":"golem.prop.redoff"),mx,my);
        switch(menu.category){
            case 0->{
                text(g,Component.translatable("golem.prop.priority"),88,44,0xbbaaff);text(g,Component.literal(""+menu.seal.getPriority()),88,56,0xffffff);
                plus(g,69,55,80,true,mx,my);plus(g,97,55,81,false,mx,my);
                plus(g,94,76,82,true,mx,my);plus(g,117,76,83,false,mx,my);
                control(g,56,72,menu.seal.isLocked()?32:48,136,16,16,menu.seal.isLocked()?26:25,Component.translatable(menu.seal.isLocked()?"golem.prop.lock":"golem.prop.unlock"),mx,my);
                sprite(g,105,75,2,18,12,12);if(menu.seal.getColor()>0){int c=DyeColor.byId(menu.seal.getColor()-1).getTextureDiffuseColor();g.setColor((c>>16&255)/255f,(c>>8&255)/255f,(c&255)/255f,1);sprite(g,108,78,74,31,6,6);g.setColor(1,1,1,1);}
                if(mx>=leftPos+93&&mx<=leftPos+129&&my>=topPos+75&&my<=topPos+87)text(g,menu.seal.getColor()>0?Component.translatable("golem.prop.color",Component.translatable("color."+DyeColor.byId(menu.seal.getColor()-1).getName())):Component.translatable("golem.prop.colorall"),111,89,0xffffff);
                if(minecraft.player!=null&&menu.seal.getOwner().equals(minecraft.player.getUUID().toString()))text(g,Component.translatable("golem.prop.owner"),88,104,0xbbaaff);
            }
            case 1->{if(menu.seal.getSeal() instanceof ISealConfigFilter filter){int size=filter.getFilterSize(),sx=16+(size-1)%3*12,sy=16+(size-1)/3*12;for(int i=0;i<size;i++)sprite(g,88+i%3*24-sx,72+i/3*24-sy,0,56,32,32);control(g,80,72+(size-1)/3*24-sy+27,filter.isBlacklist()?0:16,136,16,16,filter.isBlacklist()?21:20,Component.translatable(filter.isBlacklist()?"button.bl":"button.wl"),mx,my);}}
            case 2->{int[] values={menu.seal.getArea().getY(),menu.seal.getArea().getX(),menu.seal.getArea().getZ()};String[] axes={"y","x","z"};for(int i=0;i<3;i++){int y=47+i*25;text(g,Component.translatable("button.caption."+axes[i]),88,39+i*24,0xdddddd);text(g,Component.literal(""+values[i]),88,48+i*24,0xffffff);plus(g,69,y,90+i*2,true,mx,my);plus(g,97,y,91+i*2,false,mx,my);}}
            case 3->{if(menu.seal.getSeal() instanceof ISealConfigToggles toggles){var all=toggles.getToggles();int spacing=all.length<4?8:all.length<6?7:all.length<9?6:5,h=(all.length-1)*spacing,width=12;for(var t:all)width=Math.max(width,(12+Math.min(100,font.width(Component.translatable(t.getName()))))/2);for(int i=0;i<all.length;i++){var t=all[i];int x=88-width,y=67-h+i*spacing*2;control(g,x-2,y-2,2,18,12,12,t.getValue()?60+i:30+i,Component.translatable(t.getName()),mx,my);if(t.getValue())sprite(g,x-2,y-2,18,18,12,12);g.drawString(font,Component.translatable(t.getName()),leftPos+x+12,topPos+y,0xffffff);}}}
            case 4->{text(g,Component.translatable("button.caption.required"),88,46,0xdddddd);text(g,Component.translatable("button.caption.forbidden"),88,78,0xdddddd);traits(g,menu.seal.getSeal().getRequiredTags(),56,mx,my);traits(g,menu.seal.getSeal().getForbiddenTags(),88,mx,my);}
        }
    }
    private void traits(GuiGraphics g,EnumGolemTrait[] traits,int y,int mx,int my){if(traits==null)return;for(int i=0;i<traits.length;i++){var trait=traits[i];int x=80+i*18-(traits.length-1)*9;g.blit(trait.icon,leftPos+x,topPos+y,0,0,16,16,16,16);if(mx>=leftPos+x&&mx<leftPos+x+16&&my>=topPos+y&&my<topPos+y+16)hover=Component.translatable("golem.trait."+trait.name().toLowerCase(Locale.ROOT)).append("\n").append(Component.translatable("golem.trait.text."+trait.name().toLowerCase(Locale.ROOT)));}}
    @Override protected void renderLabels(GuiGraphics g,int x,int y){ }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){super.render(g,mx,my,partial);if(menu.category==1&&menu.seal.getSeal() instanceof ISealConfigFilter f&&!f.isBlacklist()){g.pose().pushPose();g.pose().translate(0,0,300);for(int i=0;i<menu.ghosts;i++)if(!f.getFilterSlot(i).isEmpty()){var slot=menu.getSlot(i);int size=f.getFilterSlotSize(i);String label=size==0?"*":""+size;g.drawString(font,label,leftPos+slot.x+17-font.width(label),topPos+slot.y+9,size==0?0xffcc55:0xffffff);}g.pose().popPose();}renderTooltip(g,mx,my);if(hover!=null)g.renderTooltip(font,font.split(hover,200),mx,my);}
    @Override public boolean mouseClicked(double x,double y,int button){if(button==0)for(var c:controls)if(c.over(x,y)){if(c.action()<menu.categories.length)menu.category=menu.categories[c.action()];minecraft.gameMode.handleInventoryButtonClick(menu.containerId,c.action());return true;}return super.mouseClicked(x,y,button);}
}
