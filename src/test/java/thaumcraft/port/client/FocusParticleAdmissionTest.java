package thaumcraft.port.client;

import net.minecraft.client.ParticleStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FocusParticleAdmissionTest {
    @Test void userParticleSettingPreservesOriginalThreeWaySampling(){
        assertEquals(3,accepted(ParticleStatus.ALL,60));
        assertEquals(2,accepted(ParticleStatus.DECREASED,60));
        assertEquals(1,accepted(ParticleStatus.MINIMAL,60));
    }
    @Test void lowFpsRuleStartsBelowThirtyAndRemovesOneMoreThird(){
        assertEquals(3,accepted(ParticleStatus.ALL,30));
        assertEquals(2,accepted(ParticleStatus.ALL,29));
        assertEquals(1,accepted(ParticleStatus.DECREASED,29));
        assertEquals(0,accepted(ParticleStatus.MINIMAL,29));
    }
    private static int accepted(ParticleStatus setting,int fps){int accepted=0;for(int roll=0;roll<3;roll++)if(FocusParticleAdmission.allows(setting,fps,roll))accepted++;return accepted;}
}
