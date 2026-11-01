package thaumcraft.port.infusion.content.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import thaumcraft.port.client.ClientJournal;
import thaumcraft.port.infusion.armor.client.RobeArmorModel;
import thaumcraft.port.infusion.content.*;

@EventBusSubscriber(modid="thaumcraft",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class InfusionCraftingClient {
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event){
        event.enqueueWork(()->VoidseerCharmItem.clientPermanentWarp=()->ClientJournal.state.permanentWarp());
    }
    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event){
        event.registerItem(new IClientItemExtensions(){
            private RobeArmorModel outer,inner;
            @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity,ItemStack stack,EquipmentSlot slot,HumanoidModel<?> original){
                if(outer==null){outer=new RobeArmorModel(false);inner=new RobeArmorModel(true);}
                return slot==EquipmentSlot.CHEST||slot==EquipmentSlot.FEET?outer:inner;
            }
            @Override public int getDefaultDyeColor(ItemStack stack){return DyedItemColor.getOrDefault(stack,0xff000000|VoidArmorItem.DEFAULT_COLOR);}
            @Override public void setupModelAnimations(LivingEntity entity,ItemStack stack,EquipmentSlot slot,net.minecraft.client.model.Model model,
                    float swing,float amount,float partial,float age,float yaw,float pitch){if(model instanceof RobeArmorModel robe)robe.animateCloth(swing,amount);}
        },InfusionCraftingContent.VOID_ROBE_HELM,InfusionCraftingContent.VOID_ROBE_CHEST,InfusionCraftingContent.VOID_ROBE_LEGS);
    }
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item event){
        event.register((stack,index)->index==0?DyedItemColor.getOrDefault(stack,0xff000000|VoidArmorItem.DEFAULT_COLOR):-1,
                InfusionCraftingContent.VOID_ROBE_HELM.get(),InfusionCraftingContent.VOID_ROBE_CHEST.get(),InfusionCraftingContent.VOID_ROBE_LEGS.get());
    }
    private InfusionCraftingClient() { }
}
