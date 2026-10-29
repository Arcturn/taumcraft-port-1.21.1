package thaumcraft.port.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.essentia.*;

public final class EssentiaRenderers {
    private static ResourceLocation id(String p){return ResourceLocation.fromNamespaceAndPath("thaumcraft",p);}
    private static final ModelLayerLocation CENTRIFUGE=new ModelLayerLocation(id("centrifuge"),"main"),VALVE=new ModelLayerLocation(id("valve"),"main"),BELLOWS=new ModelLayerLocation(id("bellows"),"main"),JAR_LID=new ModelLayerLocation(id("jar_lid"),"main"),NOZZLE=new ModelLayerLocation(id("bellows_nozzle"),"main");
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e){
        e.registerLayerDefinition(JAR_LID,()->{
            var mesh=new MeshDefinition();var root=mesh.getRoot();
            root.addOrReplaceChild("brace",CubeListBuilder.create().mirror().texOffs(32,24).addBox(-3,-14,-3,6,2,6),PartPose.ZERO);
            root.addOrReplaceChild("collar",CubeListBuilder.create().mirror().texOffs(0,23).addBox(-2,-16,-2,4,2,4),PartPose.ZERO);
            return LayerDefinition.create(mesh,64,32);
        });
        e.registerLayerDefinition(NOZZLE,()->{
            var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("nozzle",CubeListBuilder.create().mirror().texOffs(106,42).addBox(2.5f,-2,-2,5,4,4).texOffs(106,51).addBox(7,-2.5f,-2.5f,1,5,5),PartPose.ZERO);
            return LayerDefinition.create(mesh,128,64);
        });
        e.registerLayerDefinition(BELLOWS,()->{
            var mesh=new MeshDefinition();var root=mesh.getRoot();
            root.addOrReplaceChild("bottom",CubeListBuilder.create().mirror().texOffs(0,0).addBox(-6,-8,-6,12,2,12),PartPose.ZERO);
            root.addOrReplaceChild("top",CubeListBuilder.create().mirror().texOffs(0,0).addBox(-6,6,-6,12,2,12),PartPose.ZERO);
            root.addOrReplaceChild("middle",CubeListBuilder.create().mirror().texOffs(0,0).addBox(-6,-1,-6,12,2,12).texOffs(0,36).addBox(-2,-2,6,4,4,2),PartPose.ZERO);
            root.addOrReplaceChild("bag",CubeListBuilder.create().mirror().texOffs(48,0).addBox(-10,-12.03333f,-10,20,24,20),PartPose.ZERO);
            return LayerDefinition.create(mesh,128,128);
        });
        e.registerLayerDefinition(CENTRIFUGE,()->{
            MeshDefinition mesh=new MeshDefinition();var root=mesh.getRoot();
            root.addOrReplaceChild("rotor",CubeListBuilder.create().mirror().texOffs(16,0).addBox(-4,-1,-1,8,2,2).texOffs(0,16).addBox(4,-3,-2,4,6,4).addBox(-8,-3,-2,4,6,4).texOffs(0,0).addBox(-1.5f,-4,-1.5f,3,8,3),PartPose.ZERO);
            return LayerDefinition.create(mesh,64,32);
        });
        e.registerLayerDefinition(VALVE,()->{
            MeshDefinition mesh=new MeshDefinition();var root=mesh.getRoot();
            root.addOrReplaceChild("rod",CubeListBuilder.create().mirror().texOffs(0,10).addBox(-1,2,-1,2,2,2),PartPose.ZERO);
            root.addOrReplaceChild("ring",CubeListBuilder.create().mirror().texOffs(0,0).addBox(-2,4,-2,4,1,4),PartPose.ZERO);
            return LayerDefinition.create(mesh,64,32);
        });
    }
    public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(EssentiaContent.BELLOWS_ENTITY.get(),Bellows::new);e.registerBlockEntityRenderer(EssentiaContent.THAUMATORIUM_ENTITY.get(),Thaumatorium::new);e.registerBlockEntityRenderer(EssentiaContent.JAR_ENTITY.get(),Jar::new);e.registerBlockEntityRenderer(EssentiaContent.CENTRIFUGE_ENTITY.get(),Centrifuge::new);e.registerBlockEntityRenderer(EssentiaContent.TUBE_ENTITY.get(),Tube::new);}
    private static void face(PoseStack pose,VertexConsumer out,float[][] points,int color,int light,float u0,float v0,float u1,float v1,float nx,float ny,float nz){
        float[][] uv={{u0,v1},{u1,v1},{u1,v0},{u0,v0}};
        for(int i=0;i<4;i++)out.addVertex(pose.last().pose(),points[i][0],points[i][1],points[i][2]).setColor(color).setUv(uv[i][0],uv[i][1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(),nx,ny,nz);
    }
    static void label(PoseStack pose,MultiBufferSource buffers,Aspect aspect,float y,float z,int light){
        if(aspect==null)return;
        face(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(id("textures/models/label.png"))),new float[][]{{.3f,y,z},{.7f,y,z},{.7f,y+.35f,z},{.3f,y+.35f,z}},0xffffffff,light,0,0,1,1,0,0,-1);
        face(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(id("textures/aspects/"+aspect.id()+".png"))),new float[][]{{.375f,y+.05f,z-.001f},{.625f,y+.05f,z-.001f},{.625f,y+.3f,z-.001f},{.375f,y+.3f,z-.001f}},0xff000000|aspect.color(),light,0,0,1,1,0,0,-1);
    }
    public static final class Jar implements BlockEntityRenderer<JarEntity> {
        private final ModelPart lid;
        public Jar(BlockEntityRendererProvider.Context c){lid=c.bakeLayer(JAR_LID);}
        public void render(JarEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(switch(be.labelFacing){case SOUTH->180;case EAST->270;case WEST->90;default->0;}));pose.translate(-.5,0,-.5);
            label(pose,buffers,be.tank.filter(),.2f,be.alembic?.12f:.1865f,light);pose.popPose();
            if(!be.alembic){
                pose.pushPose();pose.translate(.5,.01,.5);pose.mulPose(Axis.XP.rotationDegrees(180));var lidOut=buffers.getBuffer(RenderType.entityCutout(id("textures/models/jarbrine.png")));
                if(be.braced){pose.pushPose();pose.scale(1.001f,1.001f,1.001f);lid.getChild("brace").render(pose,lidOut,light,overlay);pose.popPose();}
                var above=be.neighbour(Direction.UP);if(above!=null&&above.connects(Direction.DOWN)){pose.scale(.9f,1,.9f);lid.getChild("collar").render(pose,lidOut,light,overlay);}pose.popPose();
            }
            if(be.alembic||be.tank.aspect()==null||be.tank.amount()==0)return;
            var sprite=Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(id("block/animatedglow"));
            float lo=.25f,hi=.75f,b=.0625f,t=b+.625f*be.tank.amount()/250f;
            int color=0xc8000000|be.tank.aspect().color();var out=buffers.getBuffer(RenderType.translucent());
            float[][][] faces={{{lo,b,lo},{lo,b,hi},{lo,t,hi},{lo,t,lo}},{{hi,b,hi},{hi,b,lo},{hi,t,lo},{hi,t,hi}},{{hi,b,lo},{lo,b,lo},{lo,t,lo},{hi,t,lo}},{{lo,b,hi},{hi,b,hi},{hi,t,hi},{lo,t,hi}},{{lo,t,lo},{lo,t,hi},{hi,t,hi},{hi,t,lo}},{{lo,b,hi},{lo,b,lo},{hi,b,lo},{hi,b,hi}}};
            int[][] normals={{-1,0,0},{1,0,0},{0,0,-1},{0,0,1},{0,1,0},{0,-1,0}};
            for(int i=0;i<6;i++)face(pose,out,faces[i],color,LightTexture.FULL_BRIGHT,sprite.getU0(),sprite.getV0(),sprite.getU1(),sprite.getV1(),normals[i][0],normals[i][1],normals[i][2]);
        }
    }
    public static final class Centrifuge implements BlockEntityRenderer<CentrifugeEntity> {
        private final ModelPart rotor;
        public Centrifuge(BlockEntityRendererProvider.Context c){rotor=c.bakeLayer(CENTRIFUGE).getChild("rotor");}
        public void render(CentrifugeEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(Axis.YP.rotationDegrees(be.previousRotation+(be.rotation-be.previousRotation)*partial));
            rotor.render(pose,buffers.getBuffer(RenderType.entityCutout(id("textures/models/centrifuge.png"))),light,overlay);pose.popPose();
        }
    }
    public static final class Tube implements BlockEntityRenderer<TubeEntity> {
        private final ModelPart model;
        public Tube(BlockEntityRendererProvider.Context c){model=c.bakeLayer(VALVE);}
        public void render(TubeEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            if(be instanceof BufferEntity buffer){for(Direction d:Direction.values())if(buffer.chokes[d.ordinal()]>0){pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(d.getRotation());pose.translate(-.5,-.5,-.5);int color=buffer.chokes[d.ordinal()]==1?0xffffaa00:0xffdd3344;face(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(id("textures/models/label.png"))),new float[][]{{.35f,.756f,.35f},{.35f,.756f,.65f},{.65f,.756f,.65f},{.65f,.756f,.35f}},color,light,0,0,1,1,0,1,0);pose.popPose();}return;}
            if(be.kind==TubeBlock.Kind.FILTER){label(pose,buffers,be.tank.filter(),.35f,.31f,light);return;}
            if(be.kind!=TubeBlock.Kind.VALVE&&be.kind!=TubeBlock.Kind.ONEWAY)return;
            pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(be.facing().getRotation());
            var out=buffers.getBuffer(RenderType.entityCutout(id("textures/models/valve.png")));
            if(be.kind==TubeBlock.Kind.VALVE){float rotation=be.previousRotation+(be.rotation-be.previousRotation)*partial;pose.mulPose(Axis.YP.rotationDegrees(-rotation*1.5f));pose.translate(0,-.03-rotation/360*.09,0);model.getChild("ring").render(pose,out,light,overlay);pose.scale(.75f,1,.75f);model.getChild("rod").render(pose,out,light,overlay);}
            else {pose.scale(2,2,2);pose.translate(0,-.32,0);model.getChild("rod").render(pose,out,light,overlay,0xff7380ff);}
            pose.popPose();
        }
    }
    private static void facing(PoseStack pose,Direction d){if(d.getAxis().isHorizontal())pose.mulPose(Axis.YP.rotationDegrees(switch(d){case NORTH->180;case EAST->90;case WEST->270;default->0;}));else pose.mulPose(Axis.XP.rotationDegrees(d==Direction.UP?-90:90));}
    public static final class Bellows implements BlockEntityRenderer<BellowsEntity>{
        private final ModelPart model,nozzle;
        public Bellows(BlockEntityRendererProvider.Context c){model=c.bakeLayer(BELLOWS);nozzle=c.bakeLayer(NOZZLE).getChild("nozzle");}
        public void render(BellowsEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            float scale=be.previousInflation+(be.inflation-be.previousInflation)*partial;float travel=(1-(.125f+scale*.875f))/2;
            Direction direction=be.getBlockState().getValue(BellowsBlock.FACING);var target=be.getBlockPos().relative(direction);
            if(be.getLevel()!=null&&be.getLevel().hasChunkAt(target)&&be.getLevel().getBlockEntity(target) instanceof BufferEntity){
                pose.pushPose();pose.translate(.5+direction.getStepX(),.5+direction.getStepY(),.5+direction.getStepZ());facing(pose,direction.getOpposite());pose.mulPose(Axis.YP.rotationDegrees(-90));nozzle.render(pose,buffers.getBuffer(RenderType.entityCutout(id("textures/models/bore.png"))),light,overlay);pose.popPose();
            }
            pose.pushPose();pose.translate(.5,.5,.5);facing(pose,direction);var out=buffers.getBuffer(RenderType.entityCutout(id("textures/block/bellows.png")));
            model.getChild("middle").render(pose,out,light,overlay);
            pose.pushPose();pose.scale(.5f,(scale+.1f)/2,.5f);model.getChild("bag").render(pose,out,light,overlay);pose.popPose();
            pose.pushPose();pose.translate(0,-travel,0);model.getChild("top").render(pose,out,light,overlay);pose.popPose();
            pose.pushPose();pose.translate(0,travel,0);model.getChild("bottom").render(pose,out,light,overlay);pose.popPose();pose.popPose();
        }
    }
    public static final class Thaumatorium implements BlockEntityRenderer<ThaumatoriumEntity>{
        public Thaumatorium(BlockEntityRendererProvider.Context c){}
        public void render(ThaumatoriumEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            if(be.top()||be.getLevel()==null||be.selected.isEmpty())return;var ids=new java.util.ArrayList<>(be.selected.keySet());var recipe=be.recipe(ids.get((int)(be.getLevel().getGameTime()/20%ids.size())));if(recipe==null)return;
            Direction d=be.front();pose.pushPose();pose.translate(.5+d.getStepX()*.505,1.125,.5+d.getStepZ()*.505);facing(pose,d);pose.scale(.5f,.5f,.5f);
            Minecraft.getInstance().getItemRenderer().renderStatic(recipe.value().result(),net.minecraft.world.item.ItemDisplayContext.FIXED,light,overlay,pose,buffers,be.getLevel(),0);pose.popPose();
        }
        @Override public boolean shouldRenderOffScreen(ThaumatoriumEntity be){return true;}
    }
    private EssentiaRenderers(){}
}
