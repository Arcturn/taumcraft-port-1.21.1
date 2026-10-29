package thaumcraft.port.essentia;

import java.util.List;
import thaumcraft.port.aspect.Aspect;

/** TC6's local pressure propagation, independent of world/chunk access. */
public final class SuctionRules {
    public record Demand(Aspect aspect,int strength,boolean filtered){}
    public static Demand calculate(Aspect stored,Aspect filter,boolean restrict,List<Demand> neighbours){
        int suction=0;Aspect selected=null;
        for(Demand other:neighbours){
            Aspect type=other.aspect();
            if(type!=null&&((filter!=null&&filter!=type)||(stored!=null&&stored!=type)))continue;
            if(other.strength()>0&&other.strength()>suction+1){suction=restrict?other.strength()/2:other.strength()-1;selected=type==null?filter:type;}
        }
        return new Demand(selected,suction,false);
    }
    public static boolean conflicts(Demand here,Demand other){return here.strength()>0&&!other.filtered()&&(other.strength()==here.strength()||other.strength()==here.strength()-1)&&here.aspect()!=other.aspect();}
    private SuctionRules(){}
}
