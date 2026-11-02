package thaumcraft.port.artifice.construct.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import thaumcraft.port.artifice.construct.*;

/** BETA26 ModelCrossbow: original boxes, pivots, hierarchy and 64x32 UVs.
 * Source mirror flags were set after box construction and do not mirror those existing boxes. */
public final class ConstructCrossbowModel extends EntityModel<TurretEntity> {
    private final ModelPart root;
    public ConstructCrossbowModel(ModelPart root){this.root=root;}
    public static LayerDefinition layer(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        var crossbow=root.addOrReplaceChild("crossbow",CubeListBuilder.create().texOffs(28,14).addBox(-2.0f, 0.0f, -7.0f, 4, 2, 14),PartPose.offsetAndRotation(0.0f, 10.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        var basebarr=crossbow.addOrReplaceChild("basebarr",CubeListBuilder.create().texOffs(40,23).addBox(-1.0f, 0.0f, 7.0f, 1, 2, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, -0.1396263f, 0.0f));
        var basebarl=crossbow.addOrReplaceChild("basebarl",CubeListBuilder.create().texOffs(40,23).addBox(0.0f, 0.0f, 7.0f, 1, 2, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.1396263f, 0.0f));
        var barrel=crossbow.addOrReplaceChild("barrel",CubeListBuilder.create().texOffs(20,28).addBox(-1.0f, -1.0f, -8.0f, 2, 2, 2),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        var basebarcross=crossbow.addOrReplaceChild("basebarcross",CubeListBuilder.create().texOffs(0,13).addBox(-2.0f, 0.5f, 10.0f, 4, 1, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        var ammobox=crossbow.addOrReplaceChild("ammobox",CubeListBuilder.create().texOffs(38,0).addBox(-2.0f, -5.0f, -6.0f, 4, 5, 9),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        var loadbarcross=crossbow.addOrReplaceChild("loadbarcross",CubeListBuilder.create().texOffs(0,13).addBox(-2.0f, -8.5f, -0.5f, 4, 1, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, -0.5585054f, 0.0f, 0.0f));
        var loadbarl=crossbow.addOrReplaceChild("loadbarl",CubeListBuilder.create().texOffs(0,15).addBox(2.0f, -9.0f, -1.0f, 1, 11, 2),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, -0.5585054f, 0.0f, 0.0f));
        var loadbarr=crossbow.addOrReplaceChild("loadbarr",CubeListBuilder.create().texOffs(0,15).addBox(-3.0f, -9.0f, -1.0f, 1, 11, 2),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, -0.5585054f, 0.0f, 0.0f));
        var crossl1=crossbow.addOrReplaceChild("crossl1",CubeListBuilder.create().texOffs(0,0).addBox(0.0f, 0.0f, -6.0f, 5, 2, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, -0.2443461f, 0.0f));
        var crossl2=crossbow.addOrReplaceChild("crossl2",CubeListBuilder.create().texOffs(0,0).addBox(4.0f, 0.0f, -5.0f, 3, 2, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, -0.2443461f, 0.0f));
        var crossl3=crossbow.addOrReplaceChild("crossl3",CubeListBuilder.create().texOffs(0,0).addBox(6.0f, 0.0f, -4.0f, 2, 2, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, -0.2443461f, 0.0f));
        var crossr1=crossbow.addOrReplaceChild("crossr1",CubeListBuilder.create().texOffs(0,0).addBox(-5.0f, 0.0f, -6.0f, 5, 2, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.2443461f, 0.0f));
        var crossr2=crossbow.addOrReplaceChild("crossr2",CubeListBuilder.create().texOffs(0,0).addBox(-7.0f, 0.0f, -5.0f, 3, 2, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.2443461f, 0.0f));
        var crossr3=crossbow.addOrReplaceChild("crossr3",CubeListBuilder.create().texOffs(0,0).addBox(-8.0f, 0.0f, -4.0f, 2, 2, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f, 0.0f, 0.2443461f, 0.0f));
        var leg2=root.addOrReplaceChild("leg2",CubeListBuilder.create().texOffs(20,10).addBox(-1.0f, 1.0f, -1.0f, 2, 13, 2),PartPose.offsetAndRotation(0.0f, 12.0f, 0.0f, 0.5235988f, 1.570796f, 0.0f));
        var tripod=root.addOrReplaceChild("tripod",CubeListBuilder.create().texOffs(13,0).addBox(-1.5f, 0.0f, -1.5f, 3, 2, 3),PartPose.offsetAndRotation(0.0f, 12.0f, 0.0f, 0.0f, 0.0f, 0.0f));
        var leg3=root.addOrReplaceChild("leg3",CubeListBuilder.create().texOffs(20,10).addBox(-1.0f, 1.0f, -1.0f, 2, 13, 2),PartPose.offsetAndRotation(0.0f, 12.0f, 0.0f, 0.5235988f, 3.141593f, 0.0f));
        var leg4=root.addOrReplaceChild("leg4",CubeListBuilder.create().texOffs(20,10).addBox(-1.0f, 1.0f, -1.0f, 2, 13, 2),PartPose.offsetAndRotation(0.0f, 12.0f, 0.0f, 0.5235988f, 4.712389f, 0.0f));
        var leg1=root.addOrReplaceChild("leg1",CubeListBuilder.create().texOffs(20,10).addBox(-1.0f, 1.0f, -1.0f, 2, 13, 2),PartPose.offsetAndRotation(0.0f, 12.0f, 0.0f, 0.5235988f, 0.0f, 0.0f));
        return LayerDefinition.create(mesh,64,32);
    }
    @Override public void setupAnim(TurretEntity entity,float walk,float amount,float age,float yaw,float pitch){
        var body=root.getChild("crossbow");body.yRot=yaw/57.295776f;body.xRot=pitch/57.295776f;
        float swing=entity.shotProgress(age-entity.tickCount),load=entity.loadProgress(age-entity.tickCount);
        float bend=Mth.sin(Mth.sqrt(swing)*Mth.PI*2)*.2f;
        for(int i=1;i<=3;i++){body.getChild("crossl"+i).yRot=-.2f+bend;body.getChild("crossr"+i).yRot=.2f-bend;}
        float lever=-.5f+Mth.sin(Mth.sqrt(load)*Mth.PI*2)*.5f;
        for(String name:new String[]{"loadbarl","loadbarr","loadbarcross"})body.getChild(name).xRot=lever;
        boolean cart=entity.getVehicle() instanceof net.minecraft.world.entity.vehicle.AbstractMinecart;
        for(int i=1;i<=4;i++){var leg=root.getChild("leg"+i);leg.xRot=cart?.1f:.5f;leg.y=cart?4:12;}
    }
    @Override public void renderToBuffer(PoseStack pose,VertexConsumer consumer,int light,int overlay,int color){root.render(pose,consumer,light,overlay,color);}
}
