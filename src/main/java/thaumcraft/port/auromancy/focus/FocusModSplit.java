package thaumcraft.port.auromancy.focus;

import java.util.ArrayList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public abstract class FocusModSplit extends FocusMod {
    private final ArrayList<FocusPackage> packages = new ArrayList<>();
    public final ArrayList<FocusPackage> getSplitPackages() { return packages; }
    public void deserialize(CompoundTag nbt) {
        packages.clear(); var list = nbt.getList("packages", Tag.TAG_COMPOUND);
        if (list.size() > FocusPackage.MAX_NODES) return;
        for (var value : list) { var child = new FocusPackage(); child.deserialize((CompoundTag) value); packages.add(child); }
    }
    public CompoundTag serialize() {
        var result = new CompoundTag(); var list = new ListTag();
        for (var pack : packages) list.add(pack.serialize()); result.put("packages", list); return result;
    }
    @Override public float getPowerMultiplier() { return .75f; }
}
