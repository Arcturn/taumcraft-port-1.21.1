package thaumcraft.port.client;

import net.minecraft.client.ParticleStatus;

/** BETA26 ParticleEngine admission: reduced/minimal settings and the low-FPS fallback. */
final class FocusParticleAdmission {
    static boolean allows(ParticleStatus setting,int fps,int roll) {
        int reduction=switch(setting){case ALL -> 0;case DECREASED -> 1;case MINIMAL -> 2;};
        if(fps<30)reduction++;
        return roll>=reduction;
    }
    private FocusParticleAdmission() { }
}
