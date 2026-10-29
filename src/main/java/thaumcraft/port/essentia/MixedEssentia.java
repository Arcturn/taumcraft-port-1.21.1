package thaumcraft.port.essentia;

import java.util.*;
import thaumcraft.port.aspect.Aspect;

/** Bounded multi-aspect store; simulations and malformed saves cannot create essentia. */
public final class MixedEssentia {
    private final int capacity;
    private final EnumMap<Aspect,Integer> values=new EnumMap<>(Aspect.class);
    public MixedEssentia(int capacity){this.capacity=capacity;}
    public Map<Aspect,Integer> values(){return Collections.unmodifiableMap(values);}
    public int total(){return values.values().stream().mapToInt(Integer::intValue).sum();}
    public int amount(Aspect a){return values.getOrDefault(a,0);}
    public int insert(Aspect a,int n,boolean simulate){if(a==null||n<=0)return 0;int accepted=Math.min(n,capacity-total());if(!simulate&&accepted>0)values.merge(a,accepted,Integer::sum);return accepted;}
    public int extract(Aspect a,int n,boolean simulate){if(a==null||n<=0)return 0;int taken=Math.min(n,amount(a));if(!simulate&&taken>0){int left=amount(a)-taken;if(left==0)values.remove(a);else values.put(a,left);}return taken;}
    public void clear(){values.clear();}
    public void restore(Map<Aspect,Integer> saved){clear();for(Aspect a:Aspect.values())insert(a,saved.getOrDefault(a,0),false);}
    public static int bufferSuction(int bellows,int choke){return choke==2?0:choke==1||bellows<=0?1:Math.min(6,bellows)*32;}
    public static boolean higherDemand(Aspect offered,int requester,int own,Aspect otherType,int other){return (otherType==null||otherType==offered)&&other>requester&&other>own;}
}
