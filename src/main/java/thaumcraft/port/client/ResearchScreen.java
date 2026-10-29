package thaumcraft.port.client;
import java.util.*;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.network.*;
import thaumcraft.port.research.*;

/** Original browser atlas, backgrounds, tabs, 24-pixel grid and connection routing. */
public final class ResearchScreen extends Screen {
    private static String lastCategory="BASICS";
    private static final Map<String,double[]> POSITIONS=new HashMap<>();
    private ResearchState state;
    private JournalPayload payload;
    private String category=lastCategory;
    private final ResearchBrowserLayout.Motion motion=new ResearchBrowserLayout.Motion();
    private boolean initialized;
    private float zoom=1;
    private static boolean searching;
    private EditBox search;
    private ResearchCatalog.Entry hovered;
    private List<SearchResult> results=List.of();
    private record SearchResult(String label,String category,ResearchCatalog.Entry entry,String recipe){}
    public ResearchScreen(JournalPayload payload){super(Component.translatable("item.thaumcraft.thaumonomicon"));update(payload);}
    public void update(JournalPayload p){payload=p;state=ResearchCodec.read(p.progress());if(!ResearchView.category(state,category))category="BASICS";updateSearch();}
    public static void reset(){POSITIONS.clear();lastCategory="BASICS";searching=false;}
    static void sound(String name){sound(name,.25f,1);}
    static void sound(String name,float volume,float pitch){var player=Minecraft.getInstance().player;if(player!=null)player.playSound(PortSounds.get(name),volume,pitch);}
    private static void buttonSound(){Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK,1));}
    @Override protected void init(){
        search=addRenderableWidget(new EditBox(font,20,20,89,font.lineHeight,Component.translatable("tc.search")));
        search.setMaxLength(15);search.setBordered(false);search.setTextColor(0xffffff);search.setVisible(searching);search.setResponder(s->updateSearch());if(searching){setFocused(search);search.setFocused(true);}
        if(!initialized){var p=POSITIONS.get(category);if(p!=null){zoom=(float)p[2];position(p[0],p[1]);}else center();initialized=true;}clampMap();updateSearch();
    }
    private List<ResearchCatalog.Entry> entries(){return ResearchCatalog.all().stream().filter(e->e.category().equals(category)&&ResearchView.visible(state,e)).toList();}
    private ResearchBrowserLayout.Bounds bounds(boolean horizontal){
        var es=entries();
        int min=es.stream().mapToInt(e->horizontal?e.x():e.y()).min().orElse(0);
        int max=es.stream().mapToInt(e->horizontal?e.x():e.y()).max().orElse(0);
        return ResearchBrowserLayout.bounds(min,max,horizontal?width:height);
    }
    private double mapX(){return motion.current().x();}
    private double mapY(){return motion.current().y();}
    private void position(double x,double y){motion.snap(x,y);}
    private void center(){position(bounds(true).center(),bounds(false).center());clampMap();}
    private void clampMap(){motion.aim(bounds(true).clamp(mapX(),zoom),bounds(false).clamp(mapY(),zoom));}
    private void remember(){POSITIONS.put(category,new double[]{mapX(),mapY(),zoom});lastCategory=category;}
    private void choose(String key){remember();category=key;var p=POSITIONS.get(key);if(p==null)center();else{zoom=(float)p[2];position(p[0],p[1]);clampMap();}searching=false;search.setVisible(false);}
    private void updateSearch(){
        if(search==null)return;String term=search.getValue().toLowerCase(Locale.ROOT);List<SearchResult> found=new ArrayList<>();
        for(String c:BookAssets.CATEGORIES)if(ResearchView.category(state,c)){String name=Component.translatable("tc.research_category."+c).getString();if(c.toLowerCase(Locale.ROOT).contains(term))found.add(new SearchResult(name,c,null,null));}
        for(var e:ResearchCatalog.all())if(state.isResearchKnown(e.key())){String name=Component.translatable(e.title()).getString();if(name.toLowerCase(Locale.ROOT).contains(term))found.add(new SearchResult(name,e.category(),e,null));}
        Set<String> outputs=new HashSet<>();
        for(var e:ResearchCatalog.all())if(state.isResearchKnown(e.key()))for(String recipe:ResearchView.searchRecipes(state,e))if(outputs.add(recipe)){
            var variants=BookRecipeCatalog.recipes(recipe);if(variants.size()!=1)continue;var value=variants.getFirst();
            if(value.get("type").getAsString().contains("blueprint")||!value.has("result")||value.has("book_group")&&value.get("book_group").getAsString().equalsIgnoreCase(recipe))continue;
            String name=BookAssets.name(RecipeBook.output(recipe,0)).getString();if(name.toLowerCase(Locale.ROOT).contains(term))found.add(new SearchResult(name,e.category(),e,recipe));
        }
        found.sort(Comparator.comparing(SearchResult::label));results=List.copyOf(found);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        clampMap();
        var interpolated=motion.interpolate(partial);
        double originX=bounds(true).display(interpolated.x(),zoom),originY=bounds(false).display(interpolated.y(),zoom);
        double panX=ResearchBrowserLayout.pan(originX,width,zoom),panY=ResearchBrowserLayout.pan(originY,height,zoom);
        g.fill(0,0,width,height,0xff000000);com.mojang.blaze3d.systems.RenderSystem.enableBlend();g.enableScissor(14,14,width-14,height-14);
        tiled(g,BookAssets.background(category),-originX/2,-originY/2);tiled(g,BookAssets.id("textures/gui/gui_research_back_over.png"),-originX/1.5,-originY/1.5);hovered=null;
        if(!searching){
            g.pose().pushPose();g.pose().translate(width/2.0,height/2.0,0);g.pose().scale(1/zoom,1/zoom,1);g.pose().translate(panX,panY,0);
            var entries=entries();
            for(var e:entries){
                for(String key:e.parents()){
                    var p=ResearchCatalog.get(ResearchView.clean(key));
                    if(key.startsWith("~")||p==null||!p.category().equals(category)||!ResearchView.visible(state,p)||p.siblings().contains(e.key()))continue;
                    float c=state.knows(key)?.6f:.2f;g.setColor(c,c,c,1);line(g,e,p,true,e.meta().contains("REVERSE"));
                }
                for(String key:e.siblings()){
                    var p=ResearchCatalog.get(key);
                    if(key.startsWith("~")||p==null||!p.category().equals(category)||!ResearchView.visible(state,p))continue;
                    if(state.knows(p.key()))g.setColor(.3f,.3f,.4f,1);else g.setColor(.1875f,.1875f,.25f,1);
                    line(g,p,e,false,e.meta().contains("REVERSE"));
                }
            }
            for(var e:entries){int x=e.x()*24,y=e.y()*24;boolean active=state.available(e)||state.knows(e.key());
                if(e.stages().stream().anyMatch(stage->stage.has("warp")&&stage.get("warp").getAsInt()>0))forbidden(g,x,y);
                float c=ResearchBrowserLayout.frameBrightness(state.knows(e.key()),active,Util.getMillis());g.setColor(c,c,c,1);
                int u=e.meta().contains("ROUND")?144:e.meta().contains("HEX")?112:80,v=e.meta().contains("HIDDEN")?80:48;
                BookAssets.sprite(g,BookAssets.BROWSER,x-16,y-16,u,v,32,32);
                if(e.meta().contains("SPIKY"))BookAssets.sprite(g,BookAssets.BROWSER,x-16,y-16,176,v,32,32);
                boolean unread=unread(e);
                if(ResearchView.newResearch(state,e.key()))badge(g,x-17,y-17,.5f,176,1);
                if(ResearchView.newPage(state,e.key()))badge(g,x-17,y+1,.5f,208,1);
                BookAssets.researchBrowserIcon(g,e,x-8,y-8,!active,unread?1:c);
                g.setColor(1,1,1,1);
                double hx=(mx-width/2.0)*zoom-panX,hy=(my-height/2.0)*zoom-panY;
                if(mx>=16&&mx<width-16&&my>=16&&my<height-16&&Math.abs(hx-x)<=10&&Math.abs(hy-y)<=10)hovered=e;
            }
            g.pose().popPose();
        }else{
            search.render(g,mx,my,partial);int row=0;for(var r:results){int y=32+row*10;if(r.recipe()!=null){g.pose().pushPose();g.pose().scale(.5f,.5f,1);BookAssets.sprite(g,BookAssets.BROWSER,44,y*2,224,48,16,16);g.pose().popPose();}g.drawString(font,r.label(),32,y,mx>22&&mx<width-14&&my>=y&&my<y+8?(r.recipe()!=null?0xccccff:0xffffff):r.entry()==null?0xddaaaa:r.recipe()!=null?0xaaaadd:0xdddddd,false);if(32+(++row+1)*10>height-32){g.drawString(font,Component.translatable("tc.search.more"),22,34+row*10,0xaaaaaa,false);break;}}
        }
        g.disableScissor();frame(g);int index=0;
        for(String c:BookAssets.CATEGORIES)if(ResearchView.category(state,c)){
            int y=10+(++index)*24;boolean over=mx>=1&&mx<17&&my>=y&&my<y+16,selected=c.equals(category);g.setColor(selected?.6f:1,1,1,1);
            BookAssets.sprite(g,BookAssets.BROWSER,-2,y-3,13,13,22,22);
            float shade=selected||over?1:.66f;g.setColor(shade,shade,shade,selected||over?1:.8f);BookAssets.texture(g,BookAssets.category(c),1,y,16);g.setColor(1,1,1,1);
            boolean newResearch=ResearchView.categoryFlag(state,c,ResearchState.ResearchFlag.RESEARCH),newPage=ResearchView.categoryFlag(state,c,ResearchState.ResearchFlag.PAGE);
            if(newResearch)badge(g,-1,y-2,.25f,176,.7f);if(newPage)badge(g,-1,y+9,.25f,208,.7f);
            if(over){g.drawString(font,Component.translatable("tc.research_category."+c).append(" ("+ResearchView.completion(state,c)+"%)"),23,y+4,0xffffff,true);int row=13;if(newResearch){g.drawString(font,Component.translatable("tc.research.newresearch"),23,y+row,0xffffff,true);row+=9;}if(newPage)g.drawString(font,Component.translatable("tc.research.newpage"),23,y+row,0xffffff,true);}
        }
        boolean overSearch=mx>=1&&mx<17&&my>=height-17&&my<height-1;float searchShade=overSearch?1:.8f;g.setColor(searchShade,searchShade,searchShade,1);
        BookAssets.sprite(g,BookAssets.BROWSER,1,height-17,160,16,16,16);g.setColor(1,1,1,1);if(overSearch)g.drawString(font,Component.translatable("tc.search"),20,height-13,0xffffff,true);
        if(hovered!=null){var lines=new ArrayList<Component>();lines.add(Component.translatable(hovered.title()).withStyle(ChatFormatting.GOLD));
            if(!state.available(hovered)&&!state.knows(hovered.key())){lines.add(Component.translatable("tc.researchmissing").withStyle(ChatFormatting.RED));for(String p:hovered.parents())if(!state.knows(p)){var parent=ResearchCatalog.get(ResearchView.clean(p));lines.add(Component.literal(" - ").append(parent==null?Component.literal("?"):Component.translatable(parent.title())).withStyle(ChatFormatting.YELLOW));}}
            else if(!state.knows(hovered.key()))lines.add(Component.translatable(state.stage(hovered.key())==0?"tc.research.begin":"tc.research.stage").append(state.stage(hovered.key())==0?"":" "+state.stage(hovered.key())+"/"+hovered.stages().size()).withStyle(state.stage(hovered.key())==0?ChatFormatting.GREEN:ChatFormatting.AQUA));
            if(ResearchView.newResearch(state,hovered.key()))lines.add(Component.translatable("tc.research.newresearch"));
            if(ResearchView.newPage(state,hovered.key()))lines.add(Component.translatable("tc.research.newpage"));
            researchTooltip(g,lines,mx+3,my-3);
        }
    }
    private boolean unread(ResearchCatalog.Entry e){return ResearchView.unread(state,e.key());}
    private void badge(GuiGraphics g,int x,int y,float scale,int u,float alpha){g.setColor(1,1,1,alpha);g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(scale,scale,1);BookAssets.sprite(g,BookAssets.BROWSER,0,0,u,16,32,32);g.pose().popPose();g.setColor(1,1,1,1);}
    /** GuiResearchBrowser.drawForbidden uses additive blending; image helpers reset that state. */
    private void forbidden(GuiGraphics g,int x,int y){
        int frame=160+minecraft.player.tickCount%32;
        g.flush();RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,GlStateManager.DestFactor.ONE);
        g.setColor(.33f,0,.44f,.9f);
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90));
        g.blit(BookAssets.id("textures/misc/auranodes.png"),-45,-45,90,90,
                (frame%32)*16,(frame/32)*16,16,16,512,512);
        g.flush();g.pose().popPose();RenderSystem.defaultBlendFunc();g.setColor(1,1,1,1);
    }
    /** Native-size text is the requested readability adaptation; TC6 frame colors remain. */
    private void researchTooltip(GuiGraphics g,List<Component> content,int mouseX,int mouseY){
        BookTooltip.render(g,content,mouseX,mouseY,mouseX-3,width,height,-99);
    }
    private void tiled(GuiGraphics g,net.minecraft.resources.ResourceLocation t,double ox,double oy){BookAssets.prepareImage(g);int tile=Math.max(1,Math.round(256/zoom));int sx=14+Math.floorMod((int)(ox/zoom),tile)-tile,sy=14+Math.floorMod((int)(oy/zoom),tile)-tile;for(int x=sx;x<width-14;x+=tile)for(int y=sy;y<height-14;y+=tile)g.blit(t,x,y,0,0,tile,tile,tile,tile);}
    private void frame(GuiGraphics g){
        for(int x=16;x<width-16;x+=64){int n=Math.min(64,width-16-x);BookAssets.sprite(g,BookAssets.BROWSER,x,-2,48,13,n,22);BookAssets.sprite(g,BookAssets.BROWSER,x,height-20,48,13,n,22);}
        for(int y=16;y<height-16;y+=64){int n=Math.min(64,height-16-y);BookAssets.sprite(g,BookAssets.BROWSER,-2,y,13,48,22,n);BookAssets.sprite(g,BookAssets.BROWSER,width-20,y,13,48,22,n);}
        for(int x:new int[]{-2,width-20})for(int y:new int[]{-2,height-20})BookAssets.sprite(g,BookAssets.BROWSER,x,y,13,13,22,22);
    }
    private void line(GuiGraphics g,ResearchCatalog.Entry a,ResearchCatalog.Entry b,boolean arrow,boolean flipped){
        int x=a.x(),y=a.y(),x2=b.x(),y2=b.y(),xd=Math.abs(x-x2),yd=Math.abs(y-y2),xm=Integer.signum(flipped?x-x2:x2-x),ym=Integer.signum(flipped?y-y2:y2-y);
        int xx=(flipped?x2:x)*24-12,yy=(flipped?y2:y)*24-12;boolean big=xd>1&&yd>1;
        if(arrow){int u=flipped?(xm<0?160:xm>0?128:ym>0?64:96):(ym<0?64:ym>0?96:xm>0?160:128);BookAssets.sprite(g,BookAssets.BROWSER,x*24-16,y*24-16,u,112,32,32);}
        int v=1;for(;v<yd-(big?1:0);v++)BookAssets.sprite(g,BookAssets.BROWSER,xx,yy+ym*24*v,0,228,24,24);
        if(xm!=0&&ym!=0){int u=ym>0?(xm<0?0:48):(xm<0?96:144);if(big)BookAssets.sprite(g,BookAssets.BROWSER,xx+(xm<0?-24:0),yy+ym*24*v+(ym<0?-24:0),u,180,48,48);else BookAssets.sprite(g,BookAssets.BROWSER,xx,yy+ym*24*v,48+u/2,228,24,24);}
        for(int h=big?2:1;h<xd;h++)BookAssets.sprite(g,BookAssets.BROWSER,xx+xm*24*h,yy+ym*24*yd,24,228,24,24);
    }
    private void open(ResearchCatalog.Entry e){
        if(!state.available(e)&&!state.isResearchKnown(e.key()))return;remember();
        if(!state.isResearchKnown(e.key()))PacketDistributor.sendToServer(new ResearchActionPayload(e.key(),0));
        else{state.clearReadFlags(e.key());payload.progress().put("research_flags",ResearchCodec.write(state).get("research_flags"));PacketDistributor.sendToServer(new BookReadPayload(e.key()));if(!state.knows(e.key())&&state.stage(e.key())>1&&state.stage(e.key())>=e.stages().size())PacketDistributor.sendToServer(new ResearchActionPayload(e.key(),state.stage(e.key()),ResearchActionPayload.Action.TERMINAL_NODE));}
        minecraft.setScreen(new ResearchPageScreen(payload,e.key()));
    }
    @Override public boolean mouseClicked(double x,double y,int button){
        if(button==0){int index=0;for(String c:BookAssets.CATEGORIES)if(ResearchView.category(state,c)){int cy=10+(++index)*24;if(x>=1&&x<17&&y>=cy&&y<cy+16){buttonSound();choose(c);return true;}}
            if(x>=1&&x<17&&y>=height-17&&y<height-1){searching=!searching;search.setVisible(searching);setFocused(searching?search:null);search.setFocused(searching);buttonSound();return true;}
            if(searching&&x>22&&x<width-14&&y>=32){int row=(int)(y-32)/10;if(row<results.size()&&y<40+row*10&&(row==0||32+(row+1)*10<=height-32)){var r=results.get(row);if(r.entry()==null)choose(r.category());else{remember();minecraft.setScreen(r.recipe()==null?new ResearchPageScreen(payload,r.entry().key()):new ResearchPageScreen(payload,r.entry().key(),r.recipe()));}return true;}}
            if(!searching&&hovered!=null){open(hovered);return true;}
        }return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){if(!searching&&button==0&&x>16&&y>16&&x<width-16&&y<height-16){motion.drag(-dx*zoom,-dy*zoom);clampMap();return true;}return super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseScrolled(double x,double y,double h,double v){if(!searching){zoom=Math.clamp(zoom-(float)Math.signum(v)*.25f,1,2);clampMap();return true;}return false;}
    @Override public void tick(){super.tick();motion.tick();}
    @Override public boolean keyPressed(int key,int scanCode,int modifiers){if(!searching&&minecraft.options.keyInventory.matches(key,scanCode)){onClose();return true;}return super.keyPressed(key,scanCode,modifiers);}
    @Override public void removed(){remember();}
    @Override public boolean isPauseScreen(){return false;}
}
