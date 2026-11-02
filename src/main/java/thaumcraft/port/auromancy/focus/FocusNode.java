package thaumcraft.port.auromancy.focus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.phys.HitResult;
import thaumcraft.port.aspect.Aspect;

public abstract class FocusNode implements IFocusElement {
    private FocusPackage pack;
    private FocusNode parent;
    private final Map<String, NodeSetting> settings = new HashMap<>();

    protected FocusNode() { initialize(); }
    public String getUnlocalizedName() { return getKey() + ".name"; }
    public String getUnlocalizedText() { return getKey() + ".text"; }
    public abstract int getComplexity();
    public abstract Aspect getAspect();
    public abstract EnumSupplyType[] mustBeSupplied();
    public abstract EnumSupplyType[] willSupply();
    public boolean canSupply(EnumSupplyType type) {
        var supplied = willSupply();
        if (supplied != null) for (var supply : supplied) if (supply == type) return true;
        return false;
    }
    public HitResult[] supplyTargets() { return null; }
    public Trajectory[] supplyTrajectories() { return null; }
    public final void setPackage(FocusPackage pack) { this.pack = pack; }
    public final FocusPackage getPackage() { return pack; }
    public final FocusPackage getRemainingPackage() {
        if (pack == null || pack.getExecutionIndex() + 1 >= pack.nodes.size()) return null;
        var result = new FocusPackage();
        result.world = pack.world;
        result.nodes = new ArrayList<>(pack.nodes.subList(pack.getExecutionIndex() + 1, pack.nodes.size()));
        result.setUniqueID(pack.getUniqueID()); result.setCasterUUID(pack.getCasterUUID());
        result.multiplyPower(pack.getPower()); result.setCastingHand(pack.getCastingHand());
        return result;
    }
    public final FocusNode getParent() { return parent; }
    public void setParent(FocusNode parent) { this.parent = parent; }
    public final Set<String> getSettingList() { return settings.keySet(); }
    public final NodeSetting getSetting(String key) { return settings.get(key); }
    public final int getSettingValue(String key) { var setting = settings.get(key); return setting == null ? 0 : setting.getValue(); }
    public NodeSetting[] createSettings() { return null; }
    public final void initialize() {
        settings.clear(); var created = createSettings();
        if (created != null) for (var setting : created) settings.put(setting.key, setting);
    }
    public float getPowerMultiplier() { return 1; }
    public boolean isExclusive() { return false; }
    public enum EnumSupplyType { TARGET, TRAJECTORY }
}
