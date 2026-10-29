package thaumcraft.port.research;

import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import thaumcraft.port.aspect.Aspect;

public final class Scanning {
    private Scanning() {}

    public static String itemSubject(ItemStack stack) {
        String id = "item:" + BuiltInRegistries.ITEM.getKey(stack.getItem());
        if(stack.is(thaumcraft.port.essentia.EssentiaContent.PHIAL.get())&&thaumcraft.port.essentia.EssentiaItem.aspect(stack)!=null)return id+"/filled";
        if(stack.is(thaumcraft.port.content.PortContent.CELESTIAL_NOTES.get()))return id+"/"+stack.getOrDefault(thaumcraft.port.content.PortContent.CELESTIAL_KIND.get(),0);
        return id;
    }

    public static void scan(ServerPlayer player, String subject, Map<Aspect, Integer> aspects) {
        scan(player,subject,aspects,true);
    }
    public static void scan(ServerPlayer player,String subject,Map<Aspect,Integer> aspects,boolean message){
        scan(player,new ScanObservation(subject,aspects,ScanDiscoveries.subjectFlags(subject)),message);
    }
    public static void scan(ServerPlayer player,ScanObservation observation,boolean message){
        ResearchStorage data = ResearchStorage.get(player.getServer());
        var state=data.player(player.getUUID());
        boolean changed=observation.learn(state);
        if(changed)data.setDirty();
        if(message)player.displayClientMessage(feedback(changed),true);
    }
    /** ScanningManager uses green/purple italic action-bar feedback, including repeat scans. */
    public static Component feedback(boolean changed){
        return Component.translatable(changed?"tc.knownobject":"tc.unknownobject")
                .withStyle(changed?net.minecraft.ChatFormatting.GREEN:net.minecraft.ChatFormatting.DARK_PURPLE,
                        net.minecraft.ChatFormatting.ITALIC);
    }
}
