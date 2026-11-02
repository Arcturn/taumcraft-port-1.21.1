package thaumcraft.port.auromancy.entity;

import java.util.ArrayList;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import thaumcraft.port.auromancy.focus.FocusEngine;
import thaumcraft.port.auromancy.focus.FocusPackage;
import thaumcraft.port.auromancy.focus.Trajectory;

/** Source server-event deferred execution; each mine target retains its original one-tick stagger. */
public final class SpellExecution {
    private record Pending(long due, FocusPackage pack, Trajectory[] paths, HitResult[] targets) { }
    private static final Map<ServerLevel,ArrayList<Pending>> PENDING = new WeakHashMap<>();
    public static void schedule(FocusPackage pack, Trajectory[] paths, HitResult[] targets, int delay) {
        if (pack != null && pack.world instanceof ServerLevel level)
            PENDING.computeIfAbsent(level,ignored -> new ArrayList<>()).add(new Pending(level.getGameTime()+Math.max(0,delay),pack,paths,targets));
    }
    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var list = PENDING.get(level); if (list == null) return;
        var due = new ArrayList<Pending>();
        list.removeIf(p -> {if (p.due <= level.getGameTime()) {due.add(p); return true;} return false;});
        if (list.isEmpty()) PENDING.remove(level);
        for (var pending : due) if (pending.pack.getCaster()!=null) FocusEngine.runFocusPackage(pending.pack,pending.paths,pending.targets);
    }
    private SpellExecution() { }
}
