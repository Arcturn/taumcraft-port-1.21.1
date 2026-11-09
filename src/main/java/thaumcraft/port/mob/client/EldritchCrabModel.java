package thaumcraft.port.mob.client;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import thaumcraft.port.mob.EldritchCrab;
/** TC6 crab geometry, UVs, walking legs and independently moving claws. */
public final class EldritchCrabModel extends HierarchicalModel<EldritchCrab>{
    private final ModelPart root;
    public EldritchCrabModel(ModelPart root){this.root=root;}
    @Override public ModelPart root(){return root;}
    public static LayerDefinition layer(){var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("TailHelm",CubeListBuilder.create().texOffs(0,0).addBox(-4.5f, -4.5f, -0.4f, 9, 9, 9),PartPose.offsetAndRotation(0.0f, 18.0f, 0.0f,0.1047198f, 0.0f, 0.0f));
        root.addOrReplaceChild("TailBare",CubeListBuilder.create().texOffs(64,0).addBox(-4.0f, -4.0f, -0.4f, 8, 8, 8),PartPose.offsetAndRotation(0.0f, 18.0f, 0.0f,0.1047198f, 0.0f, 0.0f));
        root.addOrReplaceChild("RClaw1",CubeListBuilder.create().texOffs(0,47).addBox(-2.0f, -1.0f, -5.066667f, 4, 3, 5),PartPose.offsetAndRotation(-6.0f, 15.5f, -10.0f,0,0,0));
        root.addOrReplaceChild("Head1",CubeListBuilder.create().texOffs(0,38).addBox(-2.0f, -1.5f, -9.066667f, 4, 4, 1),PartPose.offsetAndRotation(0.0f, 18.0f, 0.0f,0,0,0));
        root.addOrReplaceChild("RClaw0",CubeListBuilder.create().texOffs(0,55).addBox(-2.0f, -2.5f, -3.066667f, 4, 5, 3),PartPose.offsetAndRotation(-6.0f, 17.0f, -7.0f,0,0,0));
        root.addOrReplaceChild("RClaw2",CubeListBuilder.create().texOffs(14,54).addBox(-1.5f, -1.0f, -4.066667f, 3, 2, 5),PartPose.offsetAndRotation(-6.0f, 18.5f, -10.0f,0.3141593f, 0.0f, 0.0f));
        root.addOrReplaceChild("RArm",CubeListBuilder.create().texOffs(44,4).addBox(-1.0f, -1.0f, -5.066667f, 2, 2, 6),PartPose.offsetAndRotation(-3.0f, 17.0f, -4.0f,0.0f, 0.7504916f, 0.0f));
        root.addOrReplaceChild("LClaw2",CubeListBuilder.create().texOffs(14,54).addBox(-1.5f, -1.0f, -4.066667f, 3, 2, 5),PartPose.offsetAndRotation(6.0f, 18.5f, -10.0f,0.3141593f, 0.0f, 0.0f));
        root.addOrReplaceChild("LClaw1",CubeListBuilder.create().texOffs(0,47).mirror().addBox(-2.0f, -1.0f, -5.066667f, 4, 3, 5),PartPose.offsetAndRotation(6.0f, 15.5f, -10.0f,0,0,0));
        root.addOrReplaceChild("LClaw0",CubeListBuilder.create().texOffs(0,55).mirror().addBox(-2.0f, -2.5f, -3.066667f, 4, 5, 3),PartPose.offsetAndRotation(6.0f, 17.0f, -7.0f,0,0,0));
        root.addOrReplaceChild("LArm",CubeListBuilder.create().texOffs(44,4).addBox(-1.0f, -1.0f, -4.066667f, 2, 2, 6),PartPose.offsetAndRotation(4.0f, 17.0f, -5.0f,0.0f, -0.7504916f, 0.0f));
        root.addOrReplaceChild("Torso",CubeListBuilder.create().texOffs(0,18).addBox(-3.5f, -3.5f, -6.066667f, 7, 7, 6),PartPose.offsetAndRotation(0.0f, 18.0f, 0.0f,0.0523599f, 0.0f, 0.0f));
        root.addOrReplaceChild("Head0",CubeListBuilder.create().texOffs(0,31).addBox(-2.5f, -2.0f, -8.066667f, 5, 5, 2),PartPose.offsetAndRotation(0.0f, 18.0f, 0.0f,0,0,0));
        root.addOrReplaceChild("RRLeg1",CubeListBuilder.create().texOffs(36,4).addBox(-4.5f, 1.0f, -0.9f, 2, 5, 2),PartPose.offsetAndRotation(-4.0f, 20.0f, -1.5f,0,0,0));
        root.addOrReplaceChild("RFLeg1",CubeListBuilder.create().texOffs(36,4).addBox(-5.0f, 1.0f, -1.066667f, 2, 5, 2),PartPose.offsetAndRotation(-4.0f, 20.0f, -3.5f,0,0,0));
        root.addOrReplaceChild("LRLeg1",CubeListBuilder.create().texOffs(36,4).addBox(2.5f, 1.0f, -0.9f, 2, 5, 2),PartPose.offsetAndRotation(4.0f, 20.0f, -1.5f,0,0,0));
        root.addOrReplaceChild("LFLeg1",CubeListBuilder.create().texOffs(36,4).addBox(3.0f, 1.0f, -1.066667f, 2, 5, 2),PartPose.offsetAndRotation(4.0f, 20.0f, -3.5f,0,0,0));
        root.addOrReplaceChild("RRLeg0",CubeListBuilder.create().texOffs(36,0).addBox(-4.5f, -1.0f, -0.9f, 6, 2, 2),PartPose.offsetAndRotation(-4.0f, 20.0f, -1.5f,0,0,0));
        root.addOrReplaceChild("RFLeg0",CubeListBuilder.create().texOffs(36,0).addBox(-5.0f, -1.0f, -1.066667f, 6, 2, 2),PartPose.offsetAndRotation(-4.0f, 20.0f, -3.5f,0,0,0));
        root.addOrReplaceChild("LFLeg0",CubeListBuilder.create().texOffs(36,0).addBox(-1.0f, -1.0f, -1.066667f, 6, 2, 2),PartPose.offsetAndRotation(4.0f, 20.0f, -3.5f,0,0,0));
        root.addOrReplaceChild("LRLeg0",CubeListBuilder.create().texOffs(36,0).addBox(-1.5f, -1.0f, -0.9f, 6, 2, 2),PartPose.offsetAndRotation(4.0f, 20.0f, -1.5f,0,0,0));
        return LayerDefinition.create(mesh,128,64);
    }
    @Override public void setupAnim(EldritchCrab entity,float walk,float amount,float age,float yaw,float pitch){
        root.getAllParts().forEach(ModelPart::resetPose);
        for(String side:new String[]{"R","L"})for(String end:new String[]{"R","F"})for(int segment=0;segment<2;segment++){
            var leg=root.getChild(side+end+"Leg"+segment);float sign=side.equals("R")?1:-1;
            float move=-Mth.cos(walk*.6662f*2+(end.equals("F")?Mth.PI:0))*.4f*amount;
            leg.yRot=sign*((end.equals("R")?.2094395f:-.2094395f)+move);leg.zRot=sign*(.4363323f+move);
        }
        for(String tail:new String[]{"TailHelm","TailBare"}){var part=root.getChild(tail);part.yRot=Mth.cos(walk*.6662f)*amount*.25f;part.zRot=Mth.cos(walk*.6662f)*amount*.125f;}
        root.getChild("TailHelm").visible=entity.hasHelm();root.getChild("TailBare").visible=!entity.hasHelm();
        root.getChild("RClaw2").xRot=.3141593f-Mth.sin(entity.tickCount/4f)*.25f;root.getChild("LClaw2").xRot=.3141593f+Mth.sin(entity.tickCount/4.1f)*.25f;
        root.getChild("RClaw1").xRot=Mth.sin(entity.tickCount/4f)*.125f;root.getChild("LClaw1").xRot=-Mth.sin(entity.tickCount/4.1f)*.125f;
    }
}
