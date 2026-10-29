package thaumcraft.port.client;

import com.google.gson.*;
import com.mojang.math.Transformation;
import java.util.*;
import java.util.function.Function;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.*;
import net.neoforged.neoforge.client.model.geometry.*;
import net.neoforged.neoforge.client.model.obj.*;
import thaumcraft.port.world.CrystalClusterBlock;

/** Original eight OBJ shards, with TC6's stable subset per attached face and growth stage. */
public final class CrystalModel extends BakedModelWrapper<BakedModel> {
    public static final List<String> PARTS=List.of("s014","s015","s016","s017","s018","s019","s024","s026");
    private static final ModelProperty<Long> POSITION_SEED=new ModelProperty<>();
    private final Map<Direction,List<List<BakedQuad>>> faces;
    private CrystalModel(BakedModel item,Map<Direction,List<List<BakedQuad>>> faces){super(item);this.faces=faces;}
    /** TC6 BlockCrystal#getPackedLightmapCoords clamps block light to raw 180, leaving sky light intact.
     * This is model lighting only: the block still emits level 1 into the world.
     * MeshModel also disables diffuse shading; the emitting block takes the flat-lighting path.
     */
    public static BakedQuad lightWorldQuad(BakedQuad source){
        int[] vertices=source.getVertices().clone();
        for(int vertex=0;vertex<4;vertex++){
            int offset=vertex*IQuadTransformer.STRIDE+IQuadTransformer.UV2,light=vertices[offset];
            vertices[offset]=(light&0xffff0000)|Math.max(light&0xffff,180);
        }
        return new BakedQuad(vertices,source.getTintIndex(),source.getDirection(),source.getSprite(),false,false);
    }
    public static List<Integer> selection(long seed,Direction face,int size){
        var indexes=new ArrayList<>(List.of(0,1,2,3,4,5,6,7));int offset=switch(face){case UP->0;case DOWN->5;case EAST->10;case WEST->15;case NORTH->20;case SOUTH->25;};
        Collections.shuffle(indexes,new Random(seed+offset));return List.copyOf(indexes.subList(0,Math.clamp(size+1,1,4)));
    }
    @Override public ModelData getModelData(BlockAndTintGetter level,BlockPos pos,BlockState state,ModelData data){return data.derive().with(POSITION_SEED,state.getSeed(pos)).build();}
    @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random){return getQuads(state,side,random,ModelData.EMPTY,null);}
    @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType type){
        if(state==null||!(state.getBlock() instanceof CrystalClusterBlock))return originalModel.getQuads(state,side,random,data,type);
        if(side!=null)return List.of();
        // The original hides a fully enclosed cluster.
        if(CrystalClusterBlock.FACES.values().stream().allMatch(state::getValue))return List.of();
        long seed=data.has(POSITION_SEED)?data.get(POSITION_SEED):random.nextLong();var quads=new ArrayList<BakedQuad>();
        for(Direction face:Direction.values())if(state.getValue(CrystalClusterBlock.FACES.get(face)))for(int part:selection(seed,face,state.getValue(CrystalClusterBlock.SIZE)))quads.addAll(faces.get(face).get(part));
        return quads;
    }
    public static final class Loader implements IGeometryLoader<Geometry>{
        @Override public Geometry read(JsonObject json,JsonDeserializationContext context){return new Geometry(ObjLoader.INSTANCE.read(json,context));}
    }
    public record Geometry(ObjModel source) implements IUnbakedGeometry<Geometry>{
        @Override public BakedModel bake(IGeometryBakingContext owner,ModelBaker baker,Function<Material,TextureAtlasSprite> sprites,ModelState state,ItemOverrides overrides){
            if(!source.getRootComponentNames().containsAll(PARTS))throw new IllegalStateException("Incomplete original crystal OBJ");
            var faces=new EnumMap<Direction,List<List<BakedQuad>>>(Direction.class);
            for(Direction direction:Direction.values()){
                var rotation=switch(direction){case DOWN->BlockModelRotation.X0_Y0;case UP->BlockModelRotation.X180_Y0;case NORTH->BlockModelRotation.X270_Y0;case SOUTH->BlockModelRotation.X90_Y0;case EAST->BlockModelRotation.X90_Y270;case WEST->BlockModelRotation.X90_Y90;};
                var parts=new ArrayList<List<BakedQuad>>();
                for(String part:PARTS){var model=source.bake(new PartContext(owner,Set.of(part)),baker,sprites,rotation,overrides);parts.add(model.getQuads(null,null,RandomSource.create(0)).stream().map(CrystalModel::lightWorldQuad).toList());}
                faces.put(direction,List.copyOf(parts));
            }
            var item=source.bake(new PartContext(owner,Set.copyOf(PARTS.subList(0,4))),baker,sprites,state,overrides);
            return new CrystalModel(item,faces);
        }
        @Override public Set<String> getConfigurableComponentNames(){return source.getConfigurableComponentNames();}
    }
    private record PartContext(IGeometryBakingContext delegate,Set<String> visible) implements IGeometryBakingContext{
        public String getModelName(){return delegate.getModelName();}
        public boolean hasMaterial(String name){return delegate.hasMaterial(name);}
        public Material getMaterial(String name){return delegate.getMaterial(name);}
        public boolean isGui3d(){return delegate.isGui3d();}
        public boolean useBlockLight(){return delegate.useBlockLight();}
        public boolean useAmbientOcclusion(){return delegate.useAmbientOcclusion();}
        public ItemTransforms getTransforms(){return delegate.getTransforms();}
        public Transformation getRootTransform(){return delegate.getRootTransform();}
        public ResourceLocation getRenderTypeHint(){return delegate.getRenderTypeHint();}
        public boolean isComponentVisible(String name,boolean fallback){return visible.contains(name);}
    }
}
