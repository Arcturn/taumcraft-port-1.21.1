package thaumcraft.port.golem.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.*;
import thaumcraft.port.golem.*;
import thaumcraft.port.golem.part.PartModel;
import thaumcraft.port.golem.part.PartModel.EnumAttachPoint;
import thaumcraft.port.golem.part.PartModel.EnumLimbSide;

/** BETA26's original attachment pivots, object groups and PartModel callbacks. */
public final class GolemAssemblyRenderer {
    private final GolemObjModel base = new GolemObjModel(ResourceLocation.parse("thaumcraft:models/obj/golem_base.obj"));
    private final Map<ResourceLocation,GolemObjModel> models = new HashMap<>();
    private final Map<GolemEntity,float[]> grinders = new WeakHashMap<>();

    /** Static assembled model for the builder preview, with the same OBJ geometry as a placed golem. */
    public void renderPreview(GolemProperties properties,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        render(properties,null,0,0,0,0,0,0,pose,buffers,light,overlay,-1,false);
    }

    public void render(GolemProperties props,GolemEntity entity,float limb,float amount,float age,float headYaw,float pitch,float partial,
                       PoseStack pose,MultiBufferSource buffers,int light,int overlay,int color,boolean xray) {
        boolean holding=entity!=null&&!entity.getMainHandItem().isEmpty();
        boolean wheel=props.hasTrait(EnumGolemTrait.WHEELED)||props.hasTrait(EnumGolemTrait.FLYER);
        float swing=entity==null?0:entity.getAttackAnim(partial);
        float rx=(float)Math.toDegrees(Mth.sin(age*.067f)*.03f),rz=(float)Math.toDegrees(Mth.cos(age*.09f)*.05f+.05f);
        float rightX,leftX,rightY=0,leftY=0,rightZ,leftZ,bodyY=0;
        if(holding){rightX=leftX=90-rz/2;rightZ=-2;leftZ=2;}
        else {rightX=wheel?rx*2:(float)Math.toDegrees(Mth.cos(limb*.6662f+Mth.PI)*amount)+rx;
            leftX=wheel?-rx*2:(float)Math.toDegrees(Mth.cos(limb*.6662f)*amount)-rx;rightZ=rz+2;leftZ=-rz-2;}
        if(swing>0){float wiggle=-Mth.sin(Mth.sqrt(swing)*Mth.TWO_PI)*.2f;bodyY=(float)Math.toDegrees(wiggle);rightZ=-(float)Math.toDegrees(Mth.sin(wiggle)*3);rightX=(float)Math.toDegrees(-Mth.cos(wiggle)*5);rightY+=bodyY;}
        pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(bodyY));
        if(entity!=null){double dx=entity.getX()-entity.xo,dz=entity.getZ()-entity.zo;double speed=dx*dx+dz*dz;float lean=wheel?75:25;
            pose.mulPose(Axis.XN.rotationDegrees((float)(speed*lean)));pose.mulPose(Axis.ZN.rotationDegrees((float)(speed*lean*.06*(entity.getYRot()-entity.yRotO))));}
        var material=props.getMaterial().texture;
        PartModel[] parts={props.getHead().model,props.getArms().model,props.getLegs().model,props.getAddon().model};
        pose.pushPose();pose.translate(0,.5,0);base("chest",material,pose,buffers,light,overlay,color,xray);base("waist",material,pose,buffers,light,overlay,color,xray);
        if(entity!=null&&entity.getGolemColor()>0){base("flag",material,pose,buffers,light,overlay,0xff000000|DyeColor.byId(entity.getGolemColor()-1).getTextureDiffuseColor(),xray);
            // Source restores white after a flag, including in its transparent/xray pass.
            color=-1;
        }
        parts(parts,EnumAttachPoint.BODY,EnumLimbSide.MIDDLE,entity,props,pose,buffers,light,overlay,color,xray,partial);
        pose.popPose();pose.pushPose();pose.translate(0,.75,-.03125);pose.mulPose(Axis.YN.rotationDegrees(headYaw));pose.mulPose(Axis.XN.rotationDegrees(pitch));
        parts(parts,EnumAttachPoint.HEAD,EnumLimbSide.MIDDLE,entity,props,pose,buffers,light,overlay,color,xray,partial);base("head",material,pose,buffers,light,overlay,color,xray);pose.popPose();
        boolean darts=entity!=null&&props.getArms().key.equals("DARTS")&&entity.isInCombat();
        for(var side:new EnumLimbSide[]{EnumLimbSide.RIGHT,EnumLimbSide.LEFT}){
            boolean left=side==EnumLimbSide.LEFT;float x=left?leftX:rightX,y=left?leftY:rightY,z=left?leftZ:rightZ;
            if(darts){x=90-entity.xRotO+x/10;y/=10;z/=10;}
            pose.pushPose();pose.translate(left?-.20625:.20625,.6875,0);pose.mulPose(Axis.XP.rotationDegrees(x));pose.mulPose(Axis.YP.rotationDegrees(y+(left?180:0)));pose.mulPose((left?Axis.ZN:Axis.ZP).rotationDegrees(z));
            base("arm",material,pose,buffers,light,overlay,color,xray);parts(parts,EnumAttachPoint.ARMS,side,entity,props,pose,buffers,light,overlay,color,xray,partial);pose.popPose();
        }
        for(var side:new EnumLimbSide[]{EnumLimbSide.RIGHT,EnumLimbSide.LEFT}){
            boolean left=side==EnumLimbSide.LEFT;pose.pushPose();pose.translate(left?-.09375:.09375,.375,0);pose.mulPose(Axis.XP.rotationDegrees((float)Math.toDegrees(Mth.cos(limb*.6662f+(left?Mth.PI:0))*amount)));
            parts(parts,EnumAttachPoint.LEGS,side,entity,props,pose,buffers,light,overlay,color,xray,partial);pose.popPose();
        }
        if(holding){pose.pushPose();pose.translate(0,.625,0);pose.mulPose(Axis.XP.rotationDegrees(90-rz*.5f));pose.mulPose(Axis.XN.rotationDegrees(90));pose.scale(.375f,.375f,.375f);pose.translate(0,.25,-1.5);if(!(entity.getMainHandItem().getItem() instanceof BlockItem))pose.translate(0,-.6,0);item(entity,entity.getMainHandItem(),pose,buffers,light,overlay);pose.popPose();}
        pose.popPose();
    }
    private void base(String group,ResourceLocation texture,PoseStack pose,MultiBufferSource buffers,int light,int overlay,int color,boolean xray){base.render(group,pose,buffers.getBuffer(GolemRenderLayers.entity(texture,color,xray)),light,overlay,color);}
    private void parts(PartModel[] parts,EnumAttachPoint point,EnumLimbSide side,GolemEntity entity,GolemProperties props,PoseStack pose,MultiBufferSource buffers,int light,int overlay,int color,boolean xray,float partial){
        for(var part:parts)if(part!=null&&part.getAttachPoint()==point){
            var model=models.computeIfAbsent(part.getObjModel(),GolemObjModel::new);String path=part.getObjModel().getPath();
            for(var group:model.groups()){
                pose.pushPose();
                if(entity!=null){
                    if(path.endsWith("golem_legs_wheel.obj")&&group.equals("wheel")){pose.translate(0,-.375,0);pose.mulPose(Axis.XN.rotationDegrees(entity.wheelRotation));}
                    if(path.endsWith("golem_arms_claws.obj")&&group.startsWith("claw")){float f=entity.getAttackAnim(partial)*4.1f;pose.translate(0,-.2,0);pose.mulPose((group.endsWith("1")?Axis.XP:Axis.XN).rotationDegrees(f*f));}
                    if(path.endsWith("golem_arms_breakers.obj")&&group.equals("grinder")){var state=grinders.computeIfAbsent(entity,k->new float[2]);float f=Math.max(state[0],entity.getAttackAnim(partial)*20);state[1]+=f;state[0]=f*.99f;pose.translate(0,-.34,0);pose.mulPose((side==EnumLimbSide.LEFT?Axis.XN:Axis.XP).rotationDegrees((entity.tickCount+partial)/2+state[1]+(side==EnumLimbSide.LEFT?22:0)));}
                } else {
                    if(path.endsWith("golem_legs_wheel.obj")&&group.equals("wheel"))pose.translate(0,-.375,0);
                    if(path.endsWith("golem_arms_claws.obj")&&group.startsWith("claw"))pose.translate(0,-.2,0);
                    if(path.endsWith("golem_arms_breakers.obj")&&group.equals("grinder")){pose.translate(0,-.34,0);if(side==EnumLimbSide.LEFT)pose.mulPose(Axis.XN.rotationDegrees(22));}
                }
                var texture=part.useMaterialTextureForObjectPart(group)||part.getTexture()==null?props.getMaterial().texture:part.getTexture();
                model.render(group,pose,buffers.getBuffer(GolemRenderLayers.entity(texture,color,xray)),light,overlay,color);
                if(entity!=null&&path.endsWith("golem_hauler.obj")&&entity.getCarrying().size()>1&&!entity.getCarrying().get(1).isEmpty()){
                    var stack=entity.getCarrying().get(1);pose.pushPose();pose.scale(.375f,.375f,.375f);pose.translate(0,.33,.825);if(!(stack.getItem() instanceof BlockItem))pose.translate(0,0,-.25);item(entity,stack,pose,buffers,light,overlay);pose.popPose();}
                pose.popPose();
            }
        }
    }
    private static void item(GolemEntity entity,ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay){Minecraft.getInstance().getItemRenderer().renderStatic(entity,stack,ItemDisplayContext.HEAD,false,pose,buffers,entity.level(),light,overlay,entity.getId());}
}
