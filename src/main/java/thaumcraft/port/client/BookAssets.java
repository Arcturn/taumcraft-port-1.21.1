package thaumcraft.port.client;

import com.google.gson.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import thaumcraft.port.research.*;

/** Original art remains at its original paths; registered items retain their real GUI models. */
public final class BookAssets {
    public static final ResourceLocation BROWSER=id("textures/gui/gui_research_browser.png"),BOOK=id("textures/gui/gui_researchbook.png"),PAPER=id("textures/gui/paper.png"),OVERLAY=id("textures/gui/gui_researchbook_overlay.png"),UNKNOWN=id("textures/aspects/_unknown.png");
    public static final List<String> CATEGORIES=List.of("BASICS","AUROMANCY","ALCHEMY","ARTIFICE","INFUSION","GOLEMANCY","ELDRITCH");
    private static final Map<String,Integer> BACKS=Map.of("BASICS",1,"AUROMANCY",2,"ALCHEMY",3,"ARTIFICE",4,"INFUSION",7,"GOLEMANCY",5,"ELDRITCH",6);
    private static final JsonObject LEGACY=ResearchCatalog.read("/thaumcraft-port/legacy-book-models.json");
    private static final JsonObject MODELS=ResearchCatalog.read("/thaumcraft-port/book-model-index.json");
    private static final JsonObject ADVANCEMENT_ICONS=ResearchCatalog.read("/thaumcraft-port/advancement-icon-index.json");
    private static final JsonObject DISPLAY_ICONS=ResearchCatalog.read("/thaumcraft-port/research-display-icons.json");
    public static int advancementColor(ItemStack stack,int tint){
        var dyed=stack.get(net.minecraft.core.component.DataComponents.DYED_COLOR);
        if(tint==0&&dyed!=null)return 0xff000000|dyed.rgb();
        String key=Integer.toString(stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,net.minecraft.world.item.component.CustomModelData.DEFAULT).value());
        var entry=ADVANCEMENT_ICONS.getAsJsonObject(key);
        return tint==0&&entry!=null&&entry.has("tint")?0xff000000|entry.get("tint").getAsInt():0xffffffff;
    }
    public static void registerModels(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional e){
        DIMENSIONS.clear();resourceEpoch++;
        BookBlocks.registerModels(e);
        MODELS.entrySet().forEach(m->e.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(ResourceLocation.parse(m.getValue().getAsString()))));
        DISPLAY_ICONS.entrySet().forEach(m->m.getValue().getAsJsonObject().getAsJsonArray("variants").forEach(v->e.register(net.minecraft.client.resources.model.ModelResourceLocation.standalone(ResourceLocation.parse(v.getAsJsonObject().get("model").getAsString())))));
    }
    public static String ingredient(String source){
        if(source.equals("thaumcraft:leather"))return "minecraft:leather";
        return BookIngredients.resolve(source,0,System.currentTimeMillis(),false);
    }
    private static final Map<String,String> VARIANTS=Map.of("ingot","thaumium,void,brass","plate","brass,iron,thaumium,void","mind","clockwork,biothaumic","nugget","iron,copper,tin,silver,lead,quicksilver,thaumium,void,brass,quartz,rareearth","cluster","iron,gold,copper,tin,silver,lead,cinnabar,quartz","thaumonomicon","normal,cheat");
    private static final Map<ResourceLocation,int[]> DIMENSIONS=new HashMap<>();
    static int resourceEpoch;
    public static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("thaumcraft",path);}
    public static ResourceLocation background(String category){return id("textures/gui/gui_research_back_"+BACKS.getOrDefault(category,1)+".png");}
    public static ResourceLocation category(String category){return id(category.equals("BASICS")?"textures/items/thaumonomicon_cheat.png":"textures/research/cat_"+category.toLowerCase(Locale.ROOT)+".png");}
    public static boolean has(ResourceLocation id){return Minecraft.getInstance().getResourceManager().getResource(id).isPresent();}
    /** Item/font render types clear blend state when flushed. Restore it at each image boundary. */
    public static void prepareImage(GuiGraphics g){g.flush();com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();}
    public static void sprite(GuiGraphics g,ResourceLocation texture,int x,int y,int u,int v,int w,int h){prepareImage(g);g.blit(texture,x,y,u,v,w,h,256,256);}
    public static void texture(GuiGraphics g,ResourceLocation texture,int x,int y,int size){
        texture(g,texture,x,y,size,true);
    }
    private static void texture(GuiGraphics g,ResourceLocation texture,int x,int y,int size,boolean animate){
        var d=DIMENSIONS.computeIfAbsent(texture,t->{try(var in=Minecraft.getInstance().getResourceManager().open(t);var img=com.mojang.blaze3d.platform.NativeImage.read(in)){return new int[]{img.getWidth(),img.getHeight()};}catch(java.io.IOException e){return new int[]{16,16};}});
        int w=d[0],h=d[1],u=0,v=0,sw=w,sh=h;
        if(animate&&h>w&&h%w==0){sh=w;v=(int)(System.currentTimeMillis()/150%(h/w))*w;}
        else if(animate&&w>h&&w%h==0){sw=h;u=(int)(System.currentTimeMillis()/150%(w/h))*h;}
        prepareImage(g);g.blit(texture,x,y,size,size,u,v,sw,sh,w,h);
    }
    public static void image(GuiGraphics g,BookDocument.Image i,int x,int y){g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale(i.scale(),i.scale(),1);sprite(g,ResourceLocation.parse(i.texture()),0,0,i.u(),i.v(),i.width(),i.height());g.pose().popPose();}
    public static ItemStack stack(String source){
        source=ingredient(source).replace(";32767",";0");
        // Presentation conversion is separate from conservative server-side requirement matching.
        if(source.startsWith("thaumcraft:label;")){
            var parts=source.split(";",4);
            if(parts.length==4&&parts[2].equals("1")){
                var match=java.util.regex.Pattern.compile("key:['\"]([a-z]+)['\"]").matcher(parts[3]);
                if(match.find()){
                    var aspect=thaumcraft.port.aspect.Aspect.byId(match.group(1)).orElse(null);
                    if(aspect!=null){var label=new ItemStack(thaumcraft.port.essentia.EssentiaContent.LABEL.get(),Integer.parseInt(parts[1]));label.set(thaumcraft.port.essentia.EssentiaContent.ASPECT.get(),aspect.id());return label;}
                }
            }
        }
        if(source.contains("enchanted_placeholder")){var book=new ItemStack(Items.ENCHANTED_BOOK);book.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);return book;}
        if(source.startsWith("minecraft:potion;")&&source.contains("Potion:")){
            var match=java.util.regex.Pattern.compile("Potion:\"([^\"]+)\"").matcher(source);if(match.find()){var holder=BuiltInRegistries.POTION.getHolder(ResourceLocation.parse(match.group(1)));if(holder.isPresent()){var potion=new ItemStack(Items.POTION);potion.set(net.minecraft.core.component.DataComponents.POTION_CONTENTS,new net.minecraft.world.item.alchemy.PotionContents(holder.get()));return potion;}}
        }
        var req=LegacyRequirement.parse(source);
        if(!req.craftKey().startsWith("unsupported:")){
            String key=req.craftKey();int slash=key.indexOf('/');String base=slash<0?key:key.substring(0,slash);var id=ResourceLocation.tryParse(base);
            if(id!=null&&BuiltInRegistries.ITEM.containsKey(id)){
                var stack=new ItemStack(BuiltInRegistries.ITEM.get(id),req.count());
                if(slash>0&&base.equals("thaumcraft:celestial_notes")){stack=thaumcraft.port.content.CelestialNotesItem.stack(Integer.parseInt(key.substring(slash+1)));stack.setCount(req.count());}
                if(slash>0){var aspect=thaumcraft.port.aspect.Aspect.byId(key.substring(slash+1)).orElse(null);if(aspect!=null){if(base.endsWith("crystal_essence"))stack=thaumcraft.port.content.CrystalItem.stack(aspect);else if(base.endsWith("phial"))stack=thaumcraft.port.essentia.EssentiaItem.phial(aspect);stack.setCount(req.count());}}
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }
    private static String variant(String name,int meta){String v=VARIANTS.get(name);if(v==null)return "";var all=v.split(",");return meta>=0&&meta<all.length?all[meta]:"";}
    public static List<ResourceLocation> iconTextures(String source){
        if(source.contains("textures/")){var id=ResourceLocation.tryParse(source);return id!=null&&has(id)?List.of(id):List.of();}
        if(source.startsWith("focus:")){String key=source.substring(source.lastIndexOf('.')+1).toLowerCase(Locale.ROOT);for(String prefix:List.of("textures/foci/","textures/items/focus_","textures/gui/focus/")){var t=id(prefix+key+".png");if(has(t))return List.of(t);}return List.of();}
        var p=source.split(";",4);if(!p[0].startsWith("thaumcraft:"))return List.of();String name=p[0].substring(11);int meta=0;try{if(p.length>2)meta=Integer.parseInt(p[2]);}catch(NumberFormatException ignored){}
        String variant=variant(name,meta),modelName="item/"+name;
        JsonObject model=LEGACY.getAsJsonObject("models").has(modelName)?LEGACY.getAsJsonObject("models").getAsJsonObject(modelName):null;
        JsonObject states=LEGACY.getAsJsonObject("blockstates");
        if(model==null&&states.has(name)){
            var state=states.getAsJsonObject(name);model=state.has("defaults")?state.getAsJsonObject("defaults").deepCopy():new JsonObject();
            var vs=state.getAsJsonObject("variants");if(vs!=null){JsonElement v=vs.has(variant)?vs.get(variant):vs.has("normal")?vs.get("normal"):vs.has("inventory")?vs.get("inventory"):vs.entrySet().iterator().next().getValue();if(v.isJsonArray())v=v.getAsJsonArray().isEmpty()?new JsonObject():v.getAsJsonArray().get(0);if(v.isJsonObject())for(var e:v.getAsJsonObject().entrySet())model.add(e.getKey(),e.getValue());}
        }
        List<ResourceLocation> out=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(int depth=0;model!=null&&depth<8;depth++){
            if(model.has("textures")){var ts=model.getAsJsonObject("textures");for(var e:ts.entrySet()){String v=e.getValue().getAsString();if(v.startsWith("#")||(!e.getKey().startsWith("layer")&&!out.isEmpty()))continue;var at=v.split(":",2);if(at.length!=2)continue;var t=ResourceLocation.tryParse(at[0]+":textures/"+at[1]+".png");if(t!=null&&has(t)&&!out.contains(t))out.add(t);}}
            String parent=model.has("parent")?model.get("parent").getAsString():model.has("model")?model.get("model").getAsString():"";parent=parent.replace("thaumcraft:","");if(!parent.contains("/"))parent="block/"+parent;
            if(!seen.add(parent))break;model=LEGACY.getAsJsonObject("models").has(parent)?LEGACY.getAsJsonObject("models").getAsJsonObject(parent):null;
        }
        if(out.isEmpty())for(String n:List.of(name+(variant.isEmpty()?"":"_"+variant),name)){var t=id("textures/items/"+n+".png");if(has(t)){out.add(t);break;}}
        return out;
    }
    public static void icon(GuiGraphics g,String source,int x,int y){
        icon(g,source,x,y,true);
    }
    /** TC6 research uses original item GUI models, independently of the port's physical items. */
    public static void researchIcon(GuiGraphics g,ResearchCatalog.Entry entry,int x,int y){
        researchIcon(g,entry,x,y,false,false,1);
    }
    public static void researchBrowserIcon(GuiGraphics g,ResearchCatalog.Entry entry,int x,int y,boolean locked,float brightness){
        researchIcon(g,entry,x,y,true,locked,brightness);
    }
    private static void researchIcon(GuiGraphics g,ResearchCatalog.Entry entry,int x,int y,boolean browser,boolean locked,float brightness){
        if(entry.icons().isEmpty())return;
        String source=entry.icons().get(thaumcraft.port.content.ResearchDisplayItem.frame(entry.icons().size(),System.currentTimeMillis()));
        // Only the browser animates raw strips; required-research icons use the entire bitmap.
        if(source.contains(":textures/")){
            float shade=browser?(locked?.2f:brightness):1;g.setColor(shade,shade,shade,1);
            texture(g,ResourceLocation.parse(source),x,y,16,browser);g.setColor(1,1,1,1);return;
        }
        g.setColor(1,1,1,1);
        if(source.startsWith("focus:")){focus(g,source,x,y);return;}
        // RenderItem in 1.12 resets GL color before every ItemStack, even for locked entries.
        var item=thaumcraft.port.content.PortContent.RESEARCH_DISPLAY_ICONS.get(thaumcraft.port.content.ResearchDisplayItem.forResearch(entry.key()));
        g.renderItem(new ItemStack(item.get()),x,y);
    }
    public static void icon(GuiGraphics g,String source,int x,int y,boolean decorations){
        source=ingredient(source);
        if(source.equals("minecraft:air"))return;
        if(source.startsWith("focus:")){focus(g,source,x,y);return;}
        var stack=stack(source);
        if(model(g,source,x,y,stack)){
            if(decorations){
                var label=stack;if(label.isEmpty()){var parts=source.split(";",4);int count=parts.length>1?Integer.parseInt(parts[1]):1;label=new ItemStack(Items.PAPER,count);}
                g.renderItemDecorations(Minecraft.getInstance().font,label,x,y);
            }
            return;
        }
        if(!stack.isEmpty()){g.renderItem(stack,x,y);if(decorations)g.renderItemDecorations(Minecraft.getInstance().font,stack,x,y);return;}
        var textures=iconTextures(source);if(textures.isEmpty())texture(g,UNKNOWN,x,y,16);else for(var t:textures)texture(g,t,x,y,16);
    }
    public static Component name(String source){
        source=ingredient(source);
        var captured=BookItemPresentation.catalogTooltip(source);if(!captured.isEmpty())return captured.getFirst();
        var original=BookItemPresentation.name(source);if(original!=null)return original;
        var stack=stack(source);if(!stack.isEmpty())return stack.getHoverName();
        String raw=source.split(";",4)[0];if(raw.contains("textures/"))raw=raw.substring(raw.lastIndexOf('/')+1).replace(".png","");else raw=raw.substring(raw.indexOf(':')+1);
        String key="item."+raw+".name";if(net.minecraft.client.resources.language.I18n.exists(key))return Component.translatable(key);
        key="tile."+raw+".name";if(net.minecraft.client.resources.language.I18n.exists(key))return Component.translatable(key);
        return Component.literal(raw.replace('_',' '));
    }
    /** Book-only legacy metadata/NBT labels; does not create substitute gameplay items. */
    public static List<Component> tooltip(String source){
        source=ingredient(source);
        var mc=Minecraft.getInstance();
        var captured=BookItemPresentation.catalogTooltip(source,mc.options.advancedItemTooltips);if(!captured.isEmpty())
            return BookItemPresentation.withPlayerContext(source,captured,ClientJournal.state.permanentWarp());
        var stack=stack(source);
        var lines=stack.isEmpty()||BookItemPresentation.hasOwnTooltip(source)?new ArrayList<Component>(List.of(name(source))):new ArrayList<>(stack.getTooltipLines(
                Item.TooltipContext.of(mc.level),mc.player,mc.options.advancedItemTooltips?TooltipFlag.ADVANCED:TooltipFlag.NORMAL));
        lines.addAll(BookItemPresentation.details(source));
        return BookItemPresentation.withPlayerContext(source,List.copyOf(lines),ClientJournal.state.permanentWarp());
    }
    private BookAssets(){}
    private static void focus(GuiGraphics g,String source,int x,int y){
        String name=source.substring(source.lastIndexOf('.')+1).toLowerCase(Locale.ROOT).replace("splittarget","split_target").replace("splittrajectory","split_trajectory");
        int color=switch(name){case "fire"->16734721;case "frost"->0xe1ffff;case "air"->0xffff7e;case "earth"->5685248;case "flux"->0x800080;case "break"->9063176;case "rift"->3084645;case "exchange"->5735255;case "curse"->6946821;case "heal"->14548997;case "touch"->11371909;case "bolt"->11377029;case "projectile"->11382149;case "cloud"->10071429;case "mine"->8760709;case "plan"->8760728;case "spellbat"->8760748;default->0x999999;};
        boolean mod=name.equals("root")||name.startsWith("split")||name.equals("scatter"),medium=Set.of("touch","bolt","projectile","cloud","mine","plan","spellbat").contains(name);
        g.setColor(1,1,1,1);
        if(!mod)focusQuad(g,id("textures/foci/_"+(medium?"medium":"effect")+".png"),x-2.8f,y-2.8f,21.6f,0,color);
        focusQuad(g,id("textures/foci/"+name+".png"),x+(mod?-4:2),y+(mod?-4:2),mod?24:12,1,0xffffff);
    }
    private static void focusQuad(GuiGraphics g,ResourceLocation texture,float x,float y,float size,float z,int color){
        // TC6 writes brightness 50/220, but EntityRenderer disables lightmap before GUI.
        // Its UV2 values are inert here: a locked focus glyph is still white.
        g.setColor(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,1);
        g.pose().pushPose();g.pose().translate(x,y,z);g.pose().scale(size/16,size/16,1);
        texture(g,texture,0,0,16,false);g.pose().popPose();g.setColor(1,1,1,1);
    }
    public static void block(GuiGraphics g,String source){
        BookBlocks.render(g,source);
    }
    public static void block(GuiGraphics g,String source,net.minecraft.world.level.BlockAndTintGetter view,net.minecraft.core.BlockPos pos){
        BookBlocks.render(g,source,view,pos);
    }
    private static boolean model(GuiGraphics g,String source,int x,int y,ItemStack actual){
        if(!source.startsWith("thaumcraft:")||source.contains("textures/"))return false;
        var p=source.split(";",4);String name=p[0].substring(11);int meta=0;try{if(p.length>2)meta=Integer.parseInt(p[2]);}catch(NumberFormatException ignored){}if(meta==32767)meta=0;
        if(name.equals("verdant_charm")&&p.length>3)meta=Math.clamp(BookItemPresentation.descriptor(source).tag().getInt("type"),0,2);
        // Filled jars need their live level predicates; the extracted static base is empty.
        if(!actual.isEmpty()&&Set.of("jar_normal","jar_void","warded_jar","void_jar").contains(name))return false;
        if(name.equals("seal")||name.equals("seals"))name="seal_"+(p.length>3?p[3].substring(p[3].lastIndexOf(':')+1):"blank");
        String key=name+"_"+meta;if(!MODELS.has(key))return false;var mc=Minecraft.getInstance();var baked=mc.getModelManager().getModel(net.minecraft.client.resources.model.ModelResourceLocation.standalone(ResourceLocation.parse(MODELS.get(key).getAsString())));
        g.setColor(1,1,1,1);g.pose().pushPose();g.pose().translate(x+8,y+8,150);g.pose().scale(16,-16,16);boolean flat=!baked.usesBlockLight();if(flat)com.mojang.blaze3d.platform.Lighting.setupForFlatItems();
        // Keep real component-dependent ItemColors (crystals/phials/nitor) with original GUI geometry.
        var carrier=actual.isEmpty()?new ItemStack(thaumcraft.port.content.PortContent.RESEARCH_ICON.get()):actual.copy();
        if(Set.of("cloth_chest","cloth_legs","cloth_boots","void_robe_chest","void_robe_helm","void_robe_legs").contains(name))
            carrier.set(net.minecraft.core.component.DataComponents.DYED_COLOR,new net.minecraft.world.item.component.DyedItemColor(6961280,false));
        mc.getItemRenderer().render(carrier,ItemDisplayContext.GUI,false,g.pose(),g.bufferSource(),15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,baked);g.flush();if(flat)com.mojang.blaze3d.platform.Lighting.setupFor3DItems();g.pose().popPose();return true;
    }
}
