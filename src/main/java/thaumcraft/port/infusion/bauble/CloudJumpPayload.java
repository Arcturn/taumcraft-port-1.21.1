package thaumcraft.port.infusion.bauble;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** The client requests an air jump, never supplies velocity, player ID or a reset flag. */
public record CloudJumpPayload() implements CustomPacketPayload {
    public static final Type<CloudJumpPayload> TYPE=new Type<>(ResourceLocation.parse("thaumcraft:cloud_jump"));
    public static final StreamCodec<RegistryFriendlyByteBuf,CloudJumpPayload> CODEC=StreamCodec.unit(new CloudJumpPayload());
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public record Effect(int entity) implements CustomPacketPayload {
        public static final Type<Effect> TYPE=new Type<>(ResourceLocation.parse("thaumcraft:cloud_jump_effect"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Effect> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Effect::entity,Effect::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public static void register(RegisterPayloadHandlersEvent event){
        var registrar=event.registrar("9");
        registrar.playToServer(TYPE,CODEC,(payload,context)->context.enqueueWork(()->{if(context.player() instanceof ServerPlayer player)BaubleEvents.requestJump(player);}));
        registrar.playToClient(Effect.TYPE,Effect.CODEC,(payload,context)->context.enqueueWork(()->thaumcraft.port.client.InfusionBaublesClient.cloudEffect(payload.entity())));
    }
}
