package thaumcraft.port.infusion.armor.client;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import thaumcraft.port.infusion.armor.FortressArmorItem;
import thaumcraft.port.infusion.recipe.InfusionAugments;

/** TC6 ModelRobe geometry and animated split cloth panels. */
public final class RobeArmorModel extends HumanoidModel<LivingEntity> {
    public RobeArmorModel(boolean inner){super(layer(inner).bakeRoot());}
    public static LayerDefinition layer(boolean inner){
        MeshDefinition mesh=new MeshDefinition();var root=mesh.getRoot();
        var head=root.addOrReplaceChild("head",CubeListBuilder.create(),PartPose.ZERO);
        root.addOrReplaceChild("hat",CubeListBuilder.create(),PartPose.ZERO);
        var body=root.addOrReplaceChild("body",CubeListBuilder.create(),PartPose.ZERO);
        var right_arm=root.addOrReplaceChild("right_arm",CubeListBuilder.create(),PartPose.offset(-5,2,0));
        var left_arm=root.addOrReplaceChild("left_arm",CubeListBuilder.create(),PartPose.offset(5,2,0));
        var right_leg=root.addOrReplaceChild("right_leg",CubeListBuilder.create(),PartPose.offset(-1.9f,12,0));
        var left_leg=root.addOrReplaceChild("left_leg",CubeListBuilder.create(),PartPose.offset(1.9f,12,0));
        head.addOrReplaceChild("Hood1",CubeListBuilder.create().texOffs(16,7).addBox(-4.5f, -9.0f, -4.6f, 9, 9, 9),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        head.addOrReplaceChild("Hood2",CubeListBuilder.create().texOffs(52,13).addBox(-4.0f, -9.7f, 2.0f, 8, 9, 3),PartPose.offsetAndRotation(0,0,0,-0.2268928f, 0.0f, 0.0f));
        head.addOrReplaceChild("Hood3",CubeListBuilder.create().texOffs(52,14).addBox(-3.5f, -10.0f, 3.5f, 7, 8, 3),PartPose.offsetAndRotation(0,0,0,-0.3490659f, 0.0f, 0.0f));
        head.addOrReplaceChild("Hood4",CubeListBuilder.create().texOffs(53,15).addBox(-3.0f, -10.7f, 3.5f, 6, 7, 3),PartPose.offsetAndRotation(0,0,0,-0.5759587f, 0.0f, 0.0f));
        body.addOrReplaceChild("Mbelt",CubeListBuilder.create().texOffs(16,55).addBox(-4.0f, 7.0f, -3.0f, 8, 5, 1),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        body.addOrReplaceChild("MbeltB",CubeListBuilder.create().texOffs(16,55).addBox(-4.0f, 7.0f, -4.0f, 8, 5, 1),PartPose.offsetAndRotation(0,0,0,0.0f, 3.141593f, 0.0f));
        body.addOrReplaceChild("MbeltL",CubeListBuilder.create().texOffs(16,36).addBox(4.0f, 8.0f, -3.0f, 1, 3, 6),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        body.addOrReplaceChild("MbeltR",CubeListBuilder.create().texOffs(16,36).addBox(-5.0f, 8.0f, -3.0f, 1, 3, 6),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        if(inner) left_leg.addOrReplaceChild("Focipouch",CubeListBuilder.create().texOffs(100,20).addBox(3.5f, 0.5f, -2.5f, 3, 6, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -0.122173f));
        if(inner) body.addOrReplaceChild("FrontclothR1",CubeListBuilder.create().texOffs(108,38).addBox(0.0f, 0.0f, 0.0f, 3, 8, 1),PartPose.offsetAndRotation(-3.0f, 11.0f, -2.9f,-0.1047198f, 0.0f, 0.0f));
        if(inner) body.addOrReplaceChild("FrontclothR2",CubeListBuilder.create().texOffs(108,47).addBox(0.0f, 7.5f, 1.7f, 3, 3, 1),PartPose.offsetAndRotation(-3.0f, 11.0f, -2.9f,-0.3316126f, 0.0f, 0.0f));
        if(inner) body.addOrReplaceChild("FrontclothL1",CubeListBuilder.create().texOffs(108,38).mirror().addBox(0.0f, 0.0f, 0.0f, 3, 8, 1),PartPose.offsetAndRotation(0.0f, 11.0f, -2.9f,-0.1047198f, 0.0f, 0.0f));
        if(inner) body.addOrReplaceChild("FrontclothL2",CubeListBuilder.create().texOffs(108,47).mirror().addBox(0.0f, 7.5f, 1.7f, 3, 3, 1),PartPose.offsetAndRotation(0.0f, 11.0f, -2.9f,-0.3316126f, 0.0f, 0.0f));
        if(inner) body.addOrReplaceChild("ClothBackR1",CubeListBuilder.create().texOffs(118,16).mirror().addBox(0.0f, 0.0f, 0.0f, 4, 8, 1),PartPose.offsetAndRotation(-4.0f, 11.5f, 2.9f,0.1047198f, 0.0f, 0.0f));
        if(inner) body.addOrReplaceChild("ClothBackR2",CubeListBuilder.create().texOffs(123,9).addBox(0.0f, 7.8f, -0.9f, 1, 2, 1),PartPose.offsetAndRotation(-4.0f, 11.5f, 2.9f,0.2268928f, 0.0f, 0.0f));
        if(inner) body.addOrReplaceChild("ClothBackR3",CubeListBuilder.create().texOffs(120,12).mirror().addBox(1.0f, 7.8f, -0.9f, 3, 3, 1),PartPose.offsetAndRotation(-4.0f, 11.5f, 2.9f,0.2268928f, 0.0f, 0.0f));
        if(inner) body.addOrReplaceChild("ClothBackL1",CubeListBuilder.create().texOffs(118,16).addBox(0.0f, 0.0f, 0.0f, 4, 8, 1),PartPose.offsetAndRotation(0.0f, 11.5f, 2.9f,0.1047198f, 0.0f, 0.0f));
        if(inner) body.addOrReplaceChild("ClothBackL2",CubeListBuilder.create().texOffs(123,9).mirror().addBox(3.0f, 7.8f, -0.9f, 1, 2, 1),PartPose.offsetAndRotation(0.0f, 11.5f, 2.9f,0.2268928f, 0.0f, 0.0f));
        if(inner) body.addOrReplaceChild("ClothBackL3",CubeListBuilder.create().texOffs(120,12).addBox(0.0f, 7.8f, -0.9f, 3, 3, 1),PartPose.offsetAndRotation(0.0f, 11.5f, 2.9f,0.2268928f, 0.0f, 0.0f));
        if(!inner) body.addOrReplaceChild("Chestplate",CubeListBuilder.create().texOffs(16,25).addBox(-4.0f, 1.0f, -3.0f, 8, 6, 1),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        if(!inner) body.addOrReplaceChild("Chestthing",CubeListBuilder.create().texOffs(56,50).addBox(-2.5f, 1.0f, -4.0f, 5, 7, 1),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        if(!inner) body.addOrReplaceChild("Scroll",CubeListBuilder.create().texOffs(78,25).addBox(-2.0f, 9.5f, 4.0f, 8, 3, 3),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.1919862f));
        if(!inner) body.addOrReplaceChild("Backplate",CubeListBuilder.create().texOffs(36,45).addBox(-4.0f, 1.0f, 1.9f, 8, 11, 2),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        if(!inner) body.addOrReplaceChild("Book",CubeListBuilder.create().texOffs(81,16).addBox(1.0f, 0.0f, 4.0f, 5, 7, 2),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.7679449f));
        if(!inner) body.addOrReplaceChild("ClothchestL",CubeListBuilder.create().texOffs(108,38).mirror().addBox(2.1f, 0.5f, -3.5f, 2, 8, 1),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        if(!inner) body.addOrReplaceChild("ClothchestR",CubeListBuilder.create().texOffs(108,38).addBox(-4.1f, 0.5f, -3.5f, 2, 8, 1),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        right_arm.addOrReplaceChild("ShoulderR",CubeListBuilder.create().texOffs(16,45).mirror().addBox(-3.5f, -2.5f, -2.5f, 5, 5, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        right_arm.addOrReplaceChild("RArm1",CubeListBuilder.create().texOffs(88,39).addBox(-3.5f, 2.5f, -2.5f, 5, 7, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        right_arm.addOrReplaceChild("RArm2",CubeListBuilder.create().texOffs(76,32).addBox(-3.0f, 5.5f, 2.5f, 4, 4, 2),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        right_arm.addOrReplaceChild("RArm3",CubeListBuilder.create().texOffs(88,32).addBox(-2.5f, 3.5f, 2.5f, 3, 2, 1),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        right_arm.addOrReplaceChild("ShoulderplateTopR",CubeListBuilder.create().texOffs(56,25).addBox(-5.5f, -2.5f, -3.5f, 2, 1, 7),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.4363323f));
        right_arm.addOrReplaceChild("ShoulderplateR1",CubeListBuilder.create().texOffs(56,33).addBox(-4.5f, -1.5f, -3.5f, 1, 4, 7),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.4363323f));
        right_arm.addOrReplaceChild("ShoulderplateR2",CubeListBuilder.create().texOffs(40,33).addBox(-3.5f, 1.5f, -3.5f, 1, 3, 7),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.4363323f));
        right_arm.addOrReplaceChild("ShoulderplateR3",CubeListBuilder.create().texOffs(40,33).addBox(-2.5f, 3.5f, -3.5f, 1, 3, 7),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.4363323f));
        left_arm.addOrReplaceChild("ShoulderL",CubeListBuilder.create().texOffs(16,45).mirror().addBox(-1.5f, -2.5f, -2.5f, 5, 5, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        left_arm.addOrReplaceChild("LArm1",CubeListBuilder.create().texOffs(88,39).mirror().addBox(-1.5f, 2.5f, -2.5f, 5, 7, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        left_arm.addOrReplaceChild("LArm2",CubeListBuilder.create().texOffs(76,32).addBox(-1.0f, 5.5f, 2.5f, 4, 4, 2),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        left_arm.addOrReplaceChild("LArm3",CubeListBuilder.create().texOffs(88,32).addBox(-0.5f, 3.5f, 2.5f, 3, 2, 1),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.0f));
        left_arm.addOrReplaceChild("ShoulderplateTopL",CubeListBuilder.create().texOffs(56,25).addBox(3.5f, -2.5f, -3.5f, 2, 1, 7),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -0.4363323f));
        left_arm.addOrReplaceChild("ShoulderplateL1",CubeListBuilder.create().texOffs(56,33).addBox(3.5f, -1.5f, -3.5f, 1, 4, 7),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -0.4363323f));
        left_arm.addOrReplaceChild("ShoulderplateL2",CubeListBuilder.create().texOffs(40,33).addBox(2.5f, 1.5f, -3.5f, 1, 3, 7),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -0.4363323f));
        left_arm.addOrReplaceChild("ShoulderplateL3",CubeListBuilder.create().texOffs(40,33).addBox(1.5f, 3.5f, -3.5f, 1, 3, 7),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -0.4363323f));
        right_leg.addOrReplaceChild("LegpanelR4",CubeListBuilder.create().texOffs(76,38).addBox(-3.0f, 0.5f, -3.5f, 2, 3, 1),PartPose.offsetAndRotation(0,0,0,-0.4363323f, 0.0f, 0.0f));
        right_leg.addOrReplaceChild("LegpanelR5",CubeListBuilder.create().texOffs(76,42).addBox(-3.0f, 2.5f, -2.5f, 2, 3, 1),PartPose.offsetAndRotation(0,0,0,-0.4363323f, 0.0f, 0.0f));
        right_leg.addOrReplaceChild("LegpanelR6",CubeListBuilder.create().texOffs(82,38).addBox(-3.0f, 4.5f, -1.5f, 2, 3, 1),PartPose.offsetAndRotation(0,0,0,-0.4363323f, 0.0f, 0.0f));
        right_leg.addOrReplaceChild("SidepanelR1",CubeListBuilder.create().texOffs(116,25).mirror().addBox(-2.5f, 0.5f, -2.5f, 1, 4, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.4363323f));
        right_leg.addOrReplaceChild("SideclothR1",CubeListBuilder.create().texOffs(116,42).addBox(-2.5f, 0.5f, -2.5f, 1, 5, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.122173f));
        right_leg.addOrReplaceChild("SideclothR2",CubeListBuilder.create().texOffs(116,34).addBox(-1.5f, 5.5f, -2.5f, 1, 3, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.296706f));
        right_leg.addOrReplaceChild("SideclothR3",CubeListBuilder.create().texOffs(116,1).addBox(0.4f, 8.4f, -2.5f, 1, 3, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, 0.5235988f));
        left_leg.addOrReplaceChild("LegpanelL4",CubeListBuilder.create().texOffs(76,38).mirror().addBox(1.0f, 0.5f, -3.5f, 2, 3, 1),PartPose.offsetAndRotation(0,0,0,-0.4363323f, 0.0f, 0.0f));
        left_leg.addOrReplaceChild("LegpanelL5",CubeListBuilder.create().texOffs(76,42).mirror().addBox(1.0f, 2.5f, -2.5f, 2, 3, 1),PartPose.offsetAndRotation(0,0,0,-0.4363323f, 0.0f, 0.0f));
        left_leg.addOrReplaceChild("LegpanelL6",CubeListBuilder.create().texOffs(82,38).mirror().addBox(1.0f, 4.5f, -1.5f, 2, 3, 1),PartPose.offsetAndRotation(0,0,0,-0.4363323f, 0.0f, 0.0f));
        left_leg.addOrReplaceChild("SidepanelL1",CubeListBuilder.create().texOffs(116,25).addBox(1.5f, 0.5f, -2.5f, 1, 4, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -0.4363323f));
        left_leg.addOrReplaceChild("SideclothL1",CubeListBuilder.create().texOffs(116,42).addBox(1.5f, 0.5f, -2.5f, 1, 5, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -0.122173f));
        left_leg.addOrReplaceChild("SideclothL2",CubeListBuilder.create().texOffs(116,34).addBox(0.5f, 5.5f, -2.5f, 1, 3, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -0.296706f));
        left_leg.addOrReplaceChild("SideclothL3",CubeListBuilder.create().texOffs(116,1).addBox(-1.4f, 8.4f, -2.5f, 1, 3, 5),PartPose.offsetAndRotation(0,0,0,0.0f, 0.0f, -0.5235988f));
        return LayerDefinition.create(mesh,128,64);
    }
    public void animateCloth(float swing,float amount){
        float c=Math.min((float)Math.cos(swing*.6662f)*1.4f*amount,(float)Math.cos(swing*.6662f+Math.PI)*1.4f*amount);
        for(String side:java.util.List.of("L","R")){
            cloth("Frontcloth"+side+"1",c-.1047198f);cloth("Frontcloth"+side+"2",c-.3316126f);
            cloth("ClothBack"+side+"1",-c+.1047198f);cloth("ClothBack"+side+"2",-c+.2268928f);cloth("ClothBack"+side+"3",-c+.2268928f);
        }
    }
    private void cloth(String name,float angle){if(body.hasChild(name))body.getChild(name).xRot=angle;}
}
