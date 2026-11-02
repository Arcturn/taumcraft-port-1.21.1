package thaumcraft.port.auromancy.focus.mod;

import thaumcraft.port.auromancy.focus.FocusModSplit;
import thaumcraft.port.auromancy.focus.FocusNode;
import thaumcraft.port.auromancy.focus.Trajectory;

public class FocusModSplitTrajectory
extends FocusModSplit {
    @Override
    public String getResearch() {
        return "FOCUSSPLIT";
    }

    @Override
    public String getKey() {
        return "thaumcraft.SPLITTRAJECTORY";
    }

    @Override
    public int getComplexity() {
        return 5;
    }

    @Override
    public FocusNode.EnumSupplyType[] mustBeSupplied() {
        return new FocusNode.EnumSupplyType[]{FocusNode.EnumSupplyType.TRAJECTORY};
    }

    @Override
    public FocusNode.EnumSupplyType[] willSupply() {
        return new FocusNode.EnumSupplyType[]{FocusNode.EnumSupplyType.TRAJECTORY};
    }

    @Override
    public Trajectory[] supplyTrajectories() {
        return this.getParent().supplyTrajectories();
    }

    @Override
    public boolean execute() {
        return true;
    }
}
