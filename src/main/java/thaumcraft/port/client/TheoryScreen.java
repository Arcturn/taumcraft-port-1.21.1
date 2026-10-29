package thaumcraft.port.client;

import java.util.*;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.theory.*;

/** BETA26's 255x255 canvas, card paper, UVs, positions and seeded paper rotations. */
public final class TheoryScreen extends AbstractContainerScreen<TheoryMenu> {
    private static final ResourceLocation BACK=BookAssets.id("textures/gui/gui_research_table.png"),BASE=BookAssets.id("textures/gui/gui_base.png"),PAPER=BookAssets.id("textures/gui/paper.png"),GILDED=BookAssets.id("textures/gui/papergilded.png"),QUESTION=BookAssets.id("textures/aspects/_unknown.png");
    private final Set<String> selectedAids=new LinkedHashSet<>();
    private final float[] out=new float[3],in=new float[3],hover=new float[3];
    private final Map<String,Integer> displayed=new TreeMap<>();
    private final List<Spark> sparks=new ArrayList<>();
    private final Random visualRandom=new Random();
    private static final class Spark {
        float x,y,vx,vy,green,blue;int age,life,frame;
        Spark(float x,float y,Random r){this.x=x;this.y=y;vx=(float)r.nextGaussian()*.5f;vy=(float)r.nextGaussian()*.5f;life=32+r.nextInt(8);frame=r.nextFloat()<.2f?320:512;green=(189+r.nextInt(67))/255f;blue=(64+r.nextInt(192))/255f;}
        void tick(Random r){x+=vx;y+=vy+.04f;vx=vx*.9f+(float)r.nextGaussian()*.025f;vy=(vy+.04f)*.9f+(float)r.nextGaussian()*.025f;age++;}
    }
    private int previousSelection=-1;private boolean wrote;
    private List<Long> seeds=List.of();private int lastTick=-1;private long frameTime=net.minecraft.Util.getMillis(),sentRevision=-1,requestTime=0;
    private ItemStack tooltipItem=ItemStack.EMPTY;private Component tooltip;
    public TheoryScreen(TheoryMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=255;imageHeight=255;}
    private void send(int action){if(!menu.owned||menu.revision<0||sentRevision==menu.revision&&net.minecraft.Util.getMillis()-requestTime<300)return;sentRevision=menu.revision;requestTime=net.minecraft.Util.getMillis();if(action==0||action==6||action==7)sound("clack",.4f);PacketDistributor.sendToServer(new TheoryActionPayload(menu.containerId,menu.token,menu.revision,action,String.join(",",selectedAids)));}
    private void sound(String id,float volume){minecraft.player.playSound(thaumcraft.port.content.PortSounds.get(id),volume,1);}
    private boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=leftPos+x&&mx<leftPos+x+w&&my>=topPos+y&&my<topPos+y+h;}
    private boolean supplies(){var ink=menu.table.getItem(0);return ink.is(PortContent.SCRIBING_TOOLS.get())&&ink.getDamageValue()<ink.getMaxDamage()&&menu.table.getItem(1).is(Items.PAPER);}
    private void button(GuiGraphics g,int x,int y,String key,boolean enabled,int color,int mx,int my){
        boolean over=inside(mx,my,x-24,y-5,49,11);float shade=enabled?(over?1:.9f):.5f;
        g.setColor((color>>16&255)/255f*shade,(color>>8&255)/255f*shade,(color&255)/255f*shade,enabled?1:.9f);BookAssets.sprite(g,BASE,leftPos+x-25,topPos+y-6,37,66,51,13);g.setColor(1,1,1,1);
        g.pose().pushPose();g.pose().translate(leftPos+x,topPos+y,0);g.pose().scale(.5f,.5f,1);g.drawCenteredString(font,Component.translatable(key),0,-4,over?0xffffa0:0xffffff);g.pose().popPose();
    }
    private void inspiration(GuiGraphics g,int start,int remaining,int y){g.pose().pushPose();g.pose().translate(leftPos+128-start*5,topPos+y,0);g.pose().scale(.5f,.5f,1);for(int i=0;i<start;i++)BookAssets.sprite(g,BASE,20*i,0,i<remaining?32:48,96,16,16);g.pose().popPose();}
    @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
        tooltip=null;tooltipItem=ItemStack.EMPTY;g.setColor(1,1,1,1);BookAssets.sprite(g,BACK,leftPos,topPos,0,0,255,255);
        long now=net.minecraft.Util.getMillis();float dt=Math.clamp((now-frameTime)/50f,.01f,1);frameTime=now;
        var s=menu.view;
        if(s==null){
            seeds=List.of();previousSelection=-1;wrote=false;displayed.clear();selectedAids.retainAll(menu.aids);
            while(selectedAids.size()>=menu.inspiration&&!selectedAids.isEmpty())selectedAids.remove(selectedAids.iterator().next());
            inspiration(g,menu.inspiration,menu.inspiration-selectedAids.size(),55);button(g,128,22,"button.create.theory",menu.revision>=0&&supplies(),0x88ffaa,mx,my);
            int columns=Math.min(menu.aids.size(),6),i=0;
            for(String aid:menu.aids){int x=128+20*(i%columns)-columns*10,y=85+35*(i/columns);i++;
                if(selectedAids.contains(aid)||inside(mx,my,x,y,16,16))BookAssets.sprite(g,BASE,leftPos+x,topPos+y,0,96,16,16);
                var object=TheoryAids.DEFINITIONS.get(aid).object();var item=object.startsWith("entity:")?ItemStack.EMPTY:new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse(object)));
                if(!item.isEmpty())g.renderItem(item,leftPos+x,topPos+y);
                if(inside(mx,my,x,y,16,16))tooltip=item.isEmpty()?Component.translatable("thaumcraft.theory.aid."+aid):item.getHoverName();
            }
            return;
        }
        inspiration(g,s.inspirationStart,s.inspiration,16);
        if(!menu.owned){g.drawCenteredString(font,Component.translatable("thaumcraft.theory.other_owner"),leftPos+128,topPos+30,0xffcc88);}
        if(s.complete())button(g,191,96,"button.complete.theory",menu.owned,0x88ffaa,mx,my);else button(g,128,168,"button.scrap.theory",menu.owned,0xff2222,mx,my);
        for(int i=0;i<Math.min(s.bonusDraws,64);i++)BookAssets.sprite(g,BASE,leftPos+15+i*2,topPos+150+i,64,96,16,16);
        if(!s.complete()&&!menu.table.getItem(1).isEmpty()){
            var r=new Random(55);for(int i=0;i<1+menu.table.getItem(1).getCount()/4;i++)sheet(g,leftPos+65,topPos+100,6,r,1,1,null);
            boolean over=s.choices.isEmpty()&&inside(mx,my,25,55,75,90);g.pose().pushPose();g.pose().translate(leftPos+65,topPos+100,0);g.pose().scale(over?1.75f:1.5f,over?1.75f:1.5f,1);g.setColor(1,1,1,over?1:.5f);image(g,QUESTION,-8,-8,16,16);g.setColor(1,1,1,1);g.pose().popPose();
        }
        for(long seed:s.savedCards)sheet(g,leftPos+191,topPos+100,6,new Random(seed),1,1,null);
        if(s.lastDraw!=null)sheet(g,leftPos+191,topPos+100,6,new Random(s.lastDraw.seed()),1,1,s.lastDraw);
        var current=s.choices.stream().map(TheoryCards.Card::seed).toList();if(!current.equals(seeds)){seeds=current;Arrays.fill(out,0);Arrays.fill(in,0);Arrays.fill(hover,0);previousSelection=-1;wrote=false;}
        if(s.selected>=0&&previousSelection<0)sound("pageturn",1);previousSelection=s.selected;
        for(int i=0;i<s.choices.size();i++){
            var card=s.choices.get(i);boolean chosen=s.selected==i,selected=s.selected>=0;
            boolean over=inside(mx,my,133-55*s.choices.size()+110*i,40,100,120);
            if(out[i]>=.95f&&!selected)hover[i]=Math.clamp(hover[i]+(over?Math.max((.25f-hover[i])/3*dt,.0025f):-.1f*dt),0,.25f);
            if(i==s.choices.size()-1||out[i+1]>.6f){if(out[i]==0)sound("page",1);out[i]=Math.min(1,out[i]+Math.max((1-out[i])/5*dt,.0025f));}
            if(selected){in[i]=Math.min(1,in[i]+(chosen?Math.max((1-in[i])/3*dt,.0025f):.3f*dt));hover[i]=Math.clamp(1-in[i],0,.25f);}
            float x=leftPos+65+(118-55*s.choices.size()+110*i)*out[i];if(chosen)x+=(leftPos+191-x)*in[i];
            float scale=6+out[i]*2-in[i]*2+hover[i];sheet(g,x,topPos+100,scale,new Random(card.seed()),selected&&!chosen?1-in[i]:1,Math.max(1-out[i],in[i]),card);
            if(over&&!selected&&out[i]>=.95f){
                for(int j=0;j<card.required().size();j++)if(mx>=x-9*card.required().size()+18*j&&mx<x-9*card.required().size()+18*j+16&&my>=topPos+136&&my<topPos+152)tooltipItem=TheoryItems.stack(card.required().get(j));
            }
            if(chosen&&in[i]>=1){if(!wrote){sound("write",.3f);wrote=true;}send(5);}
        }
        var rising=new HashSet<String>();
        if(minecraft.player.tickCount!=lastTick){lastTick=minecraft.player.tickCount;sparks.forEach(p->p.tick(visualRandom));sparks.removeIf(p->p.age>=p.life);for(String cat:TheorySession.CATEGORIES){int n=displayed.getOrDefault(cat,0),target=s.totals.getOrDefault(cat,0);if(n<target)rising.add(cat);n+=Integer.compare(target,n);if(n==0)displayed.remove(cat);else displayed.put(cat,n);}}
        var sorted=new ArrayList<>(displayed.entrySet());sorted.sort(Map.Entry.<String,Integer>comparingByValue().reversed());int rank=0;
        for(var e:sorted){int x=leftPos+253,y=topPos+16+rank*18+(rank>0?4:0);image(g,BookAssets.category(e.getKey()),x,y,16,16);String label=e.getValue()+"%"+(rank>s.penaltyStart?" (-"+e.getValue()/3+")":"");int textY=topPos+20+rank*18+(rank>s.penaltyStart?4:0);g.drawString(font,label,leftPos+276,textY,s.blocked.contains(e.getKey())?0x606060:rank<=s.penaltyStart?57536:0xffffff);if(rising.contains(e.getKey()))for(int j=0;j<2;j++)sparks.add(new Spark(leftPos+276+visualRandom.nextFloat()*font.width(label),textY+visualRandom.nextFloat()*8,visualRandom));if(mx>=x&&mx<x+16&&my>=y&&my<y+16)tooltip=category(e.getKey());rank++;}
        g.setColor(1,1,1,1);
    }
    private static void image(GuiGraphics g,ResourceLocation texture,int x,int y,int width,int height){BookAssets.prepareImage(g);g.blit(texture,x,y,width,height,0,0,256,256,256,256);}
    private void sheet(GuiGraphics g,float x,float y,float scale,Random random,float alpha,float tilt,TheoryCards.Card card){
        g.pose().pushPose();g.pose().translate(x+random.nextGaussian(),y+random.nextGaussian(),0);g.pose().scale(scale,scale,1);g.pose().mulPose(Axis.ZP.rotationDegrees((float)random.nextGaussian()*tilt));g.setColor(1,1,1,alpha);g.pose().pushPose();
        if(random.nextBoolean())g.pose().mulPose(Axis.ZP.rotationDegrees(180));if(random.nextBoolean())g.pose().scale(-1,1,1);
        com.mojang.blaze3d.systems.RenderSystem.disableCull();image(g,card!=null&&card.fromAid()?GILDED:PAPER,-8,-8,16,16);g.flush();com.mojang.blaze3d.systems.RenderSystem.enableCull();g.pose().popPose();
        if(card!=null&&alpha==1){
            String category=TheoryCards.researchCategory(card);if(!category.isEmpty()){g.setColor(1,1,1,1/6f);image(g,BookAssets.category(category),-4,-4,8,8);}
            g.setColor(1,1,1,1);g.pose().pushPose();g.pose().scale(.0625f,.0625f,1);
            var title=text(card,true).withStyle(net.minecraft.ChatFormatting.BOLD);g.drawString(font,title,-font.width(title)/2,-65,0,false);int yLine=-48;for(var line:font.split(text(card,false),140)){g.drawString(font,line,-70,yLine,0,false);yLine+=font.lineHeight;}
            int cost=TheoryCards.cost(card.kind()),n=Math.abs(cost)+(cost<0?1:0);for(int i=0;i<n;i++)BookAssets.sprite(g,BASE,-10*n+20*i,-95,cost<0&&i==0?48:32,cost<0&&i==0?0:96,16,16);g.pose().popPose();
            g.pose().pushPose();g.pose().scale(.125f,.125f,1);for(int i=0;i<card.required().size();i++){
                var q=card.required().get(i);int xx=-9*card.required().size()+18*i;g.renderItem(TheoryItems.stack(q),xx,35);
                if(q.consumed()){float bob=(float)Math.sin((minecraft.player.tickCount+i*2)/2f)*.03f;g.pose().pushPose();g.pose().translate(xx-2,45+bob*10,0);g.pose().scale(.5f,.5f+bob,1);BookAssets.sprite(g,BASE,0,0,64,120,16,16);g.pose().popPose();}
            }g.pose().popPose();
        }g.setColor(1,1,1,1);g.pose().popPose();
    }
    private static Component category(String id){return Component.translatable("tc.research_category."+id).withStyle(net.minecraft.ChatFormatting.BOLD);}
    private static String aspect(String id){return Aspect.byId(id).map(Aspect::displayName).orElse(id);}
    private static net.minecraft.network.chat.MutableComponent text(TheoryCards.Card c,boolean title){
        String key=c.kind().equals("DarkWhispers")?"darkwhisper":c.kind().equals("MindOverMatter")?"mindmatter":c.kind().toLowerCase(Locale.ROOT);Object[] args=switch(c.kind()){
            case "Study","Reject"->new Object[]{category(c.category())};case "Analyze"->new Object[]{category(c.category()),category("BASICS")};case "Notation"->new Object[]{category(c.category()),category(c.other())};
            case "Inspired"->new Object[]{c.value(),category(c.category())};case "Celestial"->new Object[]{category(c.category())};
            case "Concentrate","Channel"->new Object[]{aspect(c.aspect())};case "Reactions","Synthesis"->new Object[]{aspect(c.required().get(0).aspect()),aspect(c.required().get(1).aspect())};
            case "Infuse"->new Object[]{aspect(c.aspect()),TheoryItems.stack(c.required().getFirst()).getHoverName(),c.value()};case "Tinker"->new Object[]{c.value()*2,c.value()*2+10};case "MindOverMatter"->new Object[]{10+c.value()};default->new Object[0];};
        return Component.translatable("card."+key+(title?".name":".text"),args);
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button!=0)return super.mouseClicked(x,y,button);var s=menu.view;
        if(s==null){if(supplies()&&inside(x,y,104,17,49,11)){send(0);return true;}int columns=Math.min(menu.aids.size(),6),i=0;for(String aid:menu.aids){int ax=128+20*(i%columns)-columns*10,ay=85+35*(i/columns);i++;if(inside(x,y,ax,ay,16,16)){if(!selectedAids.remove(aid)&&selectedAids.size()+1<menu.inspiration)selectedAids.add(aid);return true;}}}
        else if(menu.owned){
            if(s.complete()&&inside(x,y,167,91,49,11)){send(6);return true;}
            if(!s.complete()&&inside(x,y,104,163,49,11)){send(7);return true;}
            if(s.choices.isEmpty()&&!s.complete()&&inside(x,y,25,55,75,90)){send(1);return true;}
            if(s.selected<0)for(int i=0;i<s.choices.size();i++)if(out[i]>=.95f&&inside(x,y,133-55*s.choices.size()+110*i,40,100,120)){send(2+i);return true;}
        }
        return super.mouseClicked(x,y,button);
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){}
    @Override public void render(GuiGraphics g,int x,int y,float partial){
        super.render(g,x,y,partial);
        if(menu.view!=null){
            g.flush();com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.blendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA,org.lwjgl.opengl.GL11.GL_ONE);
            for(var p:sparks){float t=p.age/(float)p.life,alpha=Math.min(1,Math.min(t*6,(1-t)*6)),size=2.4f*(1+t);int frame=p.frame+p.age%16;
                g.setColor(1,p.green,p.blue,alpha);g.pose().pushPose();g.pose().translate(p.x,p.y,300);g.pose().scale(size,size,1);g.blit(BookAssets.id("textures/particle/legacy.png"),-1,-1,2,2,frame%64*16,frame/64*16,16,16,1024,1024);g.pose().popPose();
            }g.flush();g.setColor(1,1,1,1);com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            var ink=menu.table.getItem(0);int offset=0;
            if(!ink.is(PortContent.SCRIBING_TOOLS.get())||ink.getDamageValue()>=ink.getMaxDamage()){List<Component> lines=List.of(Component.translatable("tile.researchtable.noink.0"),Component.translatable("tile.researchtable.noink.1"));int width=lines.stream().mapToInt(font::width).max().orElse(0);g.renderComponentTooltip(font,lines,leftPos+116-width/2,topPos+60);offset=40;}
            if(menu.table.getItem(1).isEmpty()){var line=Component.translatable("tile.researchtable.nopaper.0");g.renderComponentTooltip(font,List.of(line),leftPos+116-font.width(line)/2,topPos+60+offset);}
        }
        renderTooltip(g,x,y);if(!tooltipItem.isEmpty())g.renderTooltip(font,tooltipItem,x,y);else if(tooltip!=null)g.renderTooltip(font,tooltip,x,y);
    }
}
