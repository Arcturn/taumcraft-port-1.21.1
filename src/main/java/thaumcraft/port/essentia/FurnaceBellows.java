package thaumcraft.port.essentia;

import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

/** Only ordinary furnaces: NeoForge/vanilla owns fuel, recipe completion, output and XP. */
public final class FurnaceBellows {
    public static boolean advance(FurnaceBlockEntity furnace){
        if(furnace.litTime<=0||furnace.cookingProgress<=0||furnace.cookingProgress>=furnace.cookingTotalTime-1)return false;
        furnace.cookingProgress++;
        furnace.setChanged();
        return true;
    }
    private FurnaceBellows(){}
}
