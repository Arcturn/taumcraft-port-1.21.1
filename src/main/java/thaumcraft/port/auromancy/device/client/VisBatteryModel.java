package thaumcraft.port.auromancy.device.client;

import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;

/** The source clamps only the world model's block-light coordinate to raw 180. */
@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class VisBatteryModel extends BakedModelWrapper<BakedModel> {
    private VisBatteryModel(BakedModel original){super(original);}
    @SubscribeEvent public static void models(ModelEvent.ModifyBakingResult event){
        var block=BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:vis_battery"));
        for(var state:block.getStateDefinition().getPossibleStates()){
            var id=BlockModelShaper.stateToModelLocation(state);
            event.getModels().computeIfPresent(id,(key,model)->new VisBatteryModel(model));
        }
    }
    @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random){return getQuads(state,side,random,ModelData.EMPTY,null);}
    @Override public List<BakedQuad> getQuads(BlockState state,Direction side,RandomSource random,ModelData data,RenderType type){
        var quads=originalModel.getQuads(state,side,random,data,type);
        return state==null?quads:quads.stream().map(VisBatteryModel::light).toList();
    }
    public static BakedQuad light(BakedQuad source){
        int[] vertices=source.getVertices().clone();
        for(int vertex=0;vertex<4;vertex++){
            int offset=vertex*IQuadTransformer.STRIDE+IQuadTransformer.UV2,light=vertices[offset];
            vertices[offset]=(light&0xffff0000)|Math.max(light&0xffff,180);
        }
        return new BakedQuad(vertices,source.getTintIndex(),source.getDirection(),source.getSprite(),source.isShade(),source.hasAmbientOcclusion());
    }
}
