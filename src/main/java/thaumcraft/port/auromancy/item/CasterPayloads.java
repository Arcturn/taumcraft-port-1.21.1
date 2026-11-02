package thaumcraft.port.auromancy.item;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class CasterPayloads {
    public record Action(int hand, int action, String selection) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(ResourceLocation.parse("thaumcraft:caster_action"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Action> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Action::hand,
                ByteBufCodecs.VAR_INT, Action::action, ByteBufCodecs.stringUtf8(512), Action::selection, Action::new);
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Aura(int base, float vis) implements CustomPacketPayload {
        public static final Type<Aura> TYPE = new Type<>(ResourceLocation.parse("thaumcraft:caster_aura"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Aura> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Aura::base, ByteBufCodecs.FLOAT, Aura::vis, Aura::new);
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public static void sendAura(ServerPlayer player, int base, float vis) { PacketDistributor.sendToPlayer(player, new Aura(base, vis)); }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("9").playToServer(Action.TYPE, Action.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || payload.hand() < 0 || payload.hand() > 1) return;
            var hand = InteractionHand.values()[payload.hand()];
            if (payload.action() == 0) CasterManager.changeFocus(player, hand, payload.selection());
            else if (payload.action() >= 1 && payload.action() <= 3) CasterManager.toggleMisc(player, hand, payload.action() - 1);
        }));
        event.registrar("9").playToClient(Aura.TYPE, Aura.CODEC, (payload, context) -> context.enqueueWork(() -> {
            thaumcraft.port.client.auromancy.CasterClient.auraBase = payload.base();
            thaumcraft.port.client.auromancy.CasterClient.auraVis = payload.vis();
        }));
    }
    private CasterPayloads() { }
}
