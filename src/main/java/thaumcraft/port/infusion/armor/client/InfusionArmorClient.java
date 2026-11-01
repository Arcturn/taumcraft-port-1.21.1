package thaumcraft.port.infusion.armor.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import thaumcraft.port.infusion.armor.*;

@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class InfusionArmorClient {
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event){event.enqueueWork(()->{InfusionFxPayload.client=InfusionEquipmentFx::receive;InfusionCollectorVisuals.trail=entity->{var p=net.minecraft.client.Minecraft.getInstance().particleEngine.createParticle(thaumcraft.port.content.PortParticles.get("crucible_bubble"),entity.getX(),entity.getY()+.1,entity.getZ(),0,0,0);if(p!=null)p.setColor(.33f,.33f,1f);};});}
    @SubscribeEvent public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(InfusionEnchantments.FOLLOWING_ITEM.get(),net.minecraft.client.renderer.entity.ItemEntityRenderer::new);}
    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event){
        event.registerItem(new IClientItemExtensions(){
            private FortressArmorModel outer,inner;
            @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity,ItemStack stack,EquipmentSlot slot,HumanoidModel<?> original){
                if(outer==null){outer=new FortressArmorModel(false);inner=new FortressArmorModel(true);}
                var model=slot==EquipmentSlot.LEGS?inner:outer;model.prepare(entity);return model;
            }
            @Override public void setupModelAnimations(LivingEntity entity,ItemStack stack,EquipmentSlot slot,net.minecraft.client.model.Model model,float swing,float amount,float partial,float age,float yaw,float pitch){if(model instanceof FortressArmorModel armor){armor.head.xScale=armor.head.yScale=armor.head.zScale=1.01f;}}
        },InfusionArmor.FORTRESS_HELM,InfusionArmor.FORTRESS_CHEST,InfusionArmor.FORTRESS_LEGS);
        event.registerItem(new IClientItemExtensions(){
            private RobeArmorModel outer,inner;
            @Override public int getDefaultDyeColor(ItemStack stack){return 0xff000000|RobeArmorItem.DEFAULT_COLOR;}
            @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity,ItemStack stack,EquipmentSlot slot,HumanoidModel<?> original){if(outer==null){outer=new RobeArmorModel(false);inner=new RobeArmorModel(true);}return slot==EquipmentSlot.LEGS?inner:outer;}
            @Override public void setupModelAnimations(LivingEntity entity,ItemStack stack,EquipmentSlot slot,net.minecraft.client.model.Model model,float swing,float amount,float partial,float age,float yaw,float pitch){if(model instanceof RobeArmorModel robe)robe.animateCloth(swing,amount);}
        },InfusionArmor.CLOTH_CHEST,InfusionArmor.CLOTH_LEGS,InfusionArmor.CLOTH_BOOTS);
    }
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event){event.register((stack,index)->index==0?DyedItemColor.getOrDefault(stack,0xff000000|RobeArmorItem.DEFAULT_COLOR):-1,InfusionArmor.CLOTH_CHEST.get(),InfusionArmor.CLOTH_LEGS.get(),InfusionArmor.CLOTH_BOOTS.get());}
}
