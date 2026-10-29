package thaumcraft.port.research;

import java.util.Collection;
import net.minecraft.nbt.*;

/** Schema three separates presentation flags from gameplay discoveries and stages. */
public final class ResearchCodec {
    public static final int SCHEMA=3;
    public static ResearchState read(CompoundTag tag) {
        var state=new ResearchState(strings(tag,"scanned"),strings(tag,"discovered"));
        state.discovered().forEach(a->state.restoreFlag("!"+a));
        var oldFlags=strings(tag,"flags");oldFlags.forEach(state::restoreFlag);strings(tag,"crafted").forEach(state::craft);
        LegacyScanKeys.migrate(state);
        var stages=tag.getCompound("stages");stages.getAllKeys().forEach(k->state.restoreStage(k,stages.getInt(k)));
        var knowledge=tag.getCompound("knowledge");knowledge.getAllKeys().forEach(k->{
            var parts=k.split(";",-1);if(parts.length==2)state.addKnowledge(parts[0],parts[1],Math.max(0,knowledge.getInt(k)));
        });
        state.warp(tag.getInt("warpPermanent"),tag.getInt("warpNormal"),tag.getInt("warpTemporary"));
        if(tag.getInt("schema")>=SCHEMA){
            var flags=tag.getCompound("research_flags");
            for(String key:flags.getAllKeys())for(String name:strings(flags,key))try{state.setResearchFlag(key,ResearchState.ResearchFlag.valueOf(name));}catch(IllegalArgumentException ignored){}
        }else migrateReadMarkers(state,oldFlags);
        return state;
    }
    public static CompoundTag write(ResearchState state) {
        var tag=new CompoundTag();tag.putInt("schema",SCHEMA);
        tag.put("scanned",list(state.scanned()));tag.put("discovered",list(state.discovered()));
        tag.put("flags",list(state.flags()));tag.put("crafted",list(state.crafted()));
        var stages=new CompoundTag();state.stages().forEach(stages::putInt);tag.put("stages",stages);
        var knowledge=new CompoundTag();state.knowledge().forEach(knowledge::putInt);tag.put("knowledge",knowledge);
        var researchFlags=new CompoundTag();state.researchFlags().forEach((key,value)->researchFlags.put(key,list(value.stream().map(Enum::name).sorted().toList())));tag.put("research_flags",researchFlags);
        tag.putInt("warpPermanent",state.permanentWarp());tag.putInt("warpNormal",state.normalWarp());tag.putInt("warpTemporary",state.temporaryWarp());return tag;
    }
    private static java.util.List<String> strings(CompoundTag tag,String key) { return tag.getList(key,Tag.TAG_STRING).stream().map(Tag::getAsString).toList(); }
    private static ListTag list(Collection<String> values) {var list=new ListTag();values.forEach(s->list.add(StringTag.valueOf(s)));return list;}
    private static void migrateReadMarkers(ResearchState state,Collection<String> flags){
        for(var entry:ResearchCatalog.all())if(state.isResearchKnown(entry.key())){
            int stage=state.stage(entry.key()),addenda=ResearchView.addenda(state,entry),seenStage=-1,seenAddenda=-1;boolean read=false;
            String prefix="ui_seen:"+entry.key()+":";
            for(String flag:flags)if(flag.startsWith(prefix))try{
                String[] parts=flag.substring(prefix.length()).split(":",-1);if(parts.length!=2)continue;
                int s=Integer.parseInt(parts[0]),a=Integer.parseInt(parts[1]);if(s<0||a<0)continue;
                seenStage=Math.max(seenStage,s);seenAddenda=Math.max(seenAddenda,a);read|=s==stage&&a==addenda;
            }catch(NumberFormatException ignored){}
            if(read)continue;
            if(seenStage<stage||seenStage<0)state.setResearchFlag(entry.key(),ResearchState.ResearchFlag.RESEARCH);
            if(seenAddenda>=0&&addenda>seenAddenda)state.setResearchFlag(entry.key(),ResearchState.ResearchFlag.PAGE);
        }
        // Old snapshots cannot tell us whether a popup has already been displayed. Do not replay it.
    }
    private ResearchCodec() {}
}
