package thaumcraft.port.auromancy.device;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import thaumcraft.port.world.AuraSystem;
import thaumcraft.port.world.WorldConfig;

/** The two original station drain algorithms deliberately differ. Neither loads remote chunks. */
public final class WorkbenchAura {
    public static boolean charged(ServerLevel level,BlockPos pos){return level.getBlockState(pos.above()).getBlock() instanceof WorkbenchChargerBlock;}
    public static int available(ServerLevel level,BlockPos pos) {
        if(!WorldConfig.AURA.get())return 0;
        if(!charged(level,pos)){var aura=AuraSystem.get(level,pos);return aura==null?0:(int)aura.vis();}
        int total=0;
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            var aura=AuraSystem.get(level,pos.offset(x*16,0,z*16));
            if(aura!=null)total=(int)(total+aura.vis());
        }
        return total;
    }
    /** TileArcaneWorkbench distributes integral installments until the requested payment is covered. */
    public static float spend(ServerLevel level,BlockPos pos,int cost) {
        if(cost<=0||!WorldConfig.AURA.get())return 0;
        if(!charged(level,pos))return AuraSystem.drain(level,pos,cost,false,false);
        int remaining=cost,installment=Math.max(1,cost/9);float drained=0;
        for(int attempts=0;remaining>0&&attempts<=1000;attempts++){
            int before=remaining;
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
                installment=Math.min(installment,remaining);
                float payment=AuraSystem.drain(level,pos.offset(x*16,0,z*16),installment,false,false);
                drained+=payment;remaining=(int)(remaining-payment);
                if(remaining<=0)return drained;
            }
            if(remaining==before)break;
        }
        return drained;
    }
    /** TileFocalManipulator takes one ninth from each chunk, once per crafting interval. */
    public static float drainFocal(ServerLevel level,BlockPos pos,float amount) {
        if(!(amount>0)||!Float.isFinite(amount))return 0;
        if(!charged(level,pos))return AuraSystem.drain(level,pos,amount,false,false);
        float remaining=amount,installment=amount/9;
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            installment=Math.min(installment,remaining);
            remaining-=AuraSystem.drain(level,pos.offset(x*16,0,z*16),installment,false,false);
            if(remaining<=0)return amount;
        }
        return amount-remaining;
    }
    private WorkbenchAura(){}
}
