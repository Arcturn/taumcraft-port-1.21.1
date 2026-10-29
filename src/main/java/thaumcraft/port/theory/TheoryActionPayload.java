package thaumcraft.port.theory;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record TheoryActionPayload(int container,String token,long revision,int action,String aids) implements CustomPacketPayload {
    public static final Type<TheoryActionPayload> TYPE=new Type<>(ResourceLocation.parse("thaumcraft:theory_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf,TheoryActionPayload> STREAM_CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,TheoryActionPayload::container,ByteBufCodecs.stringUtf8(36),TheoryActionPayload::token,ByteBufCodecs.VAR_LONG,TheoryActionPayload::revision,ByteBufCodecs.VAR_INT,TheoryActionPayload::action,ByteBufCodecs.stringUtf8(256),TheoryActionPayload::aids,TheoryActionPayload::new);
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
