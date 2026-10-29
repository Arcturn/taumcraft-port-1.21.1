package thaumcraft.port.research;

import com.google.gson.JsonObject;
import java.util.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.network.JournalPayload;

/** All requirements are validated before inventory or knowledge is consumed. */
@net.neoforged.fml.common.EventBusSubscriber(modid="thaumcraft")
public final class ResearchProgress {
    @net.neoforged.bus.api.SubscribeEvent public static void login(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event){if(event.getEntity() instanceof ServerPlayer player)sync(player);}
    @net.neoforged.bus.api.SubscribeEvent public static void respawn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event){if(event.getEntity() instanceof ServerPlayer player)sync(player);}
    public static void show(ServerPlayer player) {
        var storage=ResearchStorage.get(player.getServer());var state=storage.player(player.getUUID());
        if(state.flag("!gotthaumonomicon"))storage.setDirty();
        deliverAddenda(player,state);
        PacketDistributor.sendToPlayer(player,new JournalPayload(state.scanned().size(),String.join(",",state.discovered()),snapshot(storage,state)));
    }
    public static void craft(ServerPlayer player,ItemStack output) {
        if(output.isEmpty())return;
        var storage=ResearchStorage.get(player.getServer());
        if(storage.player(player.getUUID()).craft(LegacyRequirement.craftKey(output)))storage.setDirty();
    }
    public static void sync(ServerPlayer player){
        var storage=ResearchStorage.get(player.getServer());var s=storage.player(player.getUUID());deliverAddenda(player,s);var tag=snapshot(storage,s);
        var aura=thaumcraft.port.world.AuraSystem.get(player.serverLevel(),player.blockPosition());if(aura!=null){tag.putFloat("auraBase",aura.base());tag.putFloat("auraVis",aura.vis());tag.putFloat("auraFlux",aura.flux());}
        var target=Scanner.target(player,9,player.blockInteractionRange());String subject=Scanner.subject(player,target);tag.putString("scanTarget",Scanner.readoutKey(player,target));
        if(s.scanned().contains(subject)){var aspects=new net.minecraft.nbt.CompoundTag();Scanner.aspects(player,target).forEach((a,n)->aspects.putInt(a.id(),n));tag.put("scanAspects",aspects);}
        PacketDistributor.sendToPlayer(player,new JournalPayload(s.scanned().size(),String.join(",",s.discovered()),tag,false));
    }
    /** TC6 snapshots pending POPUP flags, then clears them on the server before the next sync. */
    static net.minecraft.nbt.CompoundTag snapshot(ResearchStorage storage,ResearchState state){var tag=ResearchCodec.write(state);if(state.clearPopups())storage.setDirty();return tag;}
    private static void deliverAddenda(ServerPlayer player,ResearchState state){
        for(String key:state.drainAddendumMessages()){
            var entry=ResearchCatalog.get(key);
            if(entry!=null)player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("tc.addaddendum",net.minecraft.network.chat.Component.translatable(entry.title())));
        }
    }
    public static void acknowledge(ServerPlayer player,String key){
        if(!player.isAlive()||(!player.getMainHandItem().is(PortContent.THAUMONOMICON.get())&&!player.getOffhandItem().is(PortContent.THAUMONOMICON.get())))return;
        if(ResearchCatalog.get(key)==null)return;
        var storage=ResearchStorage.get(player.getServer());var state=storage.player(player.getUUID());
        if(!state.isResearchKnown(key))return;
        if(state.clearReadFlags(key))storage.setDirty();
        sync(player);
    }
    public static void advance(ServerPlayer player,String key,int expectedStage) {
        advance(player,key,expectedStage,false);
    }
    /** The original browser checks the final stage on opening an already-known node. */
    public static void openTerminal(ServerPlayer player,String key,int expectedStage) {
        var entry=ResearchCatalog.get(key);
        if(entry==null||expectedStage<=1||expectedStage<entry.stages().size())return;
        advance(player,key,expectedStage,true);
    }
    private static void advance(ServerPlayer player,String key,int expectedStage,boolean notifyResearch) {
        if(!player.isAlive()||(!player.getMainHandItem().is(PortContent.THAUMONOMICON.get())&&!player.getOffhandItem().is(PortContent.THAUMONOMICON.get())))return;
        var entry=ResearchCatalog.get(key);if(entry==null)return;
        var storage=ResearchStorage.get(player.getServer());var state=storage.player(player.getUUID());int current=state.stage(key);
        if(current!=expectedStage||!state.available(entry)||state.knows(key))return;
        if(current>0) {
            JsonObject stage=entry.stages().get(current-1);
            if(!ResearchCatalog.strings(stage,"required_research").stream().allMatch(state::knows))return;
            if(!ResearchCatalog.strings(stage,"required_craft").stream().map(LegacyRequirement::parse).allMatch(r->state.crafted().contains(r.craftKey())))return;
            Map<String,Integer> costs=new LinkedHashMap<>();
            for(String encoded:ResearchCatalog.strings(stage,"required_knowledge")) {
                var parts=encoded.split(";");if(parts.length!=3)return;
                int progression=ResearchState.progression(parts[0]);if(progression==0)return;
                int amount;try{amount=Math.multiplyExact(Integer.parseInt(parts[2]),progression);}catch(RuntimeException e){return;}
                if(amount<0)return;costs.merge(parts[0]+";"+parts[1],amount,Integer::sum);
            }
            for(var cost:costs.entrySet()) {var parts=cost.getKey().split(";");if(state.knowledge(parts[0],parts[1])<cost.getValue())return;}
            var inventory=player.getInventory();int[] take=new int[inventory.getContainerSize()];
            for(String encoded:ResearchCatalog.strings(stage,"required_item")) {
                var requirement=LegacyRequirement.parse(encoded);int needed=requirement.count();
                for(int slot=0;slot<take.length&&needed>0;slot++) {
                    var stack=inventory.getItem(slot);if(!requirement.matches().test(stack))continue;
                    int n=Math.min(needed,stack.getCount()-take[slot]);take[slot]+=n;needed-=n;
                }
                if(needed>0)return;
            }
            // No state mutation precedes this point. Calls are made on the server main thread.
            for(int slot=0;slot<take.length;slot++)if(take[slot]>0)inventory.removeItem(slot,take[slot]);
            costs.forEach((k,n)->{var parts=k.split(";");state.addKnowledge(parts[0],parts[1],-n);});
            inventory.setChanged();
        }
        int next=current+1;
        if(next==entry.stages().size()&&noRequirements(entry.stages().getLast()))next++;
        applyTransitionWarp(state,entry,current,next);
        // TC6 consumes noFlags on the first completion, which can be a sibling.
        // Keep it within this request instead of leaking the original static flag across players.
        var notifications=new Notifications(!notifyResearch);
        state.stage(key,next,next>entry.stages().size()&&notifications.notifyCompletion());
        player.giveExperiencePoints(5);
        // TC6 siblings are explicit auto-unlocks, including hidden discovery pages.
        for(String sibling:entry.siblings()) {
            var other=ResearchCatalog.get(sibling);
            if(other==null)state.flag(sibling);
            else completeSibling(player,state,other,notifications);
        }
        storage.setDirty();player.level().playSound(null,player.blockPosition(),PortSounds.get("learn"),SoundSource.PLAYERS,.6f,1);
    }
    private static final class Notifications {
        private boolean suppress;
        Notifications(boolean suppress){this.suppress=suppress;}
        boolean notifyCompletion(){boolean notify=!suppress;suppress=false;return notify;}
    }
    private static void completeSibling(ServerPlayer player,ResearchState state,ResearchCatalog.Entry entry,Notifications notifications){
        if(state.knows(entry.key())||!entry.parents().stream().allMatch(state::knows))return;
        int current=state.stage(entry.key()),transitions=0;
        while(current<=entry.stages().size()){
            int next=current+1;
            if(next==entry.stages().size()&&noRequirements(entry.stages().getLast()))next++;
            applyTransitionWarp(state,entry,current,next);
            current=next;transitions++;
        }
        state.stage(entry.key(),current,notifications.notifyCompletion());
        player.giveExperiencePoints(5*transitions);
        for(String key:entry.siblings()){
            var sibling=ResearchCatalog.get(key);
            if(sibling==null)state.flag(key);else completeSibling(player,state,sibling,notifications);
        }
    }
    private static boolean noRequirements(JsonObject stage) {
        return List.of("required_item","required_craft","required_research","required_knowledge").stream().allMatch(k->ResearchCatalog.strings(stage,k).isEmpty());
    }
    /** TC6 adds outgoing-stage warp and, on completion, final-stage warp (otherwise outgoing again). */
    private static void applyTransitionWarp(ResearchState state,ResearchCatalog.Entry entry,int current,int next){
        if(entry.stages().isEmpty())return;
        int outgoing=current>0?stageWarp(entry.stages().get(Math.min(current,entry.stages().size())-1)):0;
        int warp=outgoing+(next>entry.stages().size()?stageWarp(entry.stages().getLast()):outgoing);
        if(warp>0)state.warp(state.permanentWarp()+(warp+1)/2,state.normalWarp()+warp/2,state.temporaryWarp());
    }
    private static int stageWarp(JsonObject stage){return stage.has("warp")?stage.get("warp").getAsInt():0;}
    private ResearchProgress() {}
}
