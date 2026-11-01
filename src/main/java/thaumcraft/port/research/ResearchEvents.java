package thaumcraft.port.research;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.Fireball;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.content.PortContent;

/** Original discovery events; deep exploration follows the new world floor. */
@EventBusSubscriber(modid="thaumcraft")
public final class ResearchEvents {
    // Registered explicitly by ThaumcraftPort; the other event hooks use the subscriber.
    public static void tick(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer p)||p.tickCount%20!=0)return;
        var storage=ResearchStorage.get(p.getServer());var state=storage.player(p.getUUID());
        // TC6 checks travel and exploration once every ten seconds.
        boolean changed=p.tickCount%200==0&&checkPeriodic(p,state);
        boolean scanner=p.getMainHandItem().is(PortContent.THAUMOMETER.get())||p.getOffhandItem().is(PortContent.THAUMOMETER.get());
        if(scanner){
            var aura=thaumcraft.port.world.AuraSystem.get(p.serverLevel(),p.blockPosition());
            if(aura!=null&&(aura.flux()>aura.vis()||aura.flux()>aura.base()/3)&&state.stage("FLUX")==0&&!state.knows("FLUX")){
                changed|=state.stage("FLUX",1);
                message(p,"research.FLUX.warn",true);
            }
        }
        if(changed)storage.setDirty();
        if(changed||scanner)ResearchProgress.sync(p);
        // Completed listeners are removed by Minecraft. This also catches old saves and /reload.
        ResearchAdvancements.RESEARCH.get().trigger(p,state);
    }

    private static boolean checkPeriodic(ServerPlayer p,ResearchState state){
        boolean changed=false;
        var biome=p.level().getBiome(p.blockPosition());
        if(biome.is(BiomeTags.IS_NETHER))changed|=discover(p,state,"m_hellandback","got.hellandback");
        if(biome.is(BiomeTags.IS_END))changed|=discover(p,state,"m_endoftheworld","got.endoftheworld");
        if(state.knows("UNLOCKAUROMANCY@1")&&!state.knows("UNLOCKAUROMANCY@2")){
            if(p.getY()<p.level().getMinBuildHeight()+10)changed|=discover(p,state,"m_deepdown","got.deepdown");
            if(p.getY()>p.level().getMaxBuildHeight()*.4)changed|=discover(p,state,"m_uphigh","got.uphigh");
        }
        if(p.getStats().getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM))>160000)changed|=state.flag("m_walker");
        if(p.getStats().getValue(Stats.CUSTOM.get(Stats.SPRINT_ONE_CM))>80000)changed|=state.flag("m_runner");
        if(p.getStats().getValue(Stats.CUSTOM.get(Stats.JUMP))>500)changed|=state.flag("m_jumper");
        if(p.getStats().getValue(Stats.CUSTOM.get(Stats.SWIM_ONE_CM))>8000)changed|=state.flag("m_swimmer");
        return changed;
    }

    /** Standing in fire is insufficient: original focus discoveries require a damage event. */
    @SubscribeEvent
    public static void damaged(LivingDamageEvent.Pre event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        var storage=ResearchStorage.get(player.getServer());var state=storage.player(player.getUUID());
        boolean changed=false;
        if(event.getSource().is(DamageTypeTags.IS_FIRE)&&state.knows("BASEAUROMANCY@2"))
            changed|=discover(player,state,"f_onfire","got.onfire");
        if(state.knows("FOCUSPROJECTILE@2")){
            var projectile=event.getSource().getDirectEntity();
            if(projectile instanceof AbstractArrow)changed|=discover(player,state,"f_arrow","got.projectile");
            if(projectile instanceof Fireball||projectile instanceof DragonFireball||projectile instanceof WitherSkull)
                changed|=discover(player,state,"f_fireball","got.projectile");
            if(projectile instanceof LlamaSpit)changed|=discover(player,state,"f_spit","got.projectile");
        }
        if(changed){storage.setDirty();ResearchProgress.sync(player);}
    }

    @SubscribeEvent
    public static void pickedUp(ItemEntityPickupEvent.Post event){
        if(!(event.getPlayer() instanceof ServerPlayer player))return;
        var storage=ResearchStorage.get(player.getServer());var state=storage.player(player.getUUID());
        var stack=event.getOriginalStack();boolean changed=false;
        if(stack.getItem() instanceof CrystalItem&&state.flag("!gotcrystals")){
            changed=true;
            message(player,"got.crystals",false);
            if(thaumcraft.port.world.WorldConfig.NO_SLEEP.get())giveDreamJournal(player,state);
        }
        if(stack.is(PortContent.THAUMONOMICON.get()))changed|=state.flag("!gotthaumonomicon");
        if(changed){storage.setDirty();ResearchProgress.sync(player);}
    }

    @SubscribeEvent
    public static void wokeUp(PlayerWakeUpEvent event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        var storage=ResearchStorage.get(player.getServer());var state=storage.player(player.getUUID());
        if(!state.knows("!gotcrystals")||!giveDreamJournal(player,state))return;
        storage.setDirty();ResearchProgress.sync(player);
    }

    private static boolean giveDreamJournal(ServerPlayer player,ResearchState state){
        if(!state.flag("!gotdream"))return false;
        var journal=DreamJournal.create(player.getGameProfile().getName());
        if(!player.getInventory().add(journal))player.drop(journal,false);
        player.getInventory().setChanged();
        message(player,"got.dream",false);
        return true;
    }

    private static boolean discover(ServerPlayer player,ResearchState state,String key,String translation){
        if(!state.flag(key))return false;
        message(player,translation,true);
        return true;
    }
    private static void message(ServerPlayer player,String key,boolean actionbar){
        player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE),actionbar);
    }
    private ResearchEvents(){}
}
