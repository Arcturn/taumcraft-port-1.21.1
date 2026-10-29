package thaumcraft.port.essentia;

import java.util.function.DoubleSupplier;

public final class SmelterUpgrades {
    public static int cookTime(int aspects,int bellows){return Math.max(1,(int)(aspects*2L*(1-.125*Math.clamp(bellows,0,3))));}
    public static int pollution(int lost,int vents,DoubleSupplier random){int remaining=0;for(int i=0;i<lost;i++){boolean removed=false;for(int v=0;v<Math.clamp(vents,0,3);v++)if(random.getAsDouble()<.333){removed=true;break;}if(!removed)remaining++;}return remaining;}
    private SmelterUpgrades(){}
}
