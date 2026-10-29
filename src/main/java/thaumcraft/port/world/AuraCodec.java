package thaumcraft.port.world;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
/** Storage format isolated from registration so corrupted and round-trip data can be tested directly. */
public final class AuraCodec {
    public static final Codec<AuraState> CODEC=RecordCodecBuilder.create(i->i.group(
            Codec.INT.fieldOf("base").forGetter(AuraState::base),Codec.FLOAT.fieldOf("vis").forGetter(AuraState::vis),
            Codec.FLOAT.fieldOf("flux").forGetter(AuraState::flux)).apply(i,AuraState::new));
    private AuraCodec(){}
}
