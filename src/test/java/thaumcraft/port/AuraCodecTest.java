package thaumcraft.port;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import thaumcraft.port.world.*;
public class AuraCodecTest {
    @Test void persistencePreservesFractionalVisAndFlux(){
        var original=new AuraState(273,251.625f,17.125f);
        var json=AuraCodec.CODEC.encodeStart(JsonOps.INSTANCE,original).getOrThrow();
        var restored=AuraCodec.CODEC.parse(JsonOps.INSTANCE,json).getOrThrow();
        assertEquals(original.base(),restored.base());assertEquals(original.vis(),restored.vis());assertEquals(original.flux(),restored.flux());
    }
    @Test void incompleteDataFailsInsteadOfPretendingToBeAHealthyChunk(){assertTrue(AuraCodec.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString("{\"base\":200}")).error().isPresent());}
    @Test void corruptedAmountsAreClampedOnLoad(){var a=AuraCodec.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseString("{\"base\":99999,\"vis\":-4,\"flux\":999999}")).getOrThrow();assertEquals(500,a.base());assertEquals(0,a.vis());assertEquals(32766,a.flux());}
}
