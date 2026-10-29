package thaumcraft.port.world;

import java.util.List;
import java.util.Random;

/** TC6 aura equations, independent of Minecraft and of background threads. */
public final class AuraState {
    public static final float LIMIT = 32766;
    private static final float[] REGEN = {.25f,.15f,.1f,.05f,0,.05f,.1f,.15f};
    private static final float[] MAX = {1.15f,1.05f,1,.95f,.85f,.95f,1,1.05f};
    private int base;
    private float vis, flux;
    public AuraState() { this(-1, 0, 0); }
    public AuraState(int base, float vis, float flux) {
        this.base = Math.max(-1, Math.min(500, base));
        this.vis = clean(vis); this.flux = clean(flux);
    }
    public static AuraState generate(long seed, int x, int z, double biomeLife) {
        Random random = new Random(seed ^ (x * 341873128712L) ^ (z * 132897987541L) ^ 0x544841554dL);
        int base = (int)Math.max(0, Math.min(500, biomeLife * 500 * (1 + random.nextGaussian() * .1)));
        return new AuraState(base, base, 0);
    }
    public int base() { return base; }
    public float vis() { return vis; }
    public float flux() { return flux; }
    private static float clean(float n) { return Float.isFinite(n) ? Math.max(0, Math.min(LIMIT, n)) : 0; }
    public void addVis(float n) { if (Float.isFinite(n) && n > 0) vis = clean(vis + n); }
    public void addFlux(float n) { if (Float.isFinite(n) && n > 0) flux = clean(flux + n); }
    public float drain(boolean polluted, float n, boolean simulate) {
        if (!Float.isFinite(n) || n <= 0) return 0;
        float amount = Math.min(polluted ? flux : vis, n);
        if (!simulate) { if (polluted) flux -= amount; else vis -= amount; }
        return amount;
    }
    /** Neighbours are already shuffled; only currently loaded chunks are passed here. */
    public boolean step(List<AuraState> neighbours, int moon, float conversionRoll, float riftRoll) {
        if (base < 0) return false;
        int phase = Math.floorMod(moon, 8);
        float capacity = base * MAX[phase];
        AuraState lowVis = null, lowFlux = null;
        for (AuraState n : neighbours) {
            if (n.base < 0) continue;
            if (n.vis + n.flux < n.base * MAX[phase] && (lowVis == null || n.vis < lowVis.vis)) lowVis = n;
            if (lowFlux == null || n.flux < lowFlux.flux) lowFlux = n;
        }
        if (lowVis != null && lowVis.vis < vis && lowVis.vis / vis < .75f) {
            float n = Math.min(1, Math.min(vis - lowVis.vis, LIMIT - lowVis.vis));
            vis -= n; lowVis.addVis(n);
        }
        if (lowFlux != null && flux > Math.max(5, base / 10f) && lowFlux.flux < flux / 1.75f) {
            float n = Math.min(1, Math.min(flux - lowFlux.flux, LIMIT - lowFlux.flux));
            flux -= n; lowFlux.addFlux(n);
        }
        float pollution = .25f - REGEN[phase];
        if (vis + flux < capacity) addVis(Math.min(capacity - vis - flux, REGEN[phase]));
        else if (vis > capacity * 1.25f && conversionRoll < .1f) addFlux(drain(false, pollution, false));
        else if (vis <= capacity * .1f && vis >= flux && conversionRoll < .1f) addFlux(pollution);
        return flux > capacity * .75f && riftRoll < flux / 5000f;
    }
}
