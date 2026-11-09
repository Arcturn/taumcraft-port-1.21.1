package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import thaumcraft.port.world.*;

@EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class TaintRenderers {
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){
        event.registerEntityRenderer(WorldEntities.FALLING_TAINT.get(),FallingRenderer::new);
        FallingTaint.LANDING_FX=TaintParticles::landing;
        TaintVisualHooks.GOO_BUBBLE=TaintParticles::goo;
    }
    private static final class FallingRenderer extends EntityRenderer<FallingTaint> {
        FallingRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=.5f;}
        @Override public ResourceLocation getTextureLocation(FallingTaint entity){return TextureAtlas.LOCATION_BLOCKS;}
        @Override public void render(FallingTaint entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            var state=entity.getBlockState();
            if(state.getRenderShape()==RenderShape.MODEL&&state!=entity.level().getBlockState(entity.blockPosition())){
                pose.pushPose();pose.translate(-.5,0,-.5);
                var renderer=Minecraft.getInstance().getBlockRenderer();var model=renderer.getBlockModel(state);
                var seed=state.getSeed(entity.getStartPos());
                for(var layer:model.getRenderTypes(state,RandomSource.create(seed),ModelData.EMPTY))
                    renderer.getModelRenderer().tesselateBlock(entity.level(),model,state,entity.blockPosition(),pose,buffers.getBuffer(net.neoforged.neoforge.client.RenderTypeHelper.getMovingBlockRenderType(layer)),false,RandomSource.create(seed),seed,OverlayTexture.NO_OVERLAY,ModelData.EMPTY,layer);
                pose.popPose();
            }
            super.render(entity,yaw,partial,pose,buffers,light);
        }
    }
    private TaintRenderers(){}
}
