package thaumcraft.port.client;

import net.minecraft.client.Minecraft;
import thaumcraft.port.network.JournalPayload;

/** Loaded only when a client receives the journal payload. */
public final class ClientJournal {
    public static thaumcraft.port.research.ResearchState state=new thaumcraft.port.research.ResearchState();
    public static float auraBase,auraVis,auraFlux;
    public static String scanTarget="";
    public static java.util.Map<thaumcraft.port.aspect.Aspect,Integer> scanAspects=java.util.Map.of();
    private ClientJournal() {}
    public static void open(JournalPayload payload) {
        state=thaumcraft.port.research.ResearchCodec.read(payload.progress());
        var game=Minecraft.getInstance();
        for(String key:state.popupResearch()){var entry=thaumcraft.port.research.ResearchCatalog.get(key);if(entry!=null)game.getToasts().addToast(new ResearchToast(entry));}
        if(state.clearPopups())payload.progress().put("research_flags",thaumcraft.port.research.ResearchCodec.write(state).get("research_flags"));
        if(payload.progress().contains("auraBase")){auraBase=payload.progress().getFloat("auraBase");auraVis=payload.progress().getFloat("auraVis");auraFlux=payload.progress().getFloat("auraFlux");}
        scanTarget=payload.progress().getString("scanTarget");var as=payload.progress().getCompound("scanAspects");var map=new java.util.EnumMap<thaumcraft.port.aspect.Aspect,Integer>(thaumcraft.port.aspect.Aspect.class);for(String key:as.getAllKeys())thaumcraft.port.aspect.Aspect.byId(key).ifPresent(a->map.put(a,as.getInt(key)));scanAspects=java.util.Map.copyOf(map);
        if(game.screen instanceof ResearchScreen screen)screen.update(payload);
        else if(game.screen instanceof ResearchPageScreen screen)screen.update(payload);
        else if(payload.open())game.setScreen(new ResearchScreen(payload));
    }
}
