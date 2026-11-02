package thaumcraft.port.auromancy.focus;

/** A serializable element of a BETA26 focus program. */
public interface IFocusElement {
    String getKey();
    String getResearch();
    EnumUnitType getType();
    enum EnumUnitType { EFFECT, MEDIUM, MOD, PACKAGE }
}
