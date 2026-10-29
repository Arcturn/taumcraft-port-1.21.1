package thaumcraft.port.research;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Map;
import java.util.LinkedHashMap;
import thaumcraft.port.aspect.Aspect;

/** Server-owned knowledge, research stages, discovery flags and the three kinds of warp. */
public final class ResearchState {
    public enum ResearchFlag { PAGE, RESEARCH, POPUP }
    private final Set<String> scanned = new LinkedHashSet<>();
    private final Set<String> discovered = new LinkedHashSet<>();
    private final Map<String,Integer> stages = new LinkedHashMap<>();
    private final Map<String,Integer> knowledge = new LinkedHashMap<>();
    private final Set<String> flags = new LinkedHashSet<>();
    private final Set<String> crafted = new LinkedHashSet<>();
    private final Map<String,java.util.EnumSet<ResearchFlag>> researchFlags = new LinkedHashMap<>();
    // Completion effects are not read flags and are never restored from saved data.
    private final java.util.List<String> addendumMessages = new java.util.ArrayList<>();
    private int permanentWarp, normalWarp, temporaryWarp;

    public ResearchState() {}

    public ResearchState(Collection<String> scanned, Collection<String> discovered) {
        for (String subject : scanned) {
            if (subject.equals("item:thaumcraft:celestial_notes")) {
                // Old journals had one shared key: retain its protection against repeat rewards.
                for (int kind = 0; kind <= 12; kind++) this.scanned.add(subject + "/" + kind);
            } else this.scanned.add(scanKey(subject));
        }
        discovered.stream().filter(id -> Aspect.byId(id).isPresent()).forEach(this.discovered::add);
    }

    public boolean recordScan(String subject, Collection<Aspect> aspects) {
        if (subject == null || subject.isBlank() || aspects.isEmpty()) {
            return false;
        }
        discoverAspects(aspects);
        return scanned.add(scanKey(subject));
    }

    /** ScanAspect is independent of ScanGeneric: variants can reveal a new aspect only once. */
    public boolean discoverAspects(Collection<Aspect> aspects) {
        boolean changed = false;
        for (var aspect : aspects) {
            boolean fresh = discovered.add(aspect.id());
            boolean newFlag = flag("!" + aspect.id());
            changed |= fresh || newFlag;
            if (fresh && newFlag) for (String category : java.util.List.of("AUROMANCY", "BASICS", "ALCHEMY"))
                addKnowledge("OBSERVATION", category, 1);
        }
        return changed;
    }

    private static String scanKey(String subject) {
        if (subject.startsWith("entity:thaumcraft:wisp:")) return "entity:thaumcraft:wisp";
        if (subject.startsWith("item:thaumcraft:crystal_essence/")) return "item:thaumcraft:crystal_essence";
        if (subject.startsWith("item:thaumcraft:phial/")) return "item:thaumcraft:phial/filled";
        return subject;
    }

    public Set<String> scanned() { return Collections.unmodifiableSet(scanned); }
    public Set<String> discovered() { return Collections.unmodifiableSet(discovered); }
    public Map<String,Integer> stages() { return Collections.unmodifiableMap(stages); }
    public Map<String,Integer> knowledge() { return Collections.unmodifiableMap(knowledge); }
    public Set<String> flags() { return Collections.unmodifiableSet(flags); }
    public Set<String> crafted() { return Collections.unmodifiableSet(crafted); }
    public boolean flag(String key) {
        if(key==null||key.isBlank()||key.length()>256||key.startsWith("ui_seen:"))return false;
        boolean complete=knows(key),changed=flags.add(key);
        if(changed&&!complete&&ResearchCatalog.get(key)!=null)completed(key,true);
        return changed;
    }
    void restoreFlag(String key){if(key!=null&&!key.isBlank()&&key.length()<=256&&!key.startsWith("ui_seen:"))flags.add(key);}
    public void pruneCelestial(long day){flags.removeIf(k->k.startsWith("CEL_")&&!k.startsWith("CEL_"+day+"_"));}
    public boolean craft(String key) { return key!=null&&!key.isBlank()&&key.length()<=512&&crafted.add(key); }
    public int stage(String key) { return stages.getOrDefault(key,0); }
    /** Administrative completion of catalog records, with no invented scans or currency. */
    public int completeCatalog() {
        int changed=0;
        for(var entry:ResearchCatalog.all())if(stage(entry.key())<=entry.stages().size()){stage(entry.key(),entry.stages().size()+1);changed++;}
        return changed;
    }
    public boolean stage(String key,int stage) {return stage(key,stage,true);}
    /** Book requests suppress the RESEARCH badge for the page already being read. */
    public boolean stage(String key,int stage,boolean notifyResearch) {
        if(key==null||key.isBlank()||key.length()>128||stage<1||stage>256)return false;
        int before=stage(key);boolean complete=knows(key);
        boolean changed=!java.util.Objects.equals(stages.put(key,stage),stage);
        if(changed){
            if(!complete&&knows(key))completed(key,notifyResearch);
            else if(before==0&&notifyResearch&&ResearchCatalog.get(key)!=null){setResearchFlag(key,ResearchFlag.RESEARCH);setResearchFlag(key,ResearchFlag.POPUP);}
        }
        return changed;
    }
    void restoreStage(String key,int stage){if(key!=null&&!key.isBlank()&&key.length()<=128&&stage>=1&&stage<=256)stages.put(key,stage);}
    public boolean isResearchKnown(String key){return stage(key)>0||knows(key);}
    public boolean hasResearchFlag(String key,ResearchFlag flag){var set=researchFlags.get(key);return set!=null&&set.contains(flag);}
    public boolean setResearchFlag(String key,ResearchFlag flag){return flag!=null&&ResearchCatalog.get(key)!=null&&researchFlags.computeIfAbsent(key,k->java.util.EnumSet.noneOf(ResearchFlag.class)).add(flag);}
    public boolean clearResearchFlag(String key,ResearchFlag flag){var set=researchFlags.get(key);if(set==null||!set.remove(flag))return false;if(set.isEmpty())researchFlags.remove(key);return true;}
    public boolean clearReadFlags(String key){boolean changed=clearResearchFlag(key,ResearchFlag.RESEARCH);return clearResearchFlag(key,ResearchFlag.PAGE)||changed;}
    public Map<String,Set<ResearchFlag>> researchFlags(){var copy=new LinkedHashMap<String,Set<ResearchFlag>>();researchFlags.forEach((key,value)->copy.put(key,Set.copyOf(value)));return Collections.unmodifiableMap(copy);}
    public Set<String> popupResearch(){return researchFlags.entrySet().stream().filter(e->e.getValue().contains(ResearchFlag.POPUP)).map(Map.Entry::getKey).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));}
    public boolean clearPopups(){boolean changed=false;for(String key:popupResearch())changed|=clearResearchFlag(key,ResearchFlag.POPUP);return changed;}
    public java.util.List<String> drainAddendumMessages(){var pending=java.util.List.copyOf(addendumMessages);addendumMessages.clear();return pending;}
    private void completed(String key,boolean notifyResearch){
        setResearchFlag(key,ResearchFlag.POPUP);
        if(notifyResearch)setResearchFlag(key,ResearchFlag.RESEARCH);
        // TC6 marks an already-completed entry when a completed prerequisite is named
        // by an addendum; PAGE is independent of that entry's RESEARCH notification.
        for(var entry:ResearchCatalog.all())if(knows(entry.key())&&entry.addenda().stream().anyMatch(a->ResearchCatalog.strings(a,"required_research").contains(key))){setResearchFlag(entry.key(),ResearchFlag.PAGE);addendumMessages.add(entry.key());}
    }
    public boolean knows(String expression) {
        if(expression==null)return false;
        if(expression.isBlank())return true;
        if(expression.contains("||"))return java.util.Arrays.stream(expression.split("\\|\\|")).anyMatch(this::knows);
        if(expression.contains("&&"))return java.util.Arrays.stream(expression.split("&&")).allMatch(this::knows);
        String key=expression.startsWith("~")?expression.substring(1):expression;
        int at=key.indexOf('@');
        if(at>=0)try {String base=key.substring(0,at);int reached=stage(base);var entry=ResearchCatalog.get(base);if(flags.contains(base)&&entry!=null)reached=Math.max(reached,entry.stages().size()+1);return reached>=Integer.parseInt(key.substring(at+1));}catch(NumberFormatException e){return false;}
        if(flags.contains(key))return true;
        var entry=ResearchCatalog.get(key);
        return entry!=null&&stage(key)>entry.stages().size();
    }
    public boolean available(ResearchCatalog.Entry entry) {
        return entry!=null&&entry.parents().stream().allMatch(this::knows)
            &&(!entry.meta().contains("HIDDEN")||!entry.parents().isEmpty()||stage(entry.key())>0||flags.contains(entry.key()));
    }
    public int knowledge(String type,String category) { return knowledge.getOrDefault(type+";"+category,0); }
    public static int progression(String type) { return type.equals("OBSERVATION")?16:type.equals("THEORY")?32:0; }
    public boolean addKnowledge(String type,String category,int amount) {
        if(progression(type)==0||!ResearchCatalog.CATEGORIES.contains(category))return false;
        long next=(long)knowledge(type,category)+amount;
        if(next<0||next>1000000)return false;
        knowledge.put(type+";"+category,(int)next);return true;
    }
    public int permanentWarp(){return permanentWarp;}
    public int normalWarp(){return normalWarp;}
    public int temporaryWarp(){return temporaryWarp;}
    public void warp(int permanent,int normal,int temporary) {
        permanentWarp=Math.clamp(permanent,0,10000);normalWarp=Math.clamp(normal,0,10000);temporaryWarp=Math.clamp(temporary,0,10000);
    }
}
