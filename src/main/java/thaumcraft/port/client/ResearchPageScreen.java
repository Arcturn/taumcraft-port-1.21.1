package thaumcraft.port.client;
import com.google.gson.JsonObject;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.network.*;
import thaumcraft.port.research.*;

/** Original two-column parchment and paper inserts; progress remains server-owned. */
public final class ResearchPageScreen extends Screen {
    private final ResearchCatalog.Entry entry;
    private JournalPayload payload;
    private ResearchState state;
    private JsonObject stage;
    private List<List<BookDocument.Placed>> pages=List.of();
    private List<net.minecraft.util.FormattedCharSequence> titleLines=List.of();
    private int titleExtra;
    private record LayoutKey(String text,int height,int lineHeight,int resourceEpoch,String language,boolean uniform){}
    private LayoutKey layoutKey;
    private List<String> recipes=List.of();
    private int page,insert,recipeVariant;
    private static int aspectPage;
    private int cycle=-1;
    private long nextCycle;
    private boolean pending,canAdvance;
    private int completeY;
    private Component tip;
    private String initialRecipe;
    private String shownRecipe;
    private final Deque<String> recipeHistory=new ArrayDeque<>();
    private final RecipeBook.BlueprintView blueprint=new RecipeBook.BlueprintView();
    public ResearchPageScreen(JournalPayload p,String key){super(Component.translatable(ResearchCatalog.get(key).title()));entry=ResearchCatalog.get(key);update(p);}
    public ResearchPageScreen(JournalPayload p,String key,String recipe){this(p,key);initialRecipe=recipe;}
    public void update(JournalPayload p){payload=p;state=ResearchCodec.read(p.progress());pending=false;if(minecraft!=null)parse();}
    @Override protected void init(){
        // Reload can expose the new model epoch before the replacement font is applied.
        // LoadingOverlay rebuilds this screen after all resources finish; reflow with that font.
        layoutKey=null;parse();if(initialRecipe!=null&&RecipeBook.available(initialRecipe,state)){insert=3;shownRecipe=initialRecipe;initialRecipe=null;}
    }
    private int left(){return (width-256)/2;}
    private int top(){return (height-181)/2;}
    private void parse(){
        if(entry.stages().isEmpty())return;stage=entry.stages().get(Math.clamp(state.stage(entry.key())-1,0,entry.stages().size()-1));
        titleLines=BookTypography.titleLines(font,title,140);titleExtra=Math.max(0,titleLines.size()-1)*font.lineHeight;
        String text=Component.translatable(stage.get("text").getAsString()).getString();LinkedHashSet<String> rec=new LinkedHashSet<>(ResearchCatalog.strings(stage,"recipes"));
        if(state.knows(entry.key())){int n=0;for(var a:entry.addenda())if(ResearchCatalog.strings(a,"required_research").stream().allMatch(state::knows)){
            if(a.has("text"))text+="<PAGE>"+Component.translatable("tc.addendumtext",++n).getString()+"<BR>"+Component.translatable(a.get("text").getAsString()).getString();rec.addAll(ResearchCatalog.strings(a,"recipes"));
        }}
        int requirements=0;if(!state.knows(entry.key()))for(String k:List.of("required_item","required_craft","required_research","required_knowledge"))if(!ResearchCatalog.strings(stage,k).isEmpty())requirements++;
        // Keep Minecraft 1.21's active font (including resource packs and accessibility settings).
        // A progress sync should not re-wrap unchanged text on the render thread.
        var nextLayout=new LayoutKey(text,BookDocument.firstPageHeight(entry.key().equals("KNOWLEDGETYPES"),knowledgeRows(),requirements)-titleExtra,font.lineHeight,BookAssets.resourceEpoch,minecraft.getLanguageManager().getSelected(),minecraft.options.forceUnicodeFont().get());
        if(!nextLayout.equals(layoutKey)){pages=BookDocument.paginate(BookDocument.parse(text,font::width,font.lineHeight),nextLayout.height(),font.lineHeight);layoutKey=nextLayout;}
        recipes=rec.stream().filter(key->RecipeBook.available(key,state)).toList();page=Math.min(page,Math.max(0,(pages.size()-1)/2*2));
        if(shownRecipe!=null){int count=RecipeBook.variants(shownRecipe,state);if(count==0){shownRecipe=null;insert=0;recipeHistory.clear();}else recipeVariant=Math.min(recipeVariant,count-1);}
    }
    @Override public void render(GuiGraphics g,int mx,int my,float partial){
        if(layoutKey==null||layoutKey.resourceEpoch()!=BookAssets.resourceEpoch||!layoutKey.language().equals(minecraft.getLanguageManager().getSelected())||layoutKey.uniform()!=minecraft.options.forceUnicodeFont().get())parse();
        renderTransparentBackground(g);tip=null;RecipeBook.beginFrame(state);
        long now=System.currentTimeMillis();if(now>nextCycle){cycle++;nextCycle=now+1000;blueprint.advanceCycle();}
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();int x=left(),y=top(),contentY=y-10;
        g.pose().pushPose();g.pose().translate((width-256*1.3f)/2,(height-181*1.3f)/2,0);g.pose().scale(1.3f,1.3f,1);BookAssets.sprite(g,BookAssets.BOOK,0,0,0,0,256,181);g.pose().popPose();
        for(int side=0;side<2;side++){int p=page+side;if(p>=pages.size())break;int py=y-16+(p==0?28+titleExtra:0),px=x-15+side*152;
            if(p==0){BookAssets.sprite(g,BookAssets.BOOK,x+4,y-17,24,184,96,4);BookAssets.sprite(g,BookAssets.BOOK,x+4,y+titleExtra,24,184,96,4);for(int row=0;row<titleLines.size();row++){var line=titleLines.get(row);g.drawString(font,line,x+55-font.width(line)/2,y-10+row*font.lineHeight,0x202020,false);}}
            for(var placed:pages.get(p)){var e=placed.element();if(e.image()!=null)BookAssets.image(g,e.image(),px+(140-e.image().displayWidth())/2,py+placed.y()+1);else if(e.text()!=null)g.drawString(font,e.text(),px,py+placed.y(),0,false);contentY=py+placed.y()+e.height()+6;}
        }
        if(page==0&&!state.knows(entry.key()))requirements(g,mx,my);
        if(state.knows("FIRSTSTEPS")){int le=inside(mx,my,x-48,y+9,25,16)?0:3;BookAssets.sprite(g,BookAssets.BOOK,x-48+le,y+9,76,232,24-le,16);BookAssets.sprite(g,BookAssets.BOOK,x-28,y+9,100,232,4,16);hover(mx,my,x-48,y+9,25,16,Component.translatable("tc.aspect.name"));}
        if(knowledgeTab()){int le=inside(mx,my,x-48,y+32,25,16)?0:3;BookAssets.sprite(g,BookAssets.BOOK,x-49+le,y+32,44,232,24-le,16);BookAssets.sprite(g,BookAssets.BOOK,x-29,y+32,100,232,4,16);hover(mx,my,x-48,y+32,25,16,Component.translatable("tc.knowledge.name"));}
        if(state.knows("KNOWLEDGETYPES")&&entry.key().equals("KNOWLEDGETYPES"))knowledge(g,mx,my,true);
        recipeBookmarks(g,mx,my,x,y);
        if(insert!=0){g.pose().pushPose();g.pose().translate(0,0,240);BookAssets.sprite(g,BookAssets.PAPER,(width-256)/2,(height-256)/2,0,0,255,255);if(insert==1)aspects(g,mx,my);else if(insert==2)knowledge(g,mx,my,false);else RecipeBook.render(g,shownRecipe,recipeVariant,state,width/2,height/2,mx,my,blueprint);g.pose().popPose();}
        if(insert!=3){if(page>0)arrow(g,x-16,y+190,0,184,12,8);if(page+2<pages.size())arrow(g,x+262,y+190,12,184,12,8);}
        if(!recipeHistory.isEmpty()){
            // Original genResearchBackground draws history navigation after every paper insert.
            g.pose().pushPose();g.pose().translate(0,0,260);arrow(g,x+118,y+190,38,202,20,12);
            if(inside(mx,my,x+118,y+190,20,12))g.drawString(font,Component.translatable("recipe.return"),mx,my,0xffffff,true);
            g.pose().popPose();
        }
        if(insert==0&&!state.knows(entry.key())&&stage!=null&&stage.has("warp")&&stage.get("warp").getAsInt()>0)forbidden(g,mx,my,x,knowledgeTab()?y+32:state.knows("FIRSTSTEPS")?y+9:contentY);
        if(!RecipeBook.renderTooltip(g,mx,my)&&tip!=null)BookTooltip.render(g,List.of(tip),mx,my+12,mx,width,height,11);
    }
    private boolean knowledgeTab(){return state.knows("KNOWLEDGETYPES")&&!entry.key().equals("KNOWLEDGETYPES");}
    private void recipeBookmarks(GuiGraphics g,int mx,int my,int x,int y){
        int currentStage=Math.clamp(state.stage(entry.key())-1,0,entry.stages().size()-1);
        var random=new Random(entry.key().hashCode()+currentStage*50);
        int spacing=Math.min(25,200/Math.max(1,recipes.size()));
        for(int i=0;i<recipes.size();i++){
            int ry=y-9+i*spacing,shift=random.nextInt(3);
            int length=random.nextInt(3)+(inside(mx,my,x+280,ry,30,16)?0:3);
            boolean selected=insert==3&&recipes.get(i).equals(shownRecipe);
            g.setColor(1,selected?.5f:1,selected?.5f:1,1);
            // TC6: the paper strip and its binding are two separate pieces of the atlas.
            BookAssets.sprite(g,BookAssets.BOOK,x+280+shift,ry,120+length,232,28,16);
            BookAssets.sprite(g,BookAssets.BOOK,x+280+shift,ry,116,232,4,16);
            g.setColor(1,1,1,1);
            int variant=Math.max(0,cycle);
            if(RecipeBook.bookmark(g,recipes.get(i),variant,state,x+287+shift-length,ry)&&inside(mx,my,x+287+shift-length,ry,16,16))RecipeBook.itemTooltip(RecipeBook.output(recipes.get(i),variant,state),false);
        }
    }
    private void forbidden(GuiGraphics g,int mx,int my,int x,int y){
        int warp=Math.min(5,stage.get("warp").getAsInt()),frame=160+Math.floorMod(minecraft.player.tickCount,32);
        BookAssets.prepareImage(g);com.mojang.blaze3d.systems.RenderSystem.blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE);
        g.setColor(.33f,0,.44f,.9f);g.pose().pushPose();g.pose().translate(x-57,y-40,0);g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90));
        g.blit(BookAssets.id("textures/misc/auranodes.png"),-45,-45,90,90,(frame%32)*16,(frame/32)*16,16,16,512,512);g.flush();g.pose().popPose();g.setColor(1,1,1,1);com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        String level=Component.translatable("tc.forbidden.level."+warp).getString();g.drawString(font,level,x-56-font.width(level)/2,y-43,0xaa99ff,false);
        hover(mx,my,x-67,y-50,20,20,Component.literal(Component.translatable("tc.warp.warn").getString().replace("%n",level)));
    }
    private void arrow(GuiGraphics g,int x,int y,int u,int v,int w,int h){float scale=1.1f+(float)Math.sin(minecraft.player.tickCount/3f)*.2f;g.pose().pushPose();g.pose().translate(x+w/2f,y+h/2f,0);g.pose().scale(scale,scale,1);BookAssets.sprite(g,BookAssets.BOOK,-w/2,-h/2,u,v,w,h);g.pose().popPose();}
    private void hover(int mx,int my,int x,int y,int w,int h,Component text){if(insert!=3&&inside(mx,my,x,y,w,h))tip=text;}
    private boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&my>=y&&mx<x+w&&my<y+h;}
    private void requirements(GuiGraphics g,int mx,int my){
        int x=left(),y=top()+194;canAdvance=state.available(entry)&&state.stage(entry.key())>0;int[] taken=new int[minecraft.player.getInventory().getContainerSize()];
        for(String field:List.of("required_research","required_item","required_craft","required_knowledge")){
            var values=ResearchCatalog.strings(stage,field);if(values.isEmpty())continue;y-=18;
            int uv=field.equals("required_research")?232:field.equals("required_item")?216:field.equals("required_craft")?200:184;
            g.setColor(1,1,1,.25f);BookAssets.sprite(g,BookAssets.BOOK,x-12,y-1,200,uv,56,16);g.setColor(1,1,1,1);
            hover(mx,my,x-15,y,20,16,Component.translatable("tc.need."+(field.equals("required_item")?"obtain":field.equals("required_research")?"research":field.equals("required_craft")?"craft":"know")));
            int step=Math.min(18,110/values.size());
            for(int i=0;i<values.size();i++){String value=values.get(i);int rx=x+9+i*step;boolean done=false;Component label;
                if(field.equals("required_knowledge")){
                    String[] p=value.split(";");int cost=Integer.parseInt(p[2]);done=state.knowledge(p[0],p[1])>=cost*ResearchState.progression(p[0]);BookAssets.texture(g,BookAssets.id("textures/research/knowledge_"+p[0].toLowerCase(Locale.ROOT)+".png"),rx,y,16);BookAssets.texture(g,BookAssets.category(p[1]),rx+2,y+2,12);
                    label=Component.translatable("tc.type."+p[0].toLowerCase(Locale.ROOT)).append(": ").append(Component.translatable("tc.research_category."+p[1]));
                    String amount=""+cost;float textScale=BookTypography.smallScale();g.pose().pushPose();g.pose().translate(rx+16-(int)Math.ceil(font.width(amount)*textScale),y+16-(int)Math.ceil(font.lineHeight*textScale),5);g.pose().scale(textScale,textScale,textScale);g.drawString(font,amount,0,0,done?0xffffff:0xff5555,true);g.pose().popPose();
                }else if(field.equals("required_research")){
                    done=state.knows(value);var e=ResearchCatalog.get(ResearchView.clean(value));var a=Aspect.byId(value.startsWith("!")?value.substring(1):"").orElse(null);
                    if(a!=null){aspect(g,a,rx,y,16,true);label=Component.literal(a.displayName());}
                    else{if(e!=null&&!e.icons().isEmpty())BookAssets.researchIcon(g,e,rx,y);
                        else if(value.startsWith("m_")||value.startsWith("c_")||value.startsWith("f_"))BookAssets.texture(g,BookAssets.id("textures/research/rd_"+(value.startsWith("m_")?"map":value.startsWith("c_")?"chest":"flask")+".png"),rx,y,16);
                        else{g.setColor(.5f,.75f,1,1);BookAssets.texture(g,BookAssets.UNKNOWN,rx,y,16);g.setColor(1,1,1,1);}
                        label=Component.translatable(e==null?"research."+value+".text":e.title());}
                }else{
                    var req=LegacyRequirement.parse(value);RecipeBook.ingredient(g,value,rx,y,insert==0?mx:-5000,insert==0?my:-5000,i);label=null;
                    if(field.equals("required_craft"))done=state.crafted().contains(req.craftKey());
                    else {int needed=req.count();for(int slot=0;slot<taken.length&&needed>0;slot++){var s=minecraft.player.getInventory().getItem(slot);if(req.matches().test(s)){int n=Math.min(needed,s.getCount()-taken[slot]);taken[slot]+=n;needed-=n;}}done=needed==0;}
                }
                canAdvance&=done;if(done)BookAssets.sprite(g,BookAssets.BOOK,rx+8,y,159,207,10,10);if(label!=null&&insert!=3)hover(mx,my,rx,y,16,16,label);
            }
        }
        completeY=y-18;
        if(y!=top()+194){BookAssets.sprite(g,BookAssets.BOOK,x+4,y-14,24,184,96,8);
            if(canAdvance){
                if(!pending){boolean hovered=inside(mx,my,x+20,completeY,64,12);g.setColor(hovered?1:.8f,hovered?1:.8f,hovered?1:.9f,1);BookAssets.sprite(g,BookAssets.BOOK,x+20,completeY,84,216,64,12);g.setColor(1,1,1,1);}
                var text=Component.translatable(pending?"tc.stage.hold":"tc.stage.complete");g.drawString(font,text,x+52-font.width(text)/2,completeY+2,0xffffff,true);
            }
        }
    }
    static void aspect(GuiGraphics g,Aspect a,int x,int y,int size,boolean known){int c=known?a.color():0xffffff;g.setColor(((c>>16)&255)/255f,((c>>8)&255)/255f,(c&255)/255f,1);BookAssets.texture(g,known?BookAssets.id("textures/aspects/"+a.id()+".png"):BookAssets.UNKNOWN,x,y,size);g.setColor(1,1,1,1);}
    private void smallLabel(GuiGraphics g,String text,int x,int y){float scale=BookTypography.smallScale();g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(scale,scale,1);g.drawString(font,text,-font.width(text)/2,0,0x505050,false);g.pose().popPose();}
    private void aspects(GuiGraphics g,int mx,int my){
        int x=width/2-68,y=height/2-104;var list=Arrays.stream(Aspect.values()).filter(a->state.discovered().contains(a.id())).sorted(Comparator.comparing(Aspect::displayName)).toList();
        for(int i=0;i<5;i++){int index=aspectPage*5+i;if(index>=list.size())break;var a=list.get(index);int ay=y+i*40;
            if(inside(mx,my,x,ay,40,40)){g.setColor(1,1,1,.5f);BookAssets.texture(g,BookAssets.id("textures/aspects/_back.png"),x-2,ay-2,32);g.setColor(1,1,1,1);}
            aspect(g,a,x+2,ay+2,24,true);smallLabel(g,a.displayName(),x+16,ay+29);hover(mx,my,x,ay,40,40,Component.literal(a.displayName()).append(": ").append(Component.translatable("tc.aspect."+a.id())));
            if(a.isPrimal())g.drawString(font,Component.translatable("tc.aspect.primal"),x+54,ay+12,0x777777,false);
            else{
                var components=a.components();
                int firstWidth=state.discovered().contains(components.get(0).id())?font.width(components.get(0).displayName()):0;
                int secondWidth=state.discovered().contains(components.get(1).id())?font.width(components.get(1).displayName()):0;float scale=BookTypography.smallScale();
                int spacing=BookTypography.componentSpacing(firstWidth,secondWidth,scale),shift=BookTypography.componentShift(secondWidth,spacing,scale);
                g.drawString(font,"=",x+41,ay+12,0x999999,false);g.drawString(font,"+",x+89+(spacing-42)/2-shift,ay+12,0x999999,false);
                for(int j=0;j<2;j++){var c=components.get(j);boolean known=state.discovered().contains(c.id());if(known)aspect(g,c,x+60+j*spacing-shift,ay+4,20,true);else{g.setColor(.8f,.8f,.8f,1);BookAssets.texture(g,BookAssets.UNKNOWN,x+60+j*spacing-shift,ay+4,20);g.setColor(1,1,1,1);}if(known)smallLabel(g,c.displayName(),x+72+j*spacing-shift,ay+29);}
            }
        }
        if(aspectPage>0)arrow(g,width/2-88,height/2+104,0,184,12,8);if((aspectPage+1)*5<list.size())arrow(g,width/2+76,height/2+104,12,184,12,8);
    }
    private int knowledgeRows(){int rows=0;for(String type:List.of("OBSERVATION","THEORY"))if(BookAssets.CATEGORIES.stream().anyMatch(category->state.knowledge(type,category)>0))rows++;return rows;}
    private void knowledge(GuiGraphics g,int mx,int my,boolean inPage){
        int x=inPage?left():left()+60,y=top()+(inPage?176:57),row=0;
        for(String t:List.of("OBSERVATION","THEORY")){int col=0;for(String c:BookAssets.CATEGORIES){int raw=state.knowledge(t,c),unit=ResearchState.progression(t);if(raw<=0)continue;int cx=x-10+(inPage?18:164/BookAssets.CATEGORIES.size())*col++,cy=y-row*(inPage?20:28);
            BookAssets.texture(g,BookAssets.id("textures/research/knowledge_"+t.toLowerCase(Locale.ROOT)+".png"),cx,cy,16);g.setColor(1,1,1,.75f);BookAssets.texture(g,BookAssets.category(c),cx+3,cy+3,11);g.setColor(1,1,1,1);
            String amount=""+(raw/unit);g.drawString(font,amount,cx+16-font.width(amount),cy+8,0xffffff,true);int n=16*(raw%unit)/unit;
            if(raw%unit>0){g.setColor(1,1,1,.75f);BookAssets.sprite(g,BookAssets.BOOK,cx,cy+17,0,232,n,2);BookAssets.sprite(g,BookAssets.BOOK,cx+n,cy+17,n,234,16-n,2);g.setColor(1,1,1,1);}
            hover(mx,my,cx,cy,16,19,Component.translatable("tc.type."+t.toLowerCase(Locale.ROOT)).append(": ").append(Component.translatable("tc.research_category."+c)));
        }if(col>0)row++;}
        if(inPage&&row>0)BookAssets.sprite(g,BookAssets.BOOK,x+4,y-row*20+12,24,184,96,8);
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button!=0)return false;int x=left(),y=top();
        if(state.knows("FIRSTSTEPS")&&inside(mx,my,x-48,y+8,25,16)){insert=insert==1?0:1;shownRecipe=null;recipeHistory.clear();blueprint.reset();if(aspectPage>=(state.discovered().size()+4)/5)aspectPage=0;ResearchScreen.sound("page",.7f,.9f);return true;}
        if(knowledgeTab()&&inside(mx,my,x-48,y+31,25,16)){insert=insert==2?0:2;shownRecipe=null;recipeHistory.clear();blueprint.reset();ResearchScreen.sound("page",.7f,.9f);return true;}
        int spacing=Math.min(25,200/Math.max(1,recipes.size()));for(int i=0;i<recipes.size();i++)if(inside(mx,my,x+280,y-8+i*spacing,30,16)){insert=insert==3&&recipes.get(i).equals(shownRecipe)?0:3;shownRecipe=insert==3?recipes.get(i):null;if(shownRecipe!=null)recipeVariant=Math.min(recipeVariant,RecipeBook.variants(shownRecipe,state)-1);recipeHistory.clear();blueprint.reset();ResearchScreen.sound("page",.7f,.9f);return true;}
        if(inside(mx,my,x+118,y+190,20,12)){
            previousRecipe();return true;
        }
        if(insert==3){
            if(blueprint.click(shownRecipe,recipeVariant,state,mx,my,x,y))return true;
            if(recipeVariant>0&&inside(mx,my,x+38,y+192,14,14)){recipeVariant--;ResearchScreen.sound("page",.7f,.9f);return true;}
            if(recipeVariant+1<RecipeBook.variants(shownRecipe,state)&&inside(mx,my,x+205,y+192,14,14)){recipeVariant++;ResearchScreen.sound("page",.7f,.9f);return true;}
        }
        if((insert==0||insert==3)&&RecipeBook.hoveredIngredient(mx,my)){var reference=RecipeBook.reference(RecipeBook.hoveredItem,state);if(reference!=null&&!reference.locked()){
            if(shownRecipe!=null)recipeHistory.push(shownRecipe);shownRecipe=reference.recipe();recipeVariant=reference.variant();insert=3;blueprint.reset();ResearchScreen.sound("page",.66f,1);return true;
        }}
        if(insert==1){int count=state.discovered().size();boolean changed=false;if(inside(mx,my,x+38,y+192,14,14)&&aspectPage>0){aspectPage--;changed=true;}else if(inside(mx,my,x+205,y+192,14,14)&&(aspectPage+1)*5<count){aspectPage++;changed=true;}if(changed){ResearchScreen.sound("page",.7f,.9f);return true;}}
        if(insert!=3){if(inside(mx,my,x-17,y+189,14,10)&&page>0){turnPage(-2);return true;}if(inside(mx,my,x+261,y+189,14,10)&&page+2<pages.size()){turnPage(2);return true;}}
        if(insert==0&&page==0&&canAdvance&&!pending&&!state.knows(entry.key())&&inside(mx,my,x+20,completeY,64,12)){pending=true;PacketDistributor.sendToServer(new ResearchActionPayload(entry.key(),state.stage(entry.key())));ResearchScreen.sound("write",.66f,1);return true;}return false;
    }
    private void previousRecipe(){if(!recipeHistory.isEmpty()){shownRecipe=recipeHistory.pop();recipeVariant=Math.max(0,Math.min(recipeVariant,RecipeBook.variants(shownRecipe,state)-1));blueprint.reset();ResearchScreen.sound("page",.66f,1);}else if(insert==3){shownRecipe=null;insert=0;}}
    private void turnPage(int delta){page+=delta;cycle=-1;nextCycle=0;ResearchScreen.sound("page",.66f,1);}
    private void back(){minecraft.setScreen(new ResearchScreen(payload));}
    @Override public boolean keyPressed(int key,int scan,int mod){
        if(key==256||minecraft.options.keyInventory.matches(key,scan)){recipeHistory.clear();shownRecipe=null;if(insert!=0){insert=0;blueprint.reset();ResearchScreen.sound("page",.4f,1.1f);}else back();return true;}
        if(key==259){previousRecipe();return true;}
        if(key==263||key==265||key==266){if(page>0)turnPage(-2);return true;}
        if(key==262||key==264||key==267){if(page+2<pages.size())turnPage(2);return true;}
        return super.keyPressed(key,scan,mod);
    }
    @Override public boolean isPauseScreen(){return false;}
}
