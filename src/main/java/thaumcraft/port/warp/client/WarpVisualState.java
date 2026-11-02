package thaumcraft.port.warp.client;

/** Tick and frame state from TC6 PacketMiscEvent/RenderEventHandler, independent of the renderer. */
public final class WarpVisualState {
    private boolean mist;
    private int mistTicks, vignetteTicks;
    private float density, vignette;
    private boolean darken;

    public void mist(boolean shortMist) {
        mist = true;
        mistTicks = shortMist ? Math.max(mistTicks, 200) : 2400;
    }
    public void tick(boolean deathGaze) {
        if (deathGaze) vignetteTicks = 10;
        darken = vignetteTicks > 0;
        if (darken) vignetteTicks--;
        if (mist) {
            if (mistTicks < 100) density = .1f * (mistTicks / 100f);
            else if (density < .1f) density += .001f;
            if (--mistTicks < 0) { mist = false; density = 0; }
        }
    }
    public float vignette(float ticks) {
        vignette += ((darken ? 1f : 0f) - vignette) * .01f;
        return vignette * (1f + (float)Math.sin(ticks / 2f) * .1f);
    }
    public float density() { return mist ? density : 0; }
    public int mistTicks() { return mistTicks; }
    public boolean hasVignette() { return darken || vignette > .0001f; }
    public void reset() {
        mist = darken = false;
        mistTicks = vignetteTicks = 0;
        density = vignette = 0;
    }
}
