package thaumcraft.port.golem.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import thaumcraft.port.golem.GolemEntity;
import thaumcraft.port.golem.item.GolemBellItem;
import thaumcraft.port.golem.seal.SealPlacerItem;

/** Keeps living render events, riding/head interpolation, damage overlays and leash rendering. */
public final class GolemRenderer extends MobRenderer<GolemEntity,GolemRenderer.Model> {
    public GolemRenderer(EntityRendererProvider.Context context){super(context,new Model(),.3f);}
    @Override public ResourceLocation getTextureLocation(GolemEntity entity){return entity.getProperties().getMaterial().texture;}
    @Override public void render(GolemEntity entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        var player=Minecraft.getInstance().player;model.buffers=buffers;model.partial=partial;
        model.visible=!entity.isInvisible()||(player!=null&&!entity.isInvisibleTo(player));
        model.xray=player!=null&&player.isCrouching()&&
                (displayer(player.getMainHandItem().getItem())||displayer(player.getOffhandItem().getItem()))&&entity.level().clip(new ClipContext(
                        player.position().add(0,player.getBbHeight()/2,0),entity.position().add(0,entity.getBbHeight()/2,0),
                        ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player)).getType()!=HitResult.Type.MISS;
        super.render(entity,yaw,partial,pose,buffers,light);
        model.buffers=null;
    }
    private static boolean displayer(net.minecraft.world.item.Item item){return item instanceof GolemBellItem||item instanceof SealPlacerItem;}
    @Override protected RenderType getRenderType(GolemEntity entity,boolean visible,boolean translucent,boolean glowing){
        return model.xray?RenderType.entityTranslucent(getTextureLocation(entity)):super.getRenderType(entity,visible,translucent,glowing);
    }
    public static final class Model extends EntityModel<GolemEntity> {
        private final GolemAssemblyRenderer assembly=new GolemAssemblyRenderer();
        private MultiBufferSource buffers;private GolemEntity entity;private float partial,limb,amount,age,headYaw,pitch;private boolean visible,xray;
        Model(){super(RenderType::entityTranslucent);}
        @Override public void setupAnim(GolemEntity entity,float limb,float amount,float age,float headYaw,float pitch){this.entity=entity;this.limb=limb;this.amount=amount;this.age=age;this.headYaw=headYaw;this.pitch=pitch;}
        @Override public void renderToBuffer(PoseStack pose,VertexConsumer ignored,int light,int overlay,int color){
            // BETA26's custom OBJ renderer intentionally omitted the vanilla
            // upside-down ModelBiped transform; undo only those two operations.
            pose.pushPose();pose.translate(0,1.501f,0);pose.scale(-1,-1,1);
            if(visible)assembly.render(entity.getProperties(),entity,limb,amount,age,headYaw,pitch,partial,pose,buffers,light,overlay,color,false);
            if(xray)assembly.render(entity.getProperties(),entity,limb,amount,age,headYaw,pitch,partial,pose,buffers,light,overlay,0x3f3f3f3f,true);
            pose.popPose();
        }
    }
}
