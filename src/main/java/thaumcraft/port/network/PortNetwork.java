package thaumcraft.port.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import thaumcraft.port.client.ClientJournal;

public final class PortNetwork {
    private PortNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("9").playToClient(KnowledgeGainPayload.TYPE,KnowledgeGainPayload.STREAM_CODEC,
                (p,c)->c.enqueueWork(()->thaumcraft.port.client.KnowledgeGainHud.receive(p.knowledgeType(),p.category(),p.count())));
        event.registrar("9").playToClient(WispZapPayload.TYPE,WispZapPayload.STREAM_CODEC,
                (p,c)->c.enqueueWork(()->thaumcraft.port.client.WispFx.zap(p)));
        event.registrar("9").playToClient(PollutionFxPayload.TYPE, PollutionFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> thaumcraft.port.client.CrucibleFx.pollution(payload.pos(), payload.amount())));
        event.registrar("9").playToClient(SalisMundusFxPayload.TYPE, SalisMundusFxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> thaumcraft.port.client.SalisMundusFx.receive(payload)));
        event.registrar("9").playToServer(thaumcraft.port.theory.TheoryActionPayload.TYPE,thaumcraft.port.theory.TheoryActionPayload.STREAM_CODEC,(p,c)->c.enqueueWork(()->{
            if(c.player() instanceof net.minecraft.server.level.ServerPlayer sp&&sp.containerMenu instanceof thaumcraft.port.theory.TheoryMenu menu)menu.action(sp,p);
        }));
        event.registrar("9").playToClient(thaumcraft.port.theory.TheorySyncPayload.TYPE,thaumcraft.port.theory.TheorySyncPayload.STREAM_CODEC,(p,c)->c.enqueueWork(()->{
            if(c.player().containerMenu instanceof thaumcraft.port.theory.TheoryMenu menu&&menu.containerId==p.container())menu.receive(p.token(),p.data());
        }));
        event.registrar("9").playToServer(BookReadPayload.TYPE,BookReadPayload.STREAM_CODEC,(p,c)->c.enqueueWork(()->{
            if(!(c.player() instanceof net.minecraft.server.level.ServerPlayer player))return;
            thaumcraft.port.research.ResearchProgress.acknowledge(player,p.key());
        }));
        event.registrar("9").playToServer(ResearchActionPayload.TYPE,ResearchActionPayload.STREAM_CODEC,
            (payload,context)->context.enqueueWork(()->{
                if(context.player() instanceof net.minecraft.server.level.ServerPlayer player)
                    handleResearchAction(player,payload);
            }));
        event.registrar("9").playToClient(WorldFxPayload.TYPE, WorldFxPayload.STREAM_CODEC,
            (payload, context) -> context.enqueueWork(() -> thaumcraft.port.client.ClientFx.receive(payload)));
        event.registrar("9").playToClient(JournalPayload.TYPE, JournalPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientJournal.open(payload)));
    }

    /** Runs on the server main thread; a delayed progress response must not reopen a closed book. */
    public static void handleResearchAction(net.minecraft.server.level.ServerPlayer player,ResearchActionPayload payload){
        if(!player.getMainHandItem().is(thaumcraft.port.content.PortContent.THAUMONOMICON.get())&&!player.getOffhandItem().is(thaumcraft.port.content.PortContent.THAUMONOMICON.get()))return;
        if(player.getCooldowns().isOnCooldown(thaumcraft.port.content.PortContent.THAUMONOMICON.get()))return;
        player.getCooldowns().addCooldown(thaumcraft.port.content.PortContent.THAUMONOMICON.get(),4);
        if(payload.action()==ResearchActionPayload.Action.TERMINAL_NODE)thaumcraft.port.research.ResearchProgress.openTerminal(player,payload.key(),payload.expectedStage());
        else thaumcraft.port.research.ResearchProgress.advance(player,payload.key(),payload.expectedStage());
        thaumcraft.port.research.ResearchProgress.sync(player);
    }
}
