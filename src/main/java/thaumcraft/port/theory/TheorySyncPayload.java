package thaumcraft.port.theory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record TheorySyncPayload(int container,String token,CompoundTag data) implements CustomPacketPayload {
    public static final Type<TheorySyncPayload> TYPE=new Type<>(ResourceLocation.parse("thaumcraft:theory_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf,TheorySyncPayload> STREAM_CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,TheorySyncPayload::container,ByteBufCodecs.stringUtf8(36),TheorySyncPayload::token,ByteBufCodecs.COMPOUND_TAG,TheorySyncPayload::data,TheorySyncPayload::new);
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
