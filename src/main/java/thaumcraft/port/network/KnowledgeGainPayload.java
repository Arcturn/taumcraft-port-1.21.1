package thaumcraft.port.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.port.research.ResearchCatalog;
import thaumcraft.port.research.ResearchState;

/** One-shot server reward feedback, separate from the repeatedly synchronized knowledge balance. */
public record KnowledgeGainPayload(String knowledgeType, String category, int count) implements CustomPacketPayload {
    public KnowledgeGainPayload {
        int progression=ResearchState.progression(knowledgeType);
        if(progression==0||!ResearchCatalog.CATEGORIES.contains(category)||count<1||count>1000000/progression)
            throw new IllegalArgumentException("Invalid knowledge gain");
    }
    public static final Type<KnowledgeGainPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("thaumcraft","knowledge_gain"));
    public static final StreamCodec<RegistryFriendlyByteBuf,KnowledgeGainPayload> STREAM_CODEC=StreamCodec.composite(
            ByteBufCodecs.stringUtf8(16),KnowledgeGainPayload::knowledgeType,
            ByteBufCodecs.stringUtf8(32),KnowledgeGainPayload::category,
            ByteBufCodecs.VAR_INT,KnowledgeGainPayload::count,KnowledgeGainPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
