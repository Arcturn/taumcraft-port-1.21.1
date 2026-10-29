package thaumcraft.port.research;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Original periodic discovery thresholds; deep exploration follows the new world floor. */
public final class ResearchEvents {
    public static void tick(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer p)||p.tickCount%20!=0)return;
        var storage=ResearchStorage.get(p.getServer());var state=storage.player(p.getUUID());boolean changed=false;
        if(p.level().dimension()==Level.NETHER)changed|=state.flag("m_hellandback");
        if(p.level().dimension()==Level.END)changed|=state.flag("m_endoftheworld");
        if(state.knows("UNLOCKAUROMANCY@1")&&!state.knows("UNLOCKAUROMANCY@2")){
            if(p.getY()<p.level().getMinBuildHeight()+10)changed|=state.flag("m_deepdown");
            if(p.getY()>p.level().getMaxBuildHeight()*.4)changed|=state.flag("m_uphigh");
        }
        if(p.getStats().getValue(Stats.CUSTOM.get(Stats.WALK_ONE_CM))>160000)changed|=state.flag("m_walker");
        if(p.getStats().getValue(Stats.CUSTOM.get(Stats.SPRINT_ONE_CM))>80000)changed|=state.flag("m_runner");
        if(p.getStats().getValue(Stats.CUSTOM.get(Stats.JUMP))>500)changed|=state.flag("m_jumper");
        if(p.getStats().getValue(Stats.CUSTOM.get(Stats.SWIM_ONE_CM))>8000)changed|=state.flag("m_swimmer");
        if(p.isOnFire())changed|=state.flag("f_onfire");
        boolean scanner=p.getMainHandItem().is(thaumcraft.port.content.PortContent.THAUMOMETER.get())||p.getOffhandItem().is(thaumcraft.port.content.PortContent.THAUMOMETER.get());
        if(scanner){var aura=thaumcraft.port.world.AuraSystem.get(p.serverLevel(),p.blockPosition());if(aura!=null&&(aura.flux()>aura.vis()||aura.flux()>aura.base()/3)&&state.stage("FLUX")==0&&!state.knows("FLUX")){changed|=state.stage("FLUX",1);p.displayClientMessage(net.minecraft.network.chat.Component.translatable("research.FLUX.warn").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE),true);}ResearchProgress.sync(p);}
        if(changed)storage.setDirty();
        // Completed listeners are removed by Minecraft. This also catches old saves and /reload.
        ResearchAdvancements.RESEARCH.get().trigger(p,state);
    }
    private ResearchEvents(){}
}
