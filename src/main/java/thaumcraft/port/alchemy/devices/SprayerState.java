package thaumcraft.port.alchemy.devices;

import java.util.*;
import thaumcraft.port.aspect.Aspect;

/** TC6's ten-aspect potion recipe, eight reusable charges and exact per-aspect accounting. */
public final class SprayerState {
    private final Map<Aspect,Integer> recipe=new LinkedHashMap<>(),progress=new EnumMap<>(Aspect.class);
    private int charges;
    public Map<Aspect,Integer> recipe(){return Collections.unmodifiableMap(recipe);}
    public Map<Aspect,Integer> progress(){return Collections.unmodifiableMap(progress);}
    public int charges(){return charges;}
    public void select(Map<Aspect,Integer> aspects){var selected=cull(aspects);recipe.clear();recipe.putAll(selected);progress.clear();charges=0;}
    public Aspect needed(){return recipe.keySet().stream().sorted(Comparator.comparing(Aspect::displayName)).filter(a->progress.getOrDefault(a,0)<recipe.get(a)).findFirst().orElse(null);}
    public int insert(Aspect aspect,int amount,boolean simulate){
        if(aspect==null||amount<=0||charges>=8)return 0;
        int accepted=Math.min(amount,Math.max(0,recipe.getOrDefault(aspect,0)-progress.getOrDefault(aspect,0)));
        if(accepted>0&&!simulate)progress.merge(aspect,accepted,Integer::sum);return accepted;
    }
    public boolean complete(){if(recipe.isEmpty()||charges>=8||needed()!=null)return false;progress.clear();charges++;return true;}
    public boolean spray(){if(charges<=0)return false;charges--;return true;}
    public void restore(Map<Aspect,Integer> aspects,Map<Aspect,Integer> stored,int count){
        select(aspects);charges=Math.clamp(count,0,8);
        recipe.forEach((aspect,required)->{int value=Math.clamp(stored.getOrDefault(aspect,0),0,required);if(value>0)progress.put(aspect,value);});
    }
    /** Original AspectHelper.cullTags(source,10), which removes weak aspects without rescaling costs. */
    public static Map<Aspect,Integer> cull(Map<Aspect,Integer> source){
        var result=new LinkedHashMap<Aspect,Integer>();source.forEach((aspect,amount)->{if(aspect!=null&&amount>0)result.put(aspect,Math.min(32767,amount));});
        while(result.size()>10){
            Aspect lowest=null;float score=Float.POSITIVE_INFINITY;
            for(var entry:result.entrySet()){
                float candidate=entry.getValue();
                if(entry.getKey().isPrimal())candidate*=.9f;
                else for(Aspect component:entry.getKey().components())if(!component.isPrimal()){
                    candidate*=1.1f;for(Aspect child:component.components())if(!child.isPrimal())candidate*=1.05f;
                }
                if(candidate<score){score=candidate;lowest=entry.getKey();}
            }
            result.remove(lowest);
        }
        return Collections.unmodifiableMap(result);
    }
}
