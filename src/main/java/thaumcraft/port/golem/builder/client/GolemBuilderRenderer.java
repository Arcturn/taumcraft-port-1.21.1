package thaumcraft.port.golem.builder.client;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import thaumcraft.port.golem.builder.*;
import thaumcraft.port.golem.client.GolemObjModel;
/** Original press OBJ, orientation and sin(press)*.625 travel. */
public final class GolemBuilderRenderer implements BlockEntityRenderer<GolemBuilderEntity>{
 private static final ResourceLocation TEXTURE=ResourceLocation.parse("thaumcraft:textures/blocks/golembuilder.png");
 private final GolemObjModel model;
 public GolemBuilderRenderer(BlockEntityRendererProvider.Context context){model=new GolemObjModel(ResourceLocation.parse("thaumcraft:models/block/golembuilder.obj"),false);}
 @Override public void render(GolemBuilderEntity b,float partial,PoseStack pose,MultiBufferSource buffer,int light,int overlay){pose.pushPose();pose.translate(.5,0,.5);float rotation=switch(b.getBlockState().getValue(GolemBuilderBlock.FACING)){case EAST->270;case WEST->90;case SOUTH->180;default->0;};pose.mulPose(Axis.YP.rotationDegrees(rotation));var out=buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
  for(String group:model.groups())if(!group.equals("press"))model.render(group,pose,out,light,overlay,-1);
  pose.pushPose();pose.translate(0,-Math.sin(Math.toRadians(b.press))*.625,0);model.render("press",pose,out,light,overlay,-1);pose.popPose();
  var sprite=Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(Blocks.LAVA.defaultBlockState());var lava=buffer.getBuffer(RenderType.entityTranslucent(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS));
  float x1=-.3125f,x2=.3125f,z1=1.3125f,z2=.6875f,y=.625f;var p=pose.last();int fullLight=200;
  for(float[] v:new float[][]{{x1,y,z1,sprite.getU1(),sprite.getV1()},{x2,y,z1,sprite.getU0(),sprite.getV1()},{x2,y,z2,sprite.getU0(),sprite.getV0()},{x1,y,z2,sprite.getU1(),sprite.getV0()}})lava.addVertex(p.pose(),v[0],v[1],v[2]).setColor(255,255,255,255).setUv(v[3],v[4]).setOverlay(overlay).setLight(fullLight).setNormal(p,0,1,0);
  pose.popPose();
 }
 @Override public AABB getRenderBoundingBox(GolemBuilderEntity b){var p=b.getBlockPos();return new AABB(p.getX()-1,p.getY(),p.getZ()-1,p.getX()+2,p.getY()+2,p.getZ()+2);}
}
