import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.resources.model.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.renderer.texture.atlas.*;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomModelData;

/** CPU-only bake using actual mapped vanilla 1.21.1 classes, SpriteLoader and ItemOverrides.
 * Args: vanilla-client.jar mod.jar. NeoForge custom geometries are explicitly skipped;
 * the in-client IconModelAudit covers those after the real loader runs. No OpenGL.
 */
public class IconModelSmokeTest implements ModelBaker {
 final ResourceManager resources;
 final Map<ResourceLocation, BlockModel> models=new HashMap<>();
 final Map<ResourceLocation, BakedModel> baked=new HashMap<>();
 final Map<ResourceLocation, TextureAtlasSprite> sprites=new HashMap<>();
 final Set<ResourceLocation> custom=new HashSet<>();
 final Map<ResourceLocation,ResourceLocation> parents=new HashMap<>();
 static class Sprite extends TextureAtlasSprite {
  Sprite(SpriteContents c){super(TextureAtlas.LOCATION_BLOCKS,c,4096,4096,0,0);}
 }
 IconModelSmokeTest(ResourceManager resources){this.resources=resources;}
 public UnbakedModel getModel(ResourceLocation id){
  if(id.getPath().equals("builtin/generated"))return ModelBakery.GENERATION_MARKER;
  if(id.getPath().equals("builtin/entity"))return ModelBakery.BLOCK_ENTITY_MARKER;
  if(models.containsKey(id))return models.get(id);
  try(var reader=resources.getResourceOrThrow(id.withPrefix("models/").withSuffix(".json")).openAsReader()){
   var json=JsonParser.parseReader(reader).getAsJsonObject();
   if(json.has("loader"))custom.add(id);
   if(json.has("parent"))parents.put(id,ResourceLocation.parse(json.get("parent").getAsString()));
   var model=BlockModel.fromString(json.toString());model.name=id.toString();models.put(id,model);
   model.resolveParents(this::getModel);return model;
  }catch(Exception e){throw new IllegalStateException(id.toString(),e);}
 }
 boolean isCustom(ResourceLocation id){
  getModel(id);if(custom.contains(id))return true;
  return parents.containsKey(id)&&isCustom(parents.get(id));
 }
 public BakedModel bake(ResourceLocation id,ModelState state){
  if(baked.containsKey(id))return baked.get(id);
  var m=(BlockModel)getModel(id);
  if(isCustom(id)){System.out.println("SKIP NeoForge geometry: "+id);return null;}
  java.util.function.Function<Material,TextureAtlasSprite> getter=mat->{
   var s=sprites.get(mat.texture());
   if(s==null)throw new IllegalStateException("Absent sprite "+mat.texture()+" in "+id);
   return s;
  };
  var b=m.getRootModel()==ModelBakery.GENERATION_MARKER?
   new ItemModelGenerator().generateBlockModel(getter,m).bake(this,m,getter,state,false):m.bake(this,getter,state);
  baked.put(id,b);return b;
 }
 static PackResources pack(String name,String file){return new FilePackResources.FileResourcesSupplier(Path.of(file)).openPrimary(new PackLocationInfo(name,Component.literal(name),PackSource.BUILT_IN,Optional.empty()));}
 public static void main(String[] args)throws Exception{
  SharedConstants.tryDetectVersion();Bootstrap.bootStrap();
  try(var res=new MultiPackResourceManager(PackType.CLIENT_RESOURCES,List.of(pack("minecraft",args[0]),pack("thaumcraft",args[1])))){
   var test=new IconModelSmokeTest(res);
   var loader=SpriteResourceLoader.create(List.of(AnimationMetadataSection.SERIALIZER));
   for(var supplier:SpriteSourceList.load(res,ResourceLocation.parse("minecraft:blocks")).list(res)){
    var c=supplier.apply(loader);if(c!=null)test.sprites.put(c.name(),new Sprite(c));
   }
   var data=new JsonObject();
   JsonObject advancementIcons,originalIcons;
   try(var jar=new java.util.zip.ZipFile(args[1])) {
    advancementIcons=JsonParser.parseString(new String(jar.getInputStream(jar.getEntry("thaumcraft-port/advancement-icon-index.json")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
    for(var e:advancementIcons.entrySet())data.add("advancement:"+e.getKey(),e.getValue());
    originalIcons=JsonParser.parseString(new String(jar.getInputStream(jar.getEntry("thaumcraft-port/research-display-icons.json")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
    for(var e:originalIcons.entrySet()) {
     int frame=0;
     for(var variant:e.getValue().getAsJsonObject().getAsJsonArray("variants"))data.add("original:"+e.getKey()+":"+(frame++),variant);
    }
    var book=JsonParser.parseString(new String(jar.getInputStream(jar.getEntry("thaumcraft-port/book-model-index.json")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
    for(var e:book.entrySet()){var model=new JsonObject();model.add("model",e.getValue());data.add("book:"+e.getKey(),model);}
    for(var entry:jar.stream().filter(e->e.getName().startsWith("data/thaumcraft/advancement/")&&e.getName().endsWith(".json")).toList()) {
     var a=JsonParser.parseString(new String(jar.getInputStream(entry).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
     var icon=a.getAsJsonObject("display").getAsJsonObject("icon");
     if(!icon.get("id").getAsString().equals("thaumcraft:research_icon")){var model=new JsonObject();model.addProperty("model",icon.get("id").getAsString().replace(":",":item/"));data.add("direct:"+entry.getName(),model);}
    }
   }
   int pass=0,skip=0,fail=0,originalPass=0,originalSkip=0;
   for(var e:data.entrySet())try{
    var id=ResourceLocation.parse(e.getValue().getAsJsonObject().get("model").getAsString());
    if(test.isCustom(id)){skip++;if(e.getKey().startsWith("original:"))originalSkip++;System.out.println("SKIP "+e.getKey()+" "+id);continue;}
    var b=test.bake(id,BlockModelRotation.X0_Y0);List<BakedQuad> quads=new ArrayList<>(b.getQuads(null,null,RandomSource.create(0)));
    for(var d:Direction.values())quads.addAll(b.getQuads(null,d,RandomSource.create(0)));
    if(quads.isEmpty())throw new AssertionError("Empty geometry");
    for(var q:quads)if(q.getSprite().contents().name().equals(MissingTextureAtlasSprite.getLocation()))throw new AssertionError("Missing-texture quad");
    pass++;
    if(e.getKey().startsWith("original:"))originalPass++;
   }catch(Throwable e2){fail++;System.out.println("FAIL "+e.getKey()+" "+e.getValue()+" "+e2);}
   System.out.println("Baked "+pass+"; failed "+fail+"; custom skipped "+skip);
   if(originalPass+originalSkip!=173)throw new AssertionError("Missing original icon variants");
   System.out.println("Original art variants baked: "+originalPass+"; NeoForge OBJ variants skipped: "+originalSkip);
   // Register the production (unclamped) property in the real vanilla property
   // registry. Vanilla exposes this hook privately; NeoForge makes it public.
   var propertyField=net.minecraft.client.renderer.item.ItemProperties.class.getDeclaredField("GENERIC_PROPERTIES");
   propertyField.setAccessible(true);
   @SuppressWarnings("unchecked")
   var properties=(Map<ResourceLocation,net.minecraft.client.renderer.item.ItemPropertyFunction>)propertyField.get(null);
   var propertyId=ResourceLocation.parse("thaumcraft:research_frame");
   long[] time={0};int frameSelections=0;
   for(var definition:originalIcons.entrySet()) {
    var variants=definition.getValue().getAsJsonObject().getAsJsonArray("variants");
    var primaryId=ResourceLocation.parse(variants.get(0).getAsJsonObject().get("model").getAsString());
    if(test.isCustom(primaryId))continue;
    var primary=test.bake(primaryId,BlockModelRotation.X0_Y0);
    properties.put(propertyId,new thaumcraft.port.client.ResearchFrameProperty(variants.size(),()->time[0]));
    for(int n=0;n<variants.size();n++) {
     var target=test.bake(ResourceLocation.parse(variants.get(n).getAsJsonObject().get("model").getAsString()),BlockModelRotation.X0_Y0);
     for(int end:new int[]{0,999}) {
      time[0]=n*1000L+end;
      if(primary.getOverrides().resolve(primary,new ItemStack(Items.PAPER),null,null,0)!=target)throw new AssertionError("Wrong original research frame: "+definition.getKey()+" frame "+n);
      frameSelections++;
     }
    }
    time[0]=variants.size()*1000L;
    if(primary.getOverrides().resolve(primary,new ItemStack(Items.PAPER),null,null,0)!=primary)throw new AssertionError("Original research cycle does not wrap: "+definition.getKey());
   }
   properties.remove(propertyId);
   System.out.println("PASS: "+frameSelections+" original-art frame selections through production property + actual Minecraft ItemOverrides");
   // Exercise Minecraft's real property selection, not a hand-written >= comparison.
   var carrier=test.bake(ResourceLocation.parse("thaumcraft:item/research_icon"),BlockModelRotation.X0_Y0);
   int selected=0;
   for(var entry:advancementIcons.entrySet()) {
    var target=ResourceLocation.parse(entry.getValue().getAsJsonObject().get("model").getAsString());
    if(test.isCustom(target))continue;
    var stack=new ItemStack(Items.PAPER);stack.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(Integer.parseInt(entry.getKey())));
    if(carrier.getOverrides().resolve(carrier,stack,null,null,0)!=test.baked.get(target))throw new AssertionError("Wrong override selection: "+entry.getKey());
    selected++;
   }
   if(carrier.getOverrides().resolve(carrier,new ItemStack(Items.PAPER),null,null,0)!=carrier)throw new AssertionError("Base icon replaced without custom_model_data");
   System.out.println("PASS: "+selected+" actual CustomModelData override selections plus the default icon");
   test.sprites.values().forEach(s->s.contents().close());if(fail>0)throw new AssertionError(fail+" icons failed");
  }
 }
}
