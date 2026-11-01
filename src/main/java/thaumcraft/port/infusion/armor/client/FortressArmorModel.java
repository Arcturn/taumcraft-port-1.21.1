package thaumcraft.port.infusion.armor.client;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import thaumcraft.port.infusion.armor.FortressArmorItem;
import thaumcraft.port.infusion.recipe.InfusionAugments;

/** TC6 ModelFortressArmor geometry, UV coordinates and set-dependent ornaments. */
public final class FortressArmorModel extends HumanoidModel<LivingEntity> {
    public FortressArmorModel(boolean inner){super(layer(inner).bakeRoot());}
    public static LayerDefinition layer(boolean inner){
        MeshDefinition mesh=new MeshDefinition();var root=mesh.getRoot();
        var head=root.addOrReplaceChild("head",CubeListBuilder.create(),PartPose.ZERO);
        root.addOrReplaceChild("hat",CubeListBuilder.create(),PartPose.ZERO);
        var body=root.addOrReplaceChild("body",CubeListBuilder.create(),PartPose.ZERO);
        var right_arm=root.addOrReplaceChild("right_arm",CubeListBuilder.create(),PartPose.offset(-5,2,0));
        var left_arm=root.addOrReplaceChild("left_arm",CubeListBuilder.create(),PartPose.offset(5,2,0));
        var right_leg=root.addOrReplaceChild("right_leg",CubeListBuilder.create(),PartPose.offset(-1.9f,12,0));
        var left_leg=root.addOrReplaceChild("left_leg",CubeListBuilder.create(),PartPose.offset(1.9f,12,0));
        head.addOrReplaceChild("OrnamentL",CubeListBuilder.create().texOffs(78,8).mirror().addBox(1.5f, -9.0f, -6.5f, 2, 2, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.1396263f, 0.0f, 0.0f));
        head.addOrReplaceChild("OrnamentL2",CubeListBuilder.create().texOffs(78,8).mirror().addBox(3.5f, -10.0f, -6.5f, 1, 2, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.1396263f, 0.0f, 0.0f));
        head.addOrReplaceChild("OrnamentR",CubeListBuilder.create().texOffs(78,8).addBox(-3.5f, -9.0f, -6.5f, 2, 2, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.1396263f, 0.0f, 0.0f));
        head.addOrReplaceChild("OrnamentR2",CubeListBuilder.create().texOffs(78,8).addBox(-4.5f, -10.0f, -6.5f, 1, 2, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.1396263f, 0.0f, 0.0f));
        head.addOrReplaceChild("Helmet",CubeListBuilder.create().texOffs(41,8).addBox(-4.5f, -9.0f, -4.5f, 9, 4, 9),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
        head.addOrReplaceChild("HelmetR",CubeListBuilder.create().texOffs(21,13).addBox(-6.5f, -3.0f, -4.5f, 1, 5, 9),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.5235988f));
        head.addOrReplaceChild("HelmetL",CubeListBuilder.create().texOffs(21,13).mirror().addBox(5.5f, -3.0f, -4.5f, 1, 5, 9),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, -0.5235988f));
        head.addOrReplaceChild("HelmetB",CubeListBuilder.create().texOffs(41,21).addBox(-4.5f, -3.0f, 5.5f, 9, 5, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.5235988f, 0.0f, 0.0f));
        head.addOrReplaceChild("capsthingy",CubeListBuilder.create().texOffs(21,0).addBox(-4.5f, -6.0f, -6.5f, 9, 1, 2),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
        head.addOrReplaceChild("flapR",CubeListBuilder.create().texOffs(59,10).addBox(-10.0f, -2.0f, -1.0f, 3, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, -0.5235988f, 0.5235988f));
        head.addOrReplaceChild("flapL",CubeListBuilder.create().texOffs(59,10).mirror().addBox(7.0f, -2.0f, -1.0f, 3, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.5235988f, -0.5235988f));
        head.addOrReplaceChild("Gemornament",CubeListBuilder.create().texOffs(68,11).addBox(-1.5f, -9.0f, -7.0f, 3, 3, 2),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.1396263f, 0.0f, 0.0f));
        head.addOrReplaceChild("Gem",CubeListBuilder.create().texOffs(72,8).addBox(-1.0f, -8.5f, -7.5f, 2, 2, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.1396263f, 0.0f, 0.0f));
        head.addOrReplaceChild("Goggles",CubeListBuilder.create().texOffs(100,18).addBox(-4.5f, -5.0f, -4.25f, 9, 5, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
        if(inner) body.addOrReplaceChild("Mbelt",CubeListBuilder.create().texOffs(56,55).addBox(-4.0f, 8.0f, -3.0f, 8, 4, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
        if(inner) body.addOrReplaceChild("MbeltL",CubeListBuilder.create().texOffs(76,44).addBox(4.0f, 8.0f, -3.0f, 1, 3, 6),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
        if(inner) body.addOrReplaceChild("MbeltR",CubeListBuilder.create().texOffs(76,44).addBox(-5.0f, 8.0f, -3.0f, 1, 3, 6),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
        if(!inner) body.addOrReplaceChild("BeltR",CubeListBuilder.create().texOffs(76,44).addBox(-5.0f, 4.0f, -3.0f, 1, 3, 6),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
        if(!inner) body.addOrReplaceChild("BeltL",CubeListBuilder.create().texOffs(76,44).addBox(4.0f, 4.0f, -3.0f, 1, 3, 6),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
        if(!inner) body.addOrReplaceChild("Chestplate",CubeListBuilder.create().texOffs(56,45).addBox(-4.0f, 1.0f, -4.0f, 8, 7, 2),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
        if(!inner) body.addOrReplaceChild("Scroll",CubeListBuilder.create().texOffs(34,27).addBox(-2.0f, 9.5f, 4.0f, 8, 3, 3),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.1919862f));
        if(!inner) body.addOrReplaceChild("Backplate",CubeListBuilder.create().texOffs(36,45).addBox(-4.0f, 1.0f, 2.0f, 8, 11, 2),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
        if(!inner) body.addOrReplaceChild("Book",CubeListBuilder.create().texOffs(100,8).addBox(1.0f, -0.3f, 4.0f, 5, 7, 2),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.7679449f));
        right_arm.addOrReplaceChild("ShoulderR",CubeListBuilder.create().texOffs(56,35).addBox(-3.5f, -2.5f, -2.5f, 5, 5, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
        right_arm.addOrReplaceChild("GauntletR",CubeListBuilder.create().texOffs(100,26).addBox(-3.5f, 3.5f, -2.5f, 2, 6, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
        right_arm.addOrReplaceChild("GauntletstrapR1",CubeListBuilder.create().texOffs(84,31).addBox(-1.5f, 3.5f, -2.5f, 3, 1, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
        right_arm.addOrReplaceChild("GauntletstrapR2",CubeListBuilder.create().texOffs(84,31).addBox(-1.5f, 6.5f, -2.5f, 3, 1, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
        right_arm.addOrReplaceChild("ShoulderplateRtop",CubeListBuilder.create().texOffs(110,37).addBox(-5.5f, -2.5f, -3.5f, 2, 1, 7),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.4363323f));
        right_arm.addOrReplaceChild("ShoulderplateR1",CubeListBuilder.create().texOffs(110,45).addBox(-4.5f, -1.5f, -3.5f, 1, 4, 7),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.4363323f));
        right_arm.addOrReplaceChild("ShoulderplateR2",CubeListBuilder.create().texOffs(94,45).addBox(-3.5f, 1.5f, -3.5f, 1, 3, 7),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.4363323f));
        right_arm.addOrReplaceChild("ShoulderplateR3",CubeListBuilder.create().texOffs(94,45).addBox(-2.5f, 3.5f, -3.5f, 1, 3, 7),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.4363323f));
        left_arm.addOrReplaceChild("ShoulderL",CubeListBuilder.create().texOffs(56,35).mirror().addBox(-1.5f, -2.5f, -2.5f, 5, 5, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
        left_arm.addOrReplaceChild("GauntletL",CubeListBuilder.create().texOffs(114,26).addBox(1.5f, 3.5f, -2.5f, 2, 6, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
        left_arm.addOrReplaceChild("Gauntletstrapl1",CubeListBuilder.create().texOffs(84,31).mirror().addBox(-1.5f, 3.5f, -2.5f, 3, 1, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
        left_arm.addOrReplaceChild("GauntletstrapL2",CubeListBuilder.create().texOffs(84,31).mirror().addBox(-1.5f, 6.5f, -2.5f, 3, 1, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.0f));
        left_arm.addOrReplaceChild("ShoulderplateLtop",CubeListBuilder.create().texOffs(110,37).mirror().addBox(3.5f, -2.5f, -3.5f, 2, 1, 7),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, -0.4363323f));
        left_arm.addOrReplaceChild("ShoulderplateL1",CubeListBuilder.create().texOffs(110,45).mirror().addBox(3.5f, -1.5f, -3.5f, 1, 4, 7),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, -0.4363323f));
        left_arm.addOrReplaceChild("ShoulderplateL2",CubeListBuilder.create().texOffs(94,45).mirror().addBox(2.5f, 1.5f, -3.5f, 1, 3, 7),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, -0.4363323f));
        left_arm.addOrReplaceChild("ShoulderplateL3",CubeListBuilder.create().texOffs(94,45).mirror().addBox(1.5f, 3.5f, -3.5f, 1, 3, 7),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, -0.4363323f));
        right_leg.addOrReplaceChild("LegpanelR1",CubeListBuilder.create().texOffs(0,51).addBox(-1.0f, 0.5f, -3.5f, 3, 4, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.4363323f, 0.0f, 0.0f));
        right_leg.addOrReplaceChild("LegpanelR2",CubeListBuilder.create().texOffs(8,51).addBox(-1.0f, 3.5f, -2.5f, 3, 4, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.4363323f, 0.0f, 0.0f));
        right_leg.addOrReplaceChild("LegpanelR3",CubeListBuilder.create().texOffs(0,56).addBox(-1.0f, 6.5f, -1.5f, 3, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.4363323f, 0.0f, 0.0f));
        right_leg.addOrReplaceChild("LegpanelR4",CubeListBuilder.create().texOffs(0,43).addBox(-3.0f, 0.5f, -3.5f, 2, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.4363323f, 0.0f, 0.0f));
        right_leg.addOrReplaceChild("LegpanelR5",CubeListBuilder.create().texOffs(0,47).addBox(-3.0f, 2.5f, -2.5f, 2, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.4363323f, 0.0f, 0.0f));
        right_leg.addOrReplaceChild("LegpanelR6",CubeListBuilder.create().texOffs(6,43).addBox(-3.0f, 4.5f, -1.5f, 2, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.4363323f, 0.0f, 0.0f));
        right_leg.addOrReplaceChild("SidepanelR1",CubeListBuilder.create().texOffs(0,22).addBox(-2.5f, 0.5f, -2.5f, 1, 4, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.4363323f));
        right_leg.addOrReplaceChild("SidepanelR2",CubeListBuilder.create().texOffs(0,31).addBox(-1.5f, 3.5f, -2.5f, 1, 3, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.4363323f));
        right_leg.addOrReplaceChild("SidepanelR3",CubeListBuilder.create().texOffs(12,31).addBox(-0.5f, 5.5f, -2.5f, 1, 3, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, 0.4363323f));
        right_leg.addOrReplaceChild("BackpanelR1",CubeListBuilder.create().texOffs(0,18).addBox(-3.0f, 0.5f, 2.5f, 5, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.4363323f, 0.0f, 0.0f));
        right_leg.addOrReplaceChild("BackpanelR2",CubeListBuilder.create().texOffs(0,18).addBox(-3.0f, 2.5f, 1.5f, 5, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.4363323f, 0.0f, 0.0f));
        right_leg.addOrReplaceChild("BackpanelR3",CubeListBuilder.create().texOffs(0,18).addBox(-3.0f, 4.5f, 0.5f, 5, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.4363323f, 0.0f, 0.0f));
        left_leg.addOrReplaceChild("BackpanelL3",CubeListBuilder.create().texOffs(0,18).mirror().addBox(-2.0f, 4.5f, 0.5f, 5, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.4363323f, 0.0f, 0.0f));
        left_leg.addOrReplaceChild("LegpanelL1",CubeListBuilder.create().texOffs(0,51).mirror().addBox(-2.0f, 0.5f, -3.5f, 3, 4, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.4363323f, 0.0f, 0.0f));
        left_leg.addOrReplaceChild("LegpanelL2",CubeListBuilder.create().texOffs(8,51).mirror().addBox(-2.0f, 3.5f, -2.5f, 3, 4, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.4363323f, 0.0f, 0.0f));
        left_leg.addOrReplaceChild("LegpanelL3",CubeListBuilder.create().texOffs(0,56).mirror().addBox(-2.0f, 6.5f, -1.5f, 3, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.4363323f, 0.0f, 0.0f));
        left_leg.addOrReplaceChild("LegpanelL4",CubeListBuilder.create().texOffs(0,43).mirror().addBox(1.0f, 0.5f, -3.5f, 2, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.4363323f, 0.0f, 0.0f));
        left_leg.addOrReplaceChild("LegpanelL5",CubeListBuilder.create().texOffs(0,47).mirror().addBox(1.0f, 2.5f, -2.5f, 2, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.4363323f, 0.0f, 0.0f));
        left_leg.addOrReplaceChild("LegpanelL6",CubeListBuilder.create().texOffs(6,43).mirror().addBox(1.0f, 4.5f, -1.5f, 2, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,-0.4363323f, 0.0f, 0.0f));
        left_leg.addOrReplaceChild("SidepanelL1",CubeListBuilder.create().texOffs(0,22).mirror().addBox(1.5f, 0.5f, -2.5f, 1, 4, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, -0.4363323f));
        left_leg.addOrReplaceChild("SidepanelL2",CubeListBuilder.create().texOffs(0,31).mirror().addBox(0.5f, 3.5f, -2.5f, 1, 3, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, -0.4363323f));
        left_leg.addOrReplaceChild("SidepanelL3",CubeListBuilder.create().texOffs(12,31).mirror().addBox(-0.5f, 5.5f, -2.5f, 1, 3, 5),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.0f, 0.0f, -0.4363323f));
        left_leg.addOrReplaceChild("BackpanelL1",CubeListBuilder.create().texOffs(0,18).mirror().addBox(-2.0f, 0.5f, 2.5f, 5, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.4363323f, 0.0f, 0.0f));
        left_leg.addOrReplaceChild("BackpanelL2",CubeListBuilder.create().texOffs(0,18).mirror().addBox(-2.0f, 2.5f, 1.5f, 5, 3, 1),PartPose.offsetAndRotation(0.0f, 0.0f, 0.0f,0.4363323f, 0.0f, 0.0f));
        for(int i=0;i<3;i++)head.addOrReplaceChild("Mask"+i,CubeListBuilder.create().texOffs(52+i*24,2).addBox(-4.5f,-5,-4.6f,9,5,1),PartPose.ZERO);
        return LayerDefinition.create(mesh,128,64);
    }
    public void prepare(LivingEntity entity){
        int count=0;for(var item:entity.getArmorSlots())if(item.getItem() instanceof FortressArmorItem)count++;
        var helmet=entity.getItemBySlot(EquipmentSlot.HEAD);
        boolean fortress=helmet.getItem() instanceof FortressArmorItem;
        head.getChild("Goggles").visible=fortress&&InfusionAugments.goggles(helmet);
        for(int i=0;i<3;i++)head.getChild("Mask"+i).visible=fortress&&InfusionAugments.mask(helmet)==i;
        for(var part:java.util.List.of(head,body,rightArm,leftArm,rightLeg,leftLeg)) {
            for(String name:java.util.List.of("Scroll","OrnamentL","OrnamentL2","OrnamentR","OrnamentR2","Gemornament","Gem","ShoulderplateL2","ShoulderplateL3","ShoulderplateR2","ShoulderplateR3","SidepanelL3","SidepanelR3"))if(part.hasChild(name))part.getChild(name).visible=count>=3;
            for(String name:java.util.List.of("Book","flapL","flapR","ShoulderplateLtop","ShoulderplateL1","ShoulderplateRtop","ShoulderplateR1","SidepanelL2","SidepanelR2"))if(part.hasChild(name))part.getChild(name).visible=count>=2;
        }
    }
}
