package thaumcraft.port.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** UI intent is validated by the server; neither action bypasses any requirement. */
public record ResearchActionPayload(String key,int expectedStage,Action action) implements CustomPacketPayload {
    public enum Action { PAGE, TERMINAL_NODE;
        private static Action decode(int value){if(value<0||value>=values().length)throw new IllegalArgumentException("Unknown research action");return values()[value];}
    }
    public ResearchActionPayload(String key,int expectedStage){this(key,expectedStage,Action.PAGE);}
    public static final Type<ResearchActionPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft","research_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf,ResearchActionPayload> STREAM_CODEC=StreamCodec.composite(
        ByteBufCodecs.stringUtf8(128),ResearchActionPayload::key,ByteBufCodecs.VAR_INT,ResearchActionPayload::expectedStage,
        ByteBufCodecs.VAR_INT.map(Action::decode,Action::ordinal),ResearchActionPayload::action,ResearchActionPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
