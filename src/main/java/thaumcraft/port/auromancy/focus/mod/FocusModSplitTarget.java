package thaumcraft.port.auromancy.focus.mod;

import net.minecraft.world.phys.HitResult;
import thaumcraft.port.auromancy.focus.FocusModSplit;
import thaumcraft.port.auromancy.focus.FocusNode;

public class FocusModSplitTarget
extends FocusModSplit {
    @Override
    public String getResearch() {
        return "FOCUSSPLIT";
    }

    @Override
    public String getKey() {
        return "thaumcraft.SPLITTARGET";
    }

    @Override
    public int getComplexity() {
        return 4;
    }

    @Override
    public FocusNode.EnumSupplyType[] mustBeSupplied() {
        return new FocusNode.EnumSupplyType[]{FocusNode.EnumSupplyType.TARGET};
    }

    @Override
    public FocusNode.EnumSupplyType[] willSupply() {
        return new FocusNode.EnumSupplyType[]{FocusNode.EnumSupplyType.TARGET};
    }

    @Override
    public HitResult[] supplyTargets() {
        return this.getParent().supplyTargets();
    }

    @Override
    public boolean execute() {
        return true;
    }
}
