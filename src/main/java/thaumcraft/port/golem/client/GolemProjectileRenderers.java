package thaumcraft.port.golem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import thaumcraft.port.golem.projectile.*;

final class GolemProjectileRenderers {
    static final class Dart extends ArrowRenderer<GolemDartEntity> {
        Dart(EntityRendererProvider.Context context){super(context);}
        @Override public ResourceLocation getTextureLocation(GolemDartEntity entity){return ResourceLocation.parse("thaumcraft:textures/entity/golem_dart.png");}
        @Override public void render(GolemDartEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            // ArrowRenderer's six quads/UVs and shake formula are unchanged
            // since 1.12; only TC6's smaller .033 scale differs from .05625.
            pose.pushPose();float scale=.033f/.05625f;pose.scale(scale,scale,scale);super.render(entity,yaw,partial,pose,buffers,light);pose.popPose();
        }
    }
    static final class Orb extends EntityRenderer<GolemOrbEntity> {
        Orb(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
        @Override public ResourceLocation getTextureLocation(GolemOrbEntity entity){return ResourceLocation.parse("thaumcraft:textures/misc/particles.png");}
        @Override public void render(GolemOrbEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
            pose.pushPose();var camera=Minecraft.getInstance().gameRenderer.getMainCamera();pose.mulPose(Axis.YP.rotationDegrees(180-camera.getYRot()));pose.mulPose(Axis.XP.rotationDegrees(-camera.getXRot()));
            float size=1.2f+Mth.sin(entity.tickCount/5f)*.2f;pose.scale(size,size,size);
            float u=(1+entity.tickCount%6)/32f,v=entity.isRed()?.1875f:.21875f;var out=buffers.getBuffer(GolemRenderLayers.ORB);
            // The original vertex RGBA is white/1; it overrides the preceding
            // fixed-function glColor alpha .8 when the color array is enabled.
            out.addVertex(pose.last().pose(),-.5f,-.5f,0).setUv(u,v+1/32f).setColor(-1).setLight(220);
            out.addVertex(pose.last().pose(),.5f,-.5f,0).setUv(u+1/32f,v+1/32f).setColor(-1).setLight(220);
            out.addVertex(pose.last().pose(),.5f,.5f,0).setUv(u+1/32f,v).setColor(-1).setLight(220);
            out.addVertex(pose.last().pose(),-.5f,.5f,0).setUv(u,v).setColor(-1).setLight(220);pose.popPose();
        }
    }
    private GolemProjectileRenderers(){}
}
