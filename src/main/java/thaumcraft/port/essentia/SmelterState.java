package thaumcraft.port.essentia;

import java.util.*;
import java.util.function.DoubleSupplier;
import thaumcraft.port.aspect.Aspect;

public final class SmelterState {
    public static final int CAPACITY=256;
    private final EnumMap<Aspect,Integer> aspects=new EnumMap<>(Aspect.class);
    public int total(){return aspects.values().stream().mapToInt(Integer::intValue).sum();}
    public Map<Aspect,Integer> aspects(){return Collections.unmodifiableMap(aspects);}
    public boolean fits(Map<Aspect,Integer> input){return !input.isEmpty()&&input.entrySet().stream().allMatch(e->e.getKey()!=null&&e.getValue()!=null&&e.getValue()>0)&&input.values().stream().mapToLong(Integer::longValue).sum()<=CAPACITY-total();}
    public int smelt(Map<Aspect,Integer> input,int tier,DoubleSupplier random){
        if(!fits(input))return -1;
        double efficiency=tier==1?.9:tier==2?.95:.8;int pollution=0;
        for(var e:input.entrySet())for(int i=0;i<e.getValue();i++){
            if(random.getAsDouble()>efficiency*(e.getKey()==Aspect.FLUX?.66:1))pollution++;
            else aspects.merge(e.getKey(),1,Integer::sum);
        }
        return pollution;
    }
    public boolean take(Aspect a){int n=aspects.getOrDefault(a,0);if(n==0)return false;if(n==1)aspects.remove(a);else aspects.put(a,n-1);return true;}
    public void restore(Map<Aspect,Integer> saved){aspects.clear();for(Aspect a:Aspect.values()){int n=Math.clamp(saved.getOrDefault(a,0),0,CAPACITY-total());if(n>0)aspects.put(a,n);}}
}
