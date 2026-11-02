package thaumcraft.port.auromancy.focus;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Original ordered focus executor: media, modifiers, intermediary entities, then target effects. */
public final class FocusEngine {
    public static final Map<String, Class<? extends IFocusElement>> elements = new LinkedHashMap<>();
    private static final Map<String, ResourceLocation> ICONS = new LinkedHashMap<>();
    private static final Map<String, Integer> COLORS = new LinkedHashMap<>();
    private static final ArrayDeque<DamageKey> DAMAGE_RESIST = new ArrayDeque<>();
    private record DamageKey(UUID entity, UUID cast) { }
    static { FocusCatalog.register(); }

    public static void registerElement(Class<? extends IFocusElement> type, ResourceLocation icon, int color) {
        try {
            var node = type.getDeclaredConstructor().newInstance();
            elements.put(node.getKey(), type); ICONS.put(node.getKey(), icon); COLORS.put(node.getKey(), color);
        } catch (ReflectiveOperationException exception) { throw new IllegalArgumentException("Cannot register focus element " + type, exception); }
    }
    public static IFocusElement getElement(String key) {
        var type = elements.get(key); if (type == null) return null;
        try { return type.getDeclaredConstructor().newInstance(); }
        catch (ReflectiveOperationException exception) { throw new IllegalStateException("Cannot construct focus element " + key, exception); }
    }
    public static ResourceLocation getElementIcon(String key) { return ICONS.get(key); }
    public static int getElementColor(String key) { return COLORS.getOrDefault(key, 0xFFFFFF); }
    public static boolean doesPackageContainElement(FocusPackage pack, String key) {
        if (pack == null) return false;
        for (var node : pack.nodes) if (node.getKey().equals(key)) return true;
        return false;
    }
    public static void castFocusPackage(LivingEntity caster, FocusPackage pack) { castFocusPackage(caster, pack, false); }
    public static void castFocusPackage(LivingEntity caster, FocusPackage pack, boolean noCopy) {
        if (caster == null || pack == null || pack.nodes.isEmpty()) return;
        var cast = noCopy ? pack : pack.copy(caster); cast.initialize(caster); cast.setUniqueID(UUID.randomUUID());
        for (var effect : cast.getFocusEffects()) effect.onCast(caster);
        runFocusPackage(cast, null, null);
    }
    public static void runFocusPackage(FocusPackage pack, Trajectory[] trajectories, HitResult[] targets) {
        run(pack, trajectories, targets, 0);
    }
    private static void run(FocusPackage pack, Trajectory[] trajectories, HitResult[] targets, int depth) {
        if (pack == null || pack.nodes.isEmpty() || pack.nodes.size() > FocusPackage.MAX_NODES || depth > FocusPackage.MAX_DEPTH) return;
        var previousTrajectories = trajectories; var previousTargets = targets;
        if (!(pack.nodes.getFirst() instanceof FocusMediumRoot)) pack.nodes.addFirst(new FocusMediumRoot(trajectories, targets));
        for (int index = 0; index < pack.nodes.size(); index++) {
            pack.setExecutionIndex(index); var element = pack.nodes.get(index);
            if (element instanceof FocusPackage child) {
                inheritContext(pack, child); run(child, previousTrajectories, previousTargets, depth + 1); break;
            }
            if (!(element instanceof FocusNode node)) break;
            if (index > 0 && node.getParent() == null && pack.nodes.get(index - 1) instanceof FocusNode parent) node.setParent(parent);
            if (node.getPackage() == null) node.setPackage(pack);
            pack.multiplyPower(node.getPowerMultiplier());
            if (node instanceof FocusMedium medium) {
                if (previousTrajectories != null) for (var trajectory : previousTrajectories) medium.execute(trajectory);
                if (medium.hasIntermediary()) break;
            } else if (node instanceof FocusMod mod) {
                if (mod instanceof FocusModSplit split) {
                    for (var child : split.getSplitPackages()) {
                        inheritContext(pack, child); split.setPackage(child); child.multiplyPower(pack.getPower()); split.execute();
                        run(child, split.supplyTrajectories(), split.supplyTargets(), depth + 1);
                    }
                    break;
                }
                mod.execute();
            } else if (node instanceof FocusEffect effect && previousTargets != null) {
                for (int targetIndex = 0; targetIndex < previousTargets.length; targetIndex++) {
                    var target = previousTargets[targetIndex];
                    if (target instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity victim) {
                        var key = new DamageKey(victim.getUUID(), pack.getUniqueID());
                        if (DAMAGE_RESIST.contains(key) && victim.invulnerableTime > 0) victim.invulnerableTime = 0;
                        else { if (DAMAGE_RESIST.size() > 10) DAMAGE_RESIST.removeFirst(); DAMAGE_RESIST.addLast(key); }
                    }
                    var trajectory = previousTrajectories == null || previousTrajectories.length == 0 ? null
                            : previousTrajectories[previousTrajectories.length == previousTargets.length ? targetIndex : 0];
                    effect.execute(target, trajectory, pack.getPower(), targetIndex);
                }
            }
            previousTrajectories = node.supplyTrajectories(); previousTargets = node.supplyTargets();
        }
    }
    private static void inheritContext(FocusPackage parent, FocusPackage child) {
        child.world = parent.world; child.setCasterUUID(parent.getCasterUUID()); child.setUniqueID(parent.getUniqueID());
        child.setCastingHand(parent.getCastingHand());
    }
    private FocusEngine() { }
}
