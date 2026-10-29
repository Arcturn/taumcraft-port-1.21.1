package thaumcraft.port.essentia;

import thaumcraft.port.aspect.Aspect;

/** A single-aspect store. Mutations return accepted amounts, never remainders. */
public final class EssentiaTank {
    public final int capacity;
    private Aspect aspect, filter;
    private int amount;
    public EssentiaTank(int capacity) { if(capacity<1)throw new IllegalArgumentException();this.capacity=capacity; }
    public Aspect aspect(){return aspect;}
    public Aspect filter(){return filter;}
    public int amount(){return amount;}
    public boolean filter(Aspect value){if(amount>0&&value!=null&&value!=aspect)return false;filter=value;return true;}
    public int insert(Aspect value,int requested,boolean voidExcess,boolean simulate){
        if(value==null||requested<=0||(filter!=null&&filter!=value)||(amount>0&&aspect!=value))return 0;
        int accepted=voidExcess?requested:Math.min(requested,capacity-amount);
        if(!simulate&&accepted>0){aspect=value;amount=(int)Math.min(capacity,(long)amount+accepted);}
        return accepted;
    }
    public int extract(Aspect value,int requested,boolean simulate){
        if(value==null||value!=aspect||requested<=0)return 0;
        int taken=Math.min(requested,amount);
        if(!simulate){amount-=taken;if(amount==0)aspect=null;}
        return taken;
    }
    public void restore(Aspect value,int count,Aspect label){
        amount=value==null?0:Math.clamp(count,0,capacity);aspect=amount==0?null:value;
        filter=amount>0&&label!=null&&label!=aspect?null:label;
    }
    public int suction(boolean voidJar){return voidJar?(filter!=null&&amount<capacity?48:32):(amount==capacity?0:(filter==null?32:64));}
    public int minimum(boolean voidJar){return filter==null?32:(voidJar?48:64);}
}
