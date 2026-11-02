package thaumcraft.port.auromancy.focus;

import thaumcraft.port.aspect.Aspect;

public abstract class FocusMod extends FocusNode {
    @Override public EnumUnitType getType() { return EnumUnitType.MOD; }
    @Override public Aspect getAspect() { return null; }
    public abstract boolean execute();
}
