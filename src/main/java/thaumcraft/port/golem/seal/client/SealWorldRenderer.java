package thaumcraft.port.golem.seal.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Quaternionf;
import thaumcraft.port.golem.GolemHelper;
import thaumcraft.port.golem.item.GolemBellItem;
import thaumcraft.port.golem.seal.*;

/** Animated atlas seals, coloured rotating ring and BETA26 area-corner frames. */
@EventBusSubscriber(modid="thaumcraft",value=Dist.CLIENT)
public final class SealWorldRenderer extends RenderType {
    private static final ResourceLocation MIDDLE=ResourceLocation.parse("thaumcraft:textures/misc/seal_area.png"),CORNER=ResourceLocation.parse("thaumcraft:textures/misc/frame_corner.png");
    private static final RenderType ICON=layer("icon",TextureAtlas.LOCATION_BLOCKS,false),ICON_THROUGH=layer("icon_through",TextureAtlas.LOCATION_BLOCKS,true),RING=layer("ring",MIDDLE,false),RING_THROUGH=layer("ring_through",MIDDLE,true),FRAME=layer("frame",CORNER,false),FRAME_THROUGH=layer("frame_through",CORNER,true);
    private static final Direction[][] FACES={{Direction.DOWN,Direction.NORTH,Direction.WEST},{Direction.UP,Direction.NORTH,Direction.WEST},{Direction.DOWN,Direction.NORTH,Direction.EAST},{Direction.UP,Direction.NORTH,Direction.EAST},{Direction.DOWN,Direction.SOUTH,Direction.EAST},{Direction.UP,Direction.SOUTH,Direction.EAST},{Direction.DOWN,Direction.SOUTH,Direction.WEST},{Direction.UP,Direction.SOUTH,Direction.WEST}};
    private static final int[][] ROTATIONS={{0,270,0},{270,180,270},{90,0,90},{180,90,180},{180,180,0},{90,270,270},{270,90,90},{0,0,180}};
    private static RenderType layer(String name,ResourceLocation texture,boolean through){return create("thaumcraft_seal_"+name,DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,4096,false,true,CompositeState.builder().setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader)).setTextureState(new TextureStateShard(texture,false,false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setDepthTestState(through?NO_DEPTH_TEST:LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE).createCompositeState(false));}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL)return;var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
        boolean show=java.util.stream.Stream.of(mc.player.getMainHandItem(),mc.player.getOffhandItem()).anyMatch(s->s.getItem() instanceof GolemBellItem||s.getItem() instanceof SealPlacerItem);if(!show)return;
        boolean through=mc.player.isShiftKeyDown();var buffers=mc.renderBuffers().bufferSource();var pose=new PoseStack();pose.mulPose(event.getModelViewMatrix());var camera=event.getCamera().getPosition();float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
        for(var seal:SealHandler.getSealsInRange(mc.level,mc.player.blockPosition(),17)){
            var pos=seal.getSealPos();double distance=mc.player.distanceToSqr(pos.pos.getCenter());if(distance>256)continue;float alpha=1-(float)(distance/256);
            float brightness=seal.isStoppedByRedstone(mc.level)?.5f:1;
            if(brightness<1&&mc.level.random.nextFloat()<partial/12){mc.particleEngine.add(new RedstoneSpark(mc.level,pos));brightness=1;}
            pose.pushPose();pose.translate(pos.pos.getX()+.5+pos.face.getStepX()*.55,pos.pos.getY()+.5+pos.face.getStepY()*.55,pos.pos.getZ()+.5+pos.face.getStepZ()*.55);orient(pose,pos.face);
            var sprite=mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(seal.getSeal().getSealIcon());var vertex=buffers.getBuffer(through?ICON_THROUGH:ICON);
            quad(pose,vertex,.5f,0,sprite.getU1(),sprite.getV0(),sprite.getU0(),sprite.getV1(),brightness,brightness,brightness,alpha);
            if(!through){quad(pose,vertex,.5f,-.05f,sprite.getU1(),sprite.getV0(),sprite.getU0(),sprite.getV1(),brightness,brightness,brightness,alpha);int width=16,height=16;for(int i=0;i<width;i++){float x=-.25f+i*.5f/width,u=Mth.lerp((i+.5f)/width,sprite.getU1(),sprite.getU0());side(pose,vertex,x,-.25f,x,.25f,u,sprite.getV1(),u,sprite.getV0(),brightness*.8f,alpha);side(pose,vertex,x+.5f/width,-.25f,x+.5f/width,.25f,u,sprite.getV1(),u,sprite.getV0(),brightness*.8f,alpha);}for(int i=0;i<height;i++){float y=-.25f+i*.5f/height,v=Mth.lerp(1-(i+.5f)/height,sprite.getV0(),sprite.getV1());side(pose,vertex,-.25f,y,.25f,y,sprite.getU1(),v,sprite.getU0(),v,brightness*.8f,alpha);side(pose,vertex,-.25f,y+.5f/height,.25f,y+.5f/height,sprite.getU1(),v,sprite.getU0(),v,brightness*.8f,alpha);}}
            pose.popPose();
            float r,g,b;if(seal.getColor()>0){int c=DyeColor.byId(seal.getColor()-1).getTextureDiffuseColor();r=(c>>16&255)/255f;g=(c>>8&255)/255f;b=(c&255)/255f;}else{float tick=mc.player.tickCount+partial;r=.7f+Mth.sin((tick+pos.pos.getX())/4)*.1f;g=.7f+Mth.sin((tick+pos.pos.getY())/5)*.1f;b=.7f+Mth.sin((tick+pos.pos.getZ())/6)*.1f;}
            pose.pushPose();pose.translate(pos.pos.getX()+.5,pos.pos.getY()+.5,pos.pos.getZ()+.5);sourceFace(pose,pos.face,.51);pose.mulPose(Axis.ZP.rotationDegrees(mc.player.tickCount%360+partial));sourceQuad(pose,buffers.getBuffer(through?RING_THROUGH:RING),.9f,r,g,b,alpha*.8f);pose.popPose();
            if(seal.getSeal() instanceof ISealConfigArea){var area=GolemHelper.getBoundsForArea(seal);double[][] points={{area.minX,area.minY,area.minZ},{area.minX,area.maxY-1,area.minZ},{area.maxX-1,area.minY,area.minZ},{area.maxX-1,area.maxY-1,area.minZ},{area.maxX-1,area.minY,area.maxZ-1},{area.maxX-1,area.maxY-1,area.maxZ-1},{area.minX,area.minY,area.maxZ-1},{area.minX,area.maxY-1,area.maxZ-1}};for(int i=0;i<8;i++)for(int j=0;j<3;j++){pose.pushPose();pose.translate(points[i][0]+.5,points[i][1]+.5,points[i][2]+.5);sourceFace(pose,FACES[i][j],.49);pose.mulPose(Axis.ZP.rotationDegrees(ROTATIONS[i][j]-90));sourceQuad(pose,buffers.getBuffer(through?FRAME_THROUGH:FRAME),1,r,g,b,alpha*.7f);pose.popPose();}}
        }
        pose.popPose();buffers.endBatch(through?ICON_THROUGH:ICON);buffers.endBatch(through?RING_THROUGH:RING);buffers.endBatch(through?FRAME_THROUGH:FRAME);
    }
    private static void orient(PoseStack pose,Direction face){switch(face){case UP->pose.mulPose(Axis.XP.rotationDegrees(-90));case DOWN->pose.mulPose(Axis.XP.rotationDegrees(90));case NORTH->pose.mulPose(Axis.YP.rotationDegrees(180));case EAST->pose.mulPose(Axis.YP.rotationDegrees(90));case WEST->pose.mulPose(Axis.YP.rotationDegrees(-90));default->{}}}
    private static void sourceFace(PoseStack pose,Direction face,double offset){pose.mulPose(new Quaternionf().rotationAxis((float)Math.PI/2,-face.getStepY(),face.getStepX(),-face.getStepZ()));pose.translate(0,0,face.getStepZ()<0?-offset:offset);}
    private static void quad(PoseStack pose,VertexConsumer v,float size,float z,float u0,float v0,float u1,float v1,float r,float g,float b,float a){var m=pose.last().pose();float h=size/2;v.addVertex(m,-h,-h,z).setUv(u0,v1).setColor(r,g,b,a);v.addVertex(m,h,-h,z).setUv(u1,v1).setColor(r,g,b,a);v.addVertex(m,h,h,z).setUv(u1,v0).setColor(r,g,b,a);v.addVertex(m,-h,h,z).setUv(u0,v0).setColor(r,g,b,a);}
    // UtilsFX.renderQuadCentered has its own UV winding; retain it exactly for area corners.
    private static void sourceQuad(PoseStack pose,VertexConsumer v,float size,float r,float g,float b,float a){var m=pose.last().pose();float h=size/2;v.addVertex(m,-h,h,0).setUv(1,1).setColor(r,g,b,a);v.addVertex(m,h,h,0).setUv(1,0).setColor(r,g,b,a);v.addVertex(m,h,-h,0).setUv(0,0).setColor(r,g,b,a);v.addVertex(m,-h,-h,0).setUv(0,1).setColor(r,g,b,a);}
    private static void side(PoseStack pose,VertexConsumer v,float x0,float y0,float x1,float y1,float u0,float v0,float u1,float v1,float c,float a){var m=pose.last().pose();v.addVertex(m,x0,y0,-.05f).setUv(u0,v0).setColor(c,c,c,a);v.addVertex(m,x1,y1,-.05f).setUv(u1,v1).setColor(c,c,c,a);v.addVertex(m,x1,y1,0).setUv(u1,v1).setColor(c,c,c,a);v.addVertex(m,x0,y0,0).setUv(u0,v0).setColor(c,c,c,a);}
    /** FXDispatcher.spark: original eight-frame red spark on a powered seal. */
    private static final class RedstoneSpark extends net.minecraft.client.particle.TextureSheetParticle {
        private final int first;private final boolean flipped;
        RedstoneSpark(net.minecraft.client.multiplayer.ClientLevel level,SealPos pos){super(level,pos.pos.getX()+.5+pos.face.getStepX()*.66,pos.pos.getY()+.5+pos.face.getStepY()*.66,pos.pos.getZ()+.5+pos.face.getStepZ()*.66,0,0,0);xd=yd=zd=0;lifetime=5+random.nextInt(5);first=8+random.nextInt(3)*16;flipped=random.nextBoolean();quadSize=.2f;alpha=1;setColor(.8f-random.nextFloat()*.2f,0,0);}
        private int frame(){return first+Math.min(7,age*8/lifetime);}
        @Override protected float getU0(){return (frame()%16+(flipped?1:0))/16f;}
        @Override protected float getU1(){return (frame()%16+(flipped?0:1))/16f;}
        @Override protected float getV0(){return frame()/16/16f;}
        @Override protected float getV1(){return (frame()/16+1)/16f;}
        @Override public net.minecraft.client.particle.ParticleRenderType getRenderType(){return thaumcraft.port.client.SalisMundusParticle.legacyLayer(true);}
    }
    private SealWorldRenderer(){super("unused",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,0,false,false,()->{},()->{});}
}
