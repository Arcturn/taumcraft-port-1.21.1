package thaumcraft.port.machine;

import java.util.*;
import thaumcraft.port.aspect.Aspect;

/** TC6 heat thresholds, grace periods, water cost and pollution conversion. No world dependency. */
public final class CrucibleState {
    private int water,heat,timer=-100;
    private final Map<Aspect,Integer> aspects=new EnumMap<>(Aspect.class);
    public int water(){return water;}
    public int heat(){return heat;}
    public int timer(){return timer;}
    public Map<Aspect,Integer> aspects(){return Collections.unmodifiableMap(aspects);}
    public int total(){return aspects.values().stream().mapToInt(Integer::intValue).sum();}
    public boolean boiling(){return water>0&&heat>150;}
    public int fill(int amount,boolean simulate){int n=Math.clamp(amount,0,1000-water);if(!simulate)water+=n;return n;}
    public int drain(int amount,boolean simulate){int n=Math.clamp(amount,0,water);if(!simulate)water-=n;return n;}
    public boolean tick(boolean heated){int previous=heat;heat=Math.clamp(heat+(heated&&water>0?1:-1),0,200);timer++;return heat!=previous;}
    public boolean spillDue(){if(timer>=100){timer=0;return true;}return total()>500;}
    public boolean dissolve(Map<Aspect,Integer> values){
        if(!boiling()||values.isEmpty()||values.values().stream().anyMatch(n->n<=0||n>10000)||total()+values.values().stream().mapToInt(Integer::intValue).sum()>100000)return false;
        values.forEach((a,n)->aspects.merge(a,n,Integer::sum));timer=-150;return true;
    }
    public boolean contains(Map<String,Integer> cost){return !cost.isEmpty()&&cost.entrySet().stream().allMatch(e->e.getValue()>0&&Aspect.byId(e.getKey()).map(a->aspects.getOrDefault(a,0)>=e.getValue()).orElse(false));}
    public boolean craft(Map<String,Integer> cost){
        if(!boiling()||!contains(cost))return false;
        cost.forEach((id,n)->aspects.computeIfPresent(Aspect.byId(id).orElseThrow(),(a,amount)->amount==n?null:amount-n));
        water=Math.max(0,water-50);timer=-250;return true;
    }
    public float spill(int index){
        if(aspects.isEmpty())return 0;var a=new ArrayList<>(aspects.keySet()).get(Math.floorMod(index,aspects.size()));
        aspects.computeIfPresent(a,(key,n)->n<=1?null:n-1);return a==Aspect.FLUX?1:.25f;
    }
    public float empty(){float pollution=total()*.25f+aspects.getOrDefault(Aspect.FLUX,0)*.75f;aspects.clear();water=0;return pollution;}
    public void restore(int water,int heat,int timer,Map<Aspect,Integer> values){
        this.water=Math.clamp(water,0,1000);this.heat=Math.clamp(heat,0,200);this.timer=Math.clamp(timer,-250,100);aspects.clear();
        values.forEach((a,n)->{if(a!=null&&n>0)aspects.put(a,Math.min(10000,n));});
    }
}
