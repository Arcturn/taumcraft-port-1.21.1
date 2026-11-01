package thaumcraft.port.infusion.bauble;

import java.util.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.port.research.*;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(modid="thaumcraft")
public final class BaubleEvents {
    private static final Set<UUID> AIR_JUMP_USED=new HashSet<>();
    public static boolean worn(Player player,Item item){return CuriosApi.getCuriosInventory(player).map(inventory->{
        for(var handler:inventory.getCurios().values())for(int i=0;i<handler.getStacks().getSlots();i++)
            if(inventory.isSlotActive(handler.getIdentifier(),i)&&handler.getStacks().getStackInSlot(i).is(item))return true;
        return false;
    }).orElse(false);}
    @SubscribeEvent public static void xp(PlayerXpEvent.PickupXp event){
        if(event.getEntity() instanceof ServerPlayer player&&event.getOrb().value>1&&worn(player,InfusionBaubles.CURIOSITY_BAND.get())){
            int converted=event.getOrb().value/2;event.getOrb().value-=converted;
            String type=knowledgeType(converted,player.getRandom().nextFloat());
            if(type!=null){var categories=ResearchCatalog.CATEGORIES;var category=categories.get(player.getRandom().nextInt(categories.size()));
                var storage=ResearchStorage.get(player.getServer());storage.player(player.getUUID()).addKnowledge(type,category,1);storage.setDirty();ResearchProgress.sync(player);}
        }
    }
    public static String knowledgeType(int converted,float roll){return roll<.05f*converted?"THEORY":roll<.2f*converted?"OBSERVATION":null;}
    @SubscribeEvent public static void fall(LivingIncomingDamageEvent event){
        if(event.getSource().is(DamageTypeTags.IS_FALL)&&event.getEntity() instanceof Player player&&worn(player,InfusionBaubles.CLOUD_RING.get())){
            float amount=Math.max(0,event.getAmount()/3-2);if(amount<1)event.setCanceled(true);else event.setAmount(amount);
        }
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){if(!event.getEntity().level().isClientSide&&event.getEntity().onGround())AIR_JUMP_USED.remove(event.getEntity().getUUID());}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){AIR_JUMP_USED.remove(event.getEntity().getUUID());}
    public static boolean requestJump(ServerPlayer player){
        if(!player.isAlive()||player.onGround()||player.isInWater()||player.isPassenger()||player.getAbilities().flying||!worn(player,InfusionBaubles.CLOUD_RING.get())||!AIR_JUMP_USED.add(player.getUUID()))return false;
        applyJump(player);player.hurtMarked=true;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,new CloudJumpPayload.Effect(player.getId()));return true;
    }
    public static void applyJump(Player player){
        double y=.75;var boost=player.getEffect(MobEffects.JUMP);if(boost!=null)y+=(boost.getAmplifier()+1)*.1;
        Vec3 velocity=player.getDeltaMovement();double x=velocity.x,z=velocity.z;
        if(player.isSprinting()){double yaw=player.getYRot()*Math.PI/180;x-=Math.sin(yaw)*.2;z+=Math.cos(yaw)*.2;}
        player.setDeltaMovement(x,y,z);player.fallDistance=0;
        net.neoforged.neoforge.common.CommonHooks.onLivingJump(player);
    }
    private BaubleEvents(){}
}
