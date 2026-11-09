package thaumcraft.port.mob.client;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
/** Original ModelKnightArmor, shared by wearable crimson plate and inhabited zombies. */
public final class KnightArmorModel extends HumanoidModel<LivingEntity>{
    public KnightArmorModel(boolean inner){super(layer(inner).bakeRoot());}
    public static LayerDefinition layer(boolean inner){var mesh=new MeshDefinition();var root=mesh.getRoot();
        var head=root.addOrReplaceChild("head",CubeListBuilder.create(),PartPose.ZERO);
        root.addOrReplaceChild("hat",CubeListBuilder.create(),PartPose.ZERO);
        var body=root.addOrReplaceChild("body",CubeListBuilder.create(),PartPose.ZERO);
        var right_arm=root.addOrReplaceChild("right_arm",CubeListBuilder.create(),PartPose.offset(-5,2,0));
        var left_arm=root.addOrReplaceChild("left_arm",CubeListBuilder.create(),PartPose.offset(5,2,0));
        var right_leg=root.addOrReplaceChild("right_leg",CubeListBuilder.create(),PartPose.offset(-1.9f,12,0));
        var left_leg=root.addOrReplaceChild("left_leg",CubeListBuilder.create(),PartPose.offset(1.9f,12,0));
        head.addOrReplaceChild("Helmet",CubeListBuilder.create().texOffs(41,8).addBox(-4.5f, -9.0f, -4.5f, 9, 9, 9),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        body.addOrReplaceChild("Mbelt",CubeListBuilder.create().texOffs(56,55).addBox(-4.0f, 8.0f, -3.0f, 8, 4, 1),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        body.addOrReplaceChild("MbeltL",CubeListBuilder.create().texOffs(76,44).addBox(4.0f, 8.0f, -3.0f, 1, 3, 6),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        body.addOrReplaceChild("MbeltR",CubeListBuilder.create().texOffs(76,44).addBox(-5.0f, 8.0f, -3.0f, 1, 3, 6),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        if(!inner) body.addOrReplaceChild("Tabbard",CubeListBuilder.create().texOffs(114,52).addBox(-3.0f, 1.2f, -3.5f, 6, 10, 1),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        if(!inner) body.addOrReplaceChild("CloakAtL",CubeListBuilder.create().texOffs(0,43).addBox(2.5f, 1.0f, 2.0f, 2, 1, 3),PartPose.offsetAndRotation(0,0,0,0.1396263f, 0.0f, 0.0f));
        if(!inner) body.addOrReplaceChild("Backplate",CubeListBuilder.create().texOffs(36,45).addBox(-4.0f, 1.0f, 2.0f, 8, 11, 2),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        if(!inner) body.addOrReplaceChild("Cloak1",CubeListBuilder.create().texOffs(0,47).addBox(0.0f, 0.0f, 0.0f, 9, 12, 1),PartPose.offsetAndRotation(-4.5f, 1.3f, 4.2f,0.1396263f, 0.0f, 0.0f));
        if(!inner) body.addOrReplaceChild("Cloak2",CubeListBuilder.create().texOffs(0,59).addBox(0.0f, 11.7f, -2.0f, 9, 4, 1),PartPose.offsetAndRotation(-4.5f, 1.3f, 4.2f,0.3069452f, 0.0f, 0.0f));
        if(!inner) body.addOrReplaceChild("Cloak3",CubeListBuilder.create().texOffs(0,59).addBox(0.0f, 15.2f, -4.2f, 9, 4, 1),PartPose.offsetAndRotation(-4.5f, 1.3f, 4.2f,0.4465716f, 0.0f, 0.0f));
        if(!inner) body.addOrReplaceChild("CloakAtR",CubeListBuilder.create().texOffs(0,43).addBox(-4.5f, 1.0f, 2.0f, 2, 1, 3),PartPose.offsetAndRotation(0,0,0,0.1396263f, 0.0f, 0.0f));
        if(!inner) body.addOrReplaceChild("Chestplate",CubeListBuilder.create().texOffs(56,45).addBox(-4.0f, 1.0f, -3.0f, 8, 7, 1),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        right_arm.addOrReplaceChild("ShoulderR1",CubeListBuilder.create().texOffs(0,19).addBox(-3.3f, 3.5f, -2.5f, 1, 1, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.7853982f));
        right_arm.addOrReplaceChild("GauntletR",CubeListBuilder.create().texOffs(100,26).addBox(-3.5f, 3.5f, -2.5f, 2, 6, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        right_arm.addOrReplaceChild("GauntletstrapR1",CubeListBuilder.create().texOffs(84,31).addBox(-1.5f, 3.5f, -2.5f, 3, 1, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        right_arm.addOrReplaceChild("GauntletstrapR2",CubeListBuilder.create().texOffs(84,31).addBox(-1.5f, 6.5f, -2.5f, 3, 1, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        right_arm.addOrReplaceChild("ShoulderR",CubeListBuilder.create().texOffs(56,35).addBox(-3.5f, -2.5f, -2.5f, 5, 5, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        right_arm.addOrReplaceChild("ShoulderR0",CubeListBuilder.create().texOffs(0,0).addBox(-4.3f, -1.5f, -3.0f, 3, 5, 6),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.7853982f));
        right_arm.addOrReplaceChild("ShoulderR2",CubeListBuilder.create().texOffs(0,11).addBox(-2.3f, 3.5f, -3.0f, 1, 2, 6),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.7853982f));
        left_arm.addOrReplaceChild("ShoulderL1",CubeListBuilder.create().texOffs(0,19).mirror().addBox(2.3f, 3.5f, -2.5f, 1, 1, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -0.7853982f));
        left_arm.addOrReplaceChild("GauntletL",CubeListBuilder.create().texOffs(114,26).addBox(1.5f, 3.5f, -2.5f, 2, 6, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        left_arm.addOrReplaceChild("GauntletstrapL1",CubeListBuilder.create().texOffs(84,31).mirror().addBox(-1.5f, 3.5f, -2.5f, 3, 1, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        left_arm.addOrReplaceChild("GauntletstrapL2",CubeListBuilder.create().texOffs(84,31).mirror().addBox(-1.5f, 6.5f, -2.5f, 3, 1, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        left_arm.addOrReplaceChild("ShoulderL",CubeListBuilder.create().texOffs(56,35).addBox(-1.5f, -2.5f, -2.5f, 5, 5, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        left_arm.addOrReplaceChild("ShoulderL0",CubeListBuilder.create().texOffs(0,0).addBox(1.3f, -1.5f, -3.0f, 3, 5, 6),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -0.7853982f));
        left_arm.addOrReplaceChild("ShoulderL2",CubeListBuilder.create().texOffs(0,11).addBox(1.3f, 3.5f, -3.0f, 1, 2, 6),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -0.7853982f));
        right_leg.addOrReplaceChild("SidepanelR3",CubeListBuilder.create().texOffs(116,13).addBox(-3.0f, 2.5f, -2.5f, 1, 4, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.1396263f));
        right_leg.addOrReplaceChild("SidepanelR2",CubeListBuilder.create().texOffs(114,5).mirror().addBox(-2.0f, 2.5f, -2.5f, 2, 3, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.1396263f));
        left_leg.addOrReplaceChild("SidepanelL2",CubeListBuilder.create().texOffs(114,5).addBox(0.0f, 2.5f, -2.5f, 2, 3, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -0.1396263f));
        right_leg.addOrReplaceChild("SidepanelR0",CubeListBuilder.create().texOffs(96,14).addBox(-3.0f, -0.5f, -2.5f, 5, 3, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.1396263f));
        left_leg.addOrReplaceChild("SidepanelL0",CubeListBuilder.create().texOffs(96,14).addBox(-2.0f, -0.5f, -2.5f, 5, 3, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -0.1396263f));
        right_leg.addOrReplaceChild("SidepanelR1",CubeListBuilder.create().texOffs(96,7).mirror().addBox(0.0f, 2.5f, -2.5f, 2, 2, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.1396263f));
        left_leg.addOrReplaceChild("SidepanelL3",CubeListBuilder.create().texOffs(116,13).addBox(2.0f, 2.5f, -2.5f, 1, 4, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -0.1396263f));
        left_leg.addOrReplaceChild("SidepanelL1",CubeListBuilder.create().texOffs(96,7).addBox(-2.0f, 2.5f, -2.5f, 2, 2, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -0.1396263f));
        if(!inner) body.addOrReplaceChild("Frontcloth1",CubeListBuilder.create().texOffs(120,39).addBox(0.0f, 0.0f, 0.0f, 6, 8, 1),PartPose.offsetAndRotation(-3.0f, 11.0f, -3.5f,-0.1047198f, 0.0f, 0.0f));
        if(!inner) body.addOrReplaceChild("Frontcloth2",CubeListBuilder.create().texOffs(100,37).addBox(0.0f, 7.5f, 1.8f, 6, 3, 1),PartPose.offsetAndRotation(-3.0f, 11.0f, -3.5f,-0.3316126f, 0.0f, 0.0f));
        return LayerDefinition.create(mesh,128,64);
    }
    public void animateCloth(float walk,float amount){
        head.xScale=head.yScale=head.zScale=young?1:1.01f;
        if(!body.hasChild("Frontcloth1"))return;
        float bend=Math.min(Mth.cos(walk*.6662f)*1.4f*amount,Mth.cos(walk*.6662f+Mth.PI)*1.4f*amount);
        body.getChild("Frontcloth1").xRot=bend-.1047198f;body.getChild("Frontcloth2").xRot=bend-.3316126f;
        body.getChild("Cloak1").xRot=-bend/2+.1396263f;body.getChild("Cloak2").xRot=-bend/2+.3069452f;body.getChild("Cloak3").xRot=-bend/2+.4465716f;
    }
}
