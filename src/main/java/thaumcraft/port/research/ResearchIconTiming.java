package thaumcraft.port.research;

/** GuiResearchBrowser selects a research icon using wall-clock seconds. */
public final class ResearchIconTiming {
    public static int frame(int count,long millis) {
        if(count<1)throw new IllegalArgumentException("Empty research icon sequence");
        return (int)Math.floorMod(millis/1000L,count);
    }
    private ResearchIconTiming(){}
}
