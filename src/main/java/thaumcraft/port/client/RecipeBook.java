package thaumcraft.port.client;
import com.google.gson.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.research.BookRecipeCatalog;
import thaumcraft.port.research.BookIngredients;
import thaumcraft.port.research.ResearchState;
/** Recipe inserts use catalog data, never invented ingredients or client-side crafting. */
public final class RecipeBook {
    public static String hoveredItem;
    private static int hoveredX,hoveredY;
    private static List<Component> tooltip=List.of();
    private static ResearchState renderedState;
    private static final Map<String,Optional<BookRecipeCatalog.Reference>> REFERENCES=new HashMap<>();
    public static final class BlueprintView {
        private static boolean cycle;
        private int slice;
        private String key="";
        private int slice(String recipe,int height){
            if(!key.equals(recipe)){key=recipe;slice=0;}
            if(slice>height)slice=0;
            return slice;
        }
        public void reset(){key="";slice=0;}
        public void advanceCycle(){if(cycle&&!key.isEmpty())slice++;}
        public boolean click(String recipe,int variant,ResearchState state,double mx,double my,int bookLeft,int bookTop){
            var all=BookRecipeCatalog.recipes(recipe,state);if(all.isEmpty()||!all.get(Math.floorMod(variant,all.size())).get("type").getAsString().contains("blueprint"))return false;
            if(mx>=bookLeft+210&&mx<bookLeft+220&&my>=bookTop+190&&my<bookTop+200){cycle=!cycle;ResearchScreen.sound("clack",.66f,1);return true;}return false;
        }
    }
    public static void beginFrame(ResearchState state){
        if(renderedState!=state){renderedState=state;REFERENCES.clear();}
        hoveredItem=null;tooltip=List.of();
    }
    public static boolean renderTooltip(GuiGraphics g,int mx,int my){
        if(tooltip.isEmpty())return false;
        var window=Minecraft.getInstance().getWindow();BookTooltip.render(g,tooltip,mx,my+12,mx,window.getGuiScaledWidth(),window.getGuiScaledHeight(),11);return true;
    }
    public static boolean hoveredIngredient(double mx,double my){return hoveredItem!=null&&mx>=hoveredX&&mx<hoveredX+16&&my>=hoveredY&&my<hoveredY+16;}
    public static void ingredient(GuiGraphics g,String source,int x,int y,int mx,int my){ingredient(g,source,x,y,mx,my,0);}
    public static void ingredient(GuiGraphics g,String source,int x,int y,int mx,int my,int slot){draw(g,new JsonPrimitive(source),x,y,mx,my,true,slot);}
    public static void itemTooltip(String source,boolean clickthrough){
        hoveredItem=null;
        var lines=new ArrayList<Component>(BookAssets.tooltip(source));
        if(clickthrough){
            var reference=REFERENCES.computeIfAbsent(source,s->Optional.ofNullable(reference(s,renderedState))).orElse(null);
            if(reference!=null){
                lines.add(Component.translatable(reference.locked()?"recipe.unknown":"recipe.clickthrough").withStyle(
                        reference.locked()?net.minecraft.ChatFormatting.DARK_RED:net.minecraft.ChatFormatting.BLUE,net.minecraft.ChatFormatting.ITALIC));
                if(!reference.locked())hoveredItem=source;
            }
        }
        tooltip=lines;
    }
    public static int variants(String key,ResearchState state){return BookRecipeCatalog.recipes(key,state).size();}
    public static boolean available(String key){return !BookRecipeCatalog.recipes(key).isEmpty();}
    public static boolean available(String key,ResearchState state){return variants(key,state)>0;}
    public static String output(String key,int variant){return output(BookRecipeCatalog.recipes(key),variant);}
    public static String output(String key,int variant,ResearchState state){return output(BookRecipeCatalog.recipes(key,state),variant);}
    private static String output(List<JsonObject> all,int variant){return all.isEmpty()?"thaumcraft:textures/aspects/_unknown.png":BookRecipeCatalog.output(all.get(Math.floorMod(variant,all.size())));}
    /** BETA26 uses the completed construction on tabs unless BluePrint supplies a display stack. */
    public static boolean bookmark(GuiGraphics g,String key,int variant,ResearchState state,int x,int y){
        var all=BookRecipeCatalog.recipes(key,state);if(!all.isEmpty()){
            var recipe=all.get(Math.floorMod(variant,all.size()));
            if(recipe.has("target_layers")&&!recipe.get("display_stack").getAsBoolean()){
                var layers=recipe.getAsJsonArray("target_layers");
                blueprint(g,layers,x+8,y+7+layers.size(),4,0,true);return false;
            }
        }
        BookAssets.icon(g,output(all,variant),x,y,false);return true;
    }
    private static String item(JsonElement value){
        return item(value,0);
    }
    private static String item(JsonElement value,int slot){
        if(value==null)return "minecraft:air";
        long time=System.currentTimeMillis();
        if(value.isJsonPrimitive())return BookIngredients.resolve(value.getAsString(),slot,time,true);
        if(value.isJsonArray()){var array=value.getAsJsonArray();return array.isEmpty()?"minecraft:air":item(array.get(BookIngredients.index(array.size(),slot,time)),slot);}
        var o=value.getAsJsonObject();if(o.has("legacy"))return BookIngredients.resolve(o.get("legacy").getAsString(),slot,time,true);if(o.has("item")||o.has("id"))return o.get(o.has("item")?"item":"id").getAsString()+(o.has("count")?";"+o.get("count").getAsInt():"");
        if(o.has("tag")){var id=net.minecraft.resources.ResourceLocation.tryParse(o.get("tag").getAsString());if(id!=null){var tag=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,id);var options=net.minecraft.core.registries.BuiltInRegistries.ITEM.getTag(tag).map(t->t.stream().toList()).orElse(List.of());if(!options.isEmpty())return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(options.get(BookIngredients.index(options.size(),slot,time)).value()).toString();}}
        return "thaumcraft:textures/aspects/_unknown.png";
    }
    public static void render(GuiGraphics g,String key,int variant,ResearchState state,int cx,int cy,int mx,int my,BlueprintView blueprint){
        var mc=Minecraft.getInstance();var all=BookRecipeCatalog.recipes(key,state);if(all.isEmpty()){g.drawWordWrap(mc.font,Component.translatable("thaumcraft.port.recipe.unavailable"),cx-85,cy-15,170,0x403020);return;}
        var r=all.get(Math.floorMod(variant,all.size()));String type=r.has("type")?r.get("type").getAsString():"";
        String label=type.contains("blueprint")?"recipe.type.construct":type.contains("crucible")?"recipe.type.crucible":type.contains("infusion")?"recipe.type.infusion":type.contains("arcane")?(r.has("pattern")?"recipe.type.arcane":"recipe.type.arcane.shapeless"):r.has("pattern")?"recipe.type.workbench":"recipe.type.workbenchshapeless";
        var labelLines=mc.font.split(Component.translatable(label),186);
        // Keep the last header line above the original output frame; native RU shapeless
        // labels are wider than the paper and need one additional line toward its top.
        for(int line=0;line<labelLines.size();line++){
            var text=labelLines.get(line);
            g.drawString(mc.font,text,cx-mc.font.width(text)/2,cy-104-(labelLines.size()-1-line)*mc.font.lineHeight,0x505050,false);
        }
        if(type.contains("blueprint")){
            var layers=r.getAsJsonArray("layers");int ys=layers.size(),xs=layers.get(0).getAsJsonArray().size(),zs=layers.get(0).getAsJsonArray().get(0).getAsJsonArray().size();
            int slice=blueprint.slice(key+":"+variant,ys);float scale=38-Math.max(Math.max(xs,zs),ys)*2;
            blueprint(g,layers,cx,cy,scale,slice,false);
            var ingredients=r.getAsJsonArray("ingredients");for(int i=0;i<ingredients.size();i++)draw(g,ingredients.get(i),cx-85+i*17,cy+90,mx,my,true,i);
            g.setColor(1,1,1,mx>=cx+80&&mx<cx+88&&my>=cy+100&&my<cy+108?1:.75f);BookAssets.sprite(g,BookAssets.BOOK,cx+80,cy+100,BlueprintView.cycle?168:160,224,8,8);g.setColor(1,1,1,1);
        }else if(type.contains("crucible")){
            overlay(g,cx,cy,-28,-29,0,3,56,17);overlay(g,cx,cy,-28,-12,0,20,56,48);overlay(g,cx,cy,-25,-26,100,84,11,13);
            draw(g,r.get("result"),cx-8,cy-50,mx,my,false);draw(g,r.get("catalyst"),cx-64,cy-56,mx,my);tags(g,r,cx,cy+8,3,mx,my);
        }else if(type.contains("infusion")){
            overlay(g,cx,cy,-28,-46,0,3,56,17);overlay(g,cx,cy,-28,-26,200,77,60,44);
            draw(g,r.get("result"),cx-8,cy-85,mx,my,false);draw(g,r.get("input"),cx-8,cy-16,mx,my);
            var ingredients=r.getAsJsonArray("ingredients");if(ingredients!=null)for(int i=0;i<ingredients.size();i++){double angle=Math.toRadians(-90+(360/ingredients.size())*i);draw(g,ingredients.get(i),cx-8+(int)(Math.cos(angle)*40),cy-16+(int)(Math.sin(angle)*40),mx,my,true,i);}tags(g,r,cx,cy+50,5,mx,my);
            if(r.has("instability")){var text=Component.translatable("tc.inst").append(" ").append(Component.translatable("tc.inst."+Math.min(5,r.get("instability").getAsInt()/2)));g.drawString(mc.font,text,cx-mc.font.width(text)/2,cy+94,0x505050,false);}
        }else{
            boolean arcane=type.contains("arcane");overlay(g,cx,cy,-26,-26,arcane?112:60,15,arcane?52:51,52);overlay(g,cx,cy,-8,-46,20,3,16,16);
            draw(g,r.get("result"),cx-8,cy-84,mx,my,false);
            if(r.has("pattern")){var pattern=r.getAsJsonArray("pattern");var keys=r.getAsJsonObject("key");for(int row=0;row<pattern.size();row++){String line=pattern.get(row).getAsString();for(int col=0;col<line.length();col++)if(line.charAt(col)!=' ')draw(g,keys.get(""+line.charAt(col)),cx-40+col*32,cy-40+row*32,mx,my,true,col+row*line.length());}}
            else if(r.has("ingredients")){var ingredients=r.getAsJsonArray("ingredients");for(int i=0;i<ingredients.size()&&i<9;i++)draw(g,ingredients.get(i),cx-40+i%3*32,cy-40+i/3*32,mx,my,true,i);}
            else if(r.has("ingredient"))draw(g,r.get("ingredient"),cx-8,cy-8,mx,my);
            if(r.has("vis")){g.setColor(1,1,1,.4f);overlay(g,cx,cy,-6,40,68,76,12,12);g.setColor(1,1,1,1);String v=r.get("vis").getAsString();g.drawString(mc.font,v,cx-mc.font.width(v)/2,cy+90,0x505050,false);if(mx>=cx-mc.font.width(v)/2-15&&mx<cx-mc.font.width(v)/2+15&&my>=cy+75&&my<cy+105)tooltip=List.of(Component.translatable("wandtable.text1"));}
            if(r.has("crystals")){var crystals=r.getAsJsonObject("crystals");int i=0;for(var e:crystals.entrySet()){String crystal="thaumcraft:crystal_essence;"+e.getValue().getAsInt()+";0;{Aspects:[{key:\""+e.getKey()+"\",amount:1}]}";ingredient(g,crystal,cx+4-crystals.size()*10+i++*20,cy+59,mx,my);}}
        }
        if(mx>=cx-8&&mx<cx+8&&my>=cy-85&&my<cy-69&&(r.has("runic_after")||r.has("enchantment"))){
            itemTooltip(item(r.get("result")),false);var lines=new ArrayList<Component>(tooltip);
            if(r.has("runic_after"))lines.add(1,Component.translatable("item.runic.charge").append(" +"+r.get("runic_after").getAsInt()).withStyle(net.minecraft.ChatFormatting.GOLD));
            if(r.has("enchantment")){String enchantment=r.get("enchantment").getAsString();var line=Component.translatable("enchantment.infusion."+enchantment);if(Set.of("sounding","refining","arcing","essence","visbattery","swift").contains(enchantment.toLowerCase(Locale.ROOT)))line.append(" I");lines.add(1,line.withStyle(net.minecraft.ChatFormatting.GOLD));}
            tooltip=lines;
        }
        if(mx>=cx-8&&mx<cx+8&&my>=cy-16&&my<cy&&r.has("runic_before")&&r.get("runic_before").getAsInt()>0){
            var lines=new ArrayList<Component>(tooltip);lines.add(1,Component.translatable("item.runic.charge").append(" +"+r.get("runic_before").getAsInt()).withStyle(net.minecraft.ChatFormatting.GOLD));tooltip=lines;
        }
        if(all.size()>1){float bob=1.1f+(float)Math.sin(mc.player.tickCount/3f)*.2f;if(variant>0)variantArrow(g,cx-88,cy+104,0,bob);if(variant<all.size()-1)variantArrow(g,cx+76,cy+104,12,bob);}
    }
    private static void blueprint(GuiGraphics g,JsonArray layers,int x,int y,float scale,int slice,boolean target){
        int ys=layers.size(),xs=layers.get(0).getAsJsonArray().size(),zs=layers.get(0).getAsJsonArray().get(0).getAsJsonArray().size();
        var cells=new LinkedHashMap<net.minecraft.core.BlockPos,String>();
        for(int h=0;h<ys-slice;h++){
            var layer=layers.get(ys-h-1).getAsJsonArray();
            for(int bx=0;bx<xs;bx++)for(int bz=0;bz<zs;bz++){
                // BlueprintBlockAccess(target=true) rotates each source layer right three times.
                String block=layer.get(target?bz:bx).getAsJsonArray().get(target?xs-bx-1:bz).getAsString();
                if(block.equals("minecraft:air"))continue;
                cells.put(new net.minecraft.core.BlockPos(bx,h,bz),block);
            }
        }
        var view=BookBlocks.view(cells);
        g.pose().pushPose();g.pose().translate(x-xs/2,y-Math.sqrt(ys*ys+xs*xs+zs*zs)/2,Math.max(ys,Math.max(xs,zs)));g.pose().scale(scale,-scale,1);g.pose().mulPose(com.mojang.math.Axis.XP.rotationDegrees(25));g.pose().mulPose(com.mojang.math.Axis.YP.rotationDegrees(-45));
        for(var cell:cells.entrySet()){
            var pos=cell.getKey();g.pose().pushPose();g.pose().translate(pos.getX()+.5f-zs/2f,pos.getY()+.5f-ys/2f,pos.getZ()+.5f-xs/2f);BookAssets.block(g,cell.getValue(),view,pos);g.pose().popPose();
        }
        g.pose().popPose();
    }
    private static void variantArrow(GuiGraphics g,int x,int y,int u,float scale){g.pose().pushPose();g.pose().translate(x+6,y+4,0);g.pose().scale(scale,scale,1);BookAssets.sprite(g,BookAssets.BOOK,-6,-4,u,184,12,8);g.pose().popPose();}
    private static void overlay(GuiGraphics g,int x,int y,int dx,int dy,int u,int v,int w,int h){g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(2,2,1);BookAssets.sprite(g,BookAssets.OVERLAY,dx,dy,u,v,w,h);g.pose().popPose();}
    private static void tags(GuiGraphics g,JsonObject r,int cx,int cy,int cols,int mx,int my){
        if(!r.has("aspects"))return;var as=r.getAsJsonObject("aspects");var keys=as.keySet().stream().sorted().toList();int rows=(keys.size()-1)/cols,shift=(cols-keys.size()%cols)*10;
        for(int i=0;i<keys.size();i++){var a=Aspect.byId(keys.get(i)).orElse(null);if(a==null)continue;int x=cx-(cols*10-2)+i%cols*20+(i/cols>=rows&&(rows>1||keys.size()<cols)?shift:0),y=cy-10*rows+i/cols*20;ResearchPageScreen.aspect(g,a,x,y,16,true);aspectAmount(g,as.get(keys.get(i)).getAsString(),x,y);if(mx>=x&&mx<x+16&&my>=y&&my<y+16)tooltip=List.of(Component.literal(a.displayName()),Component.translatable("tc.aspect."+a.id()));}
    }
    private static void aspectAmount(GuiGraphics g,String amount,int x,int y){
        var font=Minecraft.getInstance().font;float scale=BookTypography.smallScale();g.pose().pushPose();g.pose().translate(x,y,1);g.pose().scale(scale,scale,scale);
        int tx=Math.round(16/scale)-font.width(amount),ty=Math.round(16/scale)-font.lineHeight;
        g.drawString(font,amount,tx-1,ty,0,false);g.drawString(font,amount,tx+1,ty,0,false);g.drawString(font,amount,tx,ty-1,0,false);g.drawString(font,amount,tx,ty+1,0,false);
        g.drawString(font,amount,tx,ty,0xffffff,false);g.pose().popPose();
    }
    private static void draw(GuiGraphics g,JsonElement ingredient,int x,int y,int mx,int my){draw(g,ingredient,x,y,mx,my,true);}
    private static void draw(GuiGraphics g,JsonElement ingredient,int x,int y,int mx,int my,boolean clickthrough){
        draw(g,ingredient,x,y,mx,my,clickthrough,0);
    }
    private static void draw(GuiGraphics g,JsonElement ingredient,int x,int y,int mx,int my,boolean clickthrough,int slot){
        String source=item(ingredient,slot);if(source.equals("minecraft:air"))return;BookAssets.icon(g,source,x,y);
        if(mx>=x&&mx<x+16&&my>=y&&my<y+16){itemTooltip(source,clickthrough);if(hoveredItem!=null){hoveredX=x;hoveredY=y;}}
    }
    public static BookRecipeCatalog.Reference reference(String source,ResearchState state){return BookRecipeCatalog.reference(source,state,RecipeBook::signature);}
    private static String signature(String source){return BookRecipeCatalog.itemIdentity(BookAssets.ingredient(source));}
    private RecipeBook(){}
}
