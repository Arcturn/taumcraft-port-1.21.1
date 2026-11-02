package thaumcraft.port.auromancy.focus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import thaumcraft.port.aspect.Aspect;

/** A focus program and its per-cast context. Saved programs do not retain live entity references. */
public final class FocusPackage implements IFocusElement {
    public static final int MAX_NODES = 128;
    public static final int MAX_DEPTH = 16;
    public Level world;
    public List<IFocusElement> nodes = new ArrayList<>();
    private LivingEntity caster;
    private UUID casterUUID, uid;
    private float power = 1;
    private int complexity, index;
    private InteractionHand castingHand = InteractionHand.MAIN_HAND;
    private String decodeError = "";

    public FocusPackage() { }
    public FocusPackage(LivingEntity caster) { this.caster = caster; world = caster.level(); casterUUID = caster.getUUID(); }
    @Override public String getKey() { return "thaumcraft.PACKAGE"; }
    @Override public String getResearch() { return null; }
    @Override public EnumUnitType getType() { return EnumUnitType.PACKAGE; }
    public int getComplexity() { return complexity; }
    public void setComplexity(int complexity) { this.complexity = complexity; }
    public UUID getUniqueID() { return uid; }
    public void setUniqueID(UUID uid) { this.uid = uid; }
    public int getExecutionIndex() { return index; }
    public void setExecutionIndex(int index) { this.index = index; }
    public void addNode(IFocusElement node) { nodes.add(node); }
    public UUID getCasterUUID() { return caster == null ? casterUUID : caster.getUUID(); }
    public void setCasterUUID(UUID uuid) { if (!java.util.Objects.equals(casterUUID, uuid)) caster = null; casterUUID = uuid; }
    public InteractionHand getCastingHand() { return castingHand; }
    public void setCastingHand(InteractionHand hand) { castingHand = hand == null ? InteractionHand.MAIN_HAND : hand; }
    public HumanoidArm getCastingArm() {
        var entity = getCaster(); var arm = entity == null ? HumanoidArm.RIGHT : entity.getMainArm();
        return castingHand == InteractionHand.MAIN_HAND ? arm : arm.getOpposite();
    }
    public LivingEntity getCaster() {
        if (caster != null && caster.level() == world && !caster.isRemoved()) return caster;
        if (world == null || casterUUID == null) return null;
        if (world instanceof ServerLevel server) {
            var entity = server.getEntity(casterUUID); caster = entity instanceof LivingEntity living ? living : null;
        } else caster = world.getPlayerByUUID(casterUUID);
        return caster;
    }
    public float getPower() { return power; }
    public void multiplyPower(float multiplier) { power *= multiplier; }
    public float getVisCost() { return complexity / 5f; }
    public int getActivationTime() { return Math.max(5, complexity / 5 * (complexity / 4)); }
    public String getDecodeError() { return decodeError; }

    public FocusEffect[] getFocusEffects() {
        var effects = new ArrayList<FocusEffect>();
        collectEffects(this, effects, Collections.newSetFromMap(new IdentityHashMap<>()), 0);
        return effects.toArray(FocusEffect[]::new);
    }
    private static void collectEffects(FocusPackage pack, List<FocusEffect> effects, Set<FocusPackage> visited, int depth) {
        if (depth > MAX_DEPTH || !visited.add(pack)) return;
        for (var element : pack.nodes) {
            if (element instanceof FocusEffect effect) effects.add(effect);
            else if (element instanceof FocusPackage child) collectEffects(child, effects, visited, depth + 1);
            else if (element instanceof FocusModSplit split)
                for (var child : split.getSplitPackages()) collectEffects(child, effects, visited, depth + 1);
        }
    }
    public void deserialize(CompoundTag nbt) {
        try { read(nbt, 0, new Budget()); decodeError = ""; }
        catch (IllegalArgumentException exception) {
            nodes.clear(); complexity = 0; power = 1; index = 0; uid = null; caster = null; casterUUID = null;
            castingHand = InteractionHand.MAIN_HAND; world = null;
            decodeError = exception.getMessage();
        }
    }
    private void read(CompoundTag nbt, int depth, Budget budget) {
        if (depth > MAX_DEPTH) throw new IllegalArgumentException("Focus package nesting exceeds " + MAX_DEPTH);
        nodes.clear(); caster = null;
        uid = nbt.hasUUID("uid") ? nbt.getUUID("uid") : null;
        casterUUID = nbt.hasUUID("casterUUID") ? nbt.getUUID("casterUUID") : null;
        index = nbt.getInt("index"); complexity = nbt.getInt("complexity");
        power = nbt.contains("power", Tag.TAG_ANY_NUMERIC) ? nbt.getFloat("power") : 1;
        if (!Float.isFinite(power) || power < 0 || power > 1_000_000 || complexity < 0 || complexity > 1_000_000)
            throw new IllegalArgumentException("Invalid focus package power/complexity");
        int hand = nbt.getInt("castingHand");
        if (hand < 0 || hand >= InteractionHand.values().length) throw new IllegalArgumentException("Invalid casting hand");
        castingHand = InteractionHand.values()[hand];
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ResourceLocation dimension = nbt.contains("dimension", Tag.TAG_STRING) ? ResourceLocation.tryParse(nbt.getString("dimension"))
                    : (nbt.getInt("dim") == -1 ? Level.NETHER.location() : nbt.getInt("dim") == 1 ? Level.END.location() : Level.OVERWORLD.location());
            if (dimension != null) world = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
        }
        if (!nbt.contains("nodes", Tag.TAG_LIST)) throw new IllegalArgumentException("Missing focus nodes");
        var list = nbt.getList("nodes", Tag.TAG_COMPOUND);
        for (var value : list) {
            if (++budget.nodes > MAX_NODES) throw new IllegalArgumentException("Focus node count exceeds " + MAX_NODES);
            var nodeTag = (CompoundTag) value;
            if (nodeTag.getString("type").equals(EnumUnitType.PACKAGE.name())) {
                var child = new FocusPackage(); child.read(nodeTag.getCompound("package"), depth + 1, budget); nodes.add(child);
                continue;
            }
            var element = FocusEngine.getElement(nodeTag.getString("key"));
            if (!(element instanceof FocusNode node) || !element.getType().name().equals(nodeTag.getString("type")))
                throw new IllegalArgumentException("Unknown focus node/type " + nodeTag.getString("key"));
            for (var setting : node.getSettingList())
                if (!nodeTag.contains("setting." + setting, Tag.TAG_INT) || !node.getSetting(setting).setValueChecked(nodeTag.getInt("setting." + setting)))
                    throw new IllegalArgumentException("Invalid focus setting " + node.getKey() + "/" + setting);
            if (node instanceof FocusModSplit split) {
                var branches = nodeTag.getCompound("packages").getList("packages", Tag.TAG_COMPOUND);
                if (branches.size() > MAX_NODES) throw new IllegalArgumentException("Too many split branches");
                for (var branch : branches) {
                    var child = new FocusPackage(); child.read((CompoundTag) branch, depth + 1, budget);
                    split.getSplitPackages().add(child);
                }
            }
            nodes.add(node);
        }
        if (index < 0 || index > nodes.size()) throw new IllegalArgumentException("Invalid focus execution index");
    }
    public CompoundTag serialize() { return write(0, new Budget(), Collections.newSetFromMap(new IdentityHashMap<>())); }
    private CompoundTag write(int depth, Budget budget, Set<FocusPackage> visited) {
        if (depth > MAX_DEPTH || !visited.add(this)) throw new IllegalArgumentException("Cyclic/deep focus package");
        var tag = new CompoundTag();
        if (uid != null) tag.putUUID("uid", uid);
        if (getCasterUUID() != null) tag.putUUID("casterUUID", getCasterUUID());
        if (world != null) tag.putString("dimension", world.dimension().location().toString());
        tag.putInt("index", index); tag.putFloat("power", power); tag.putInt("complexity", complexity);
        tag.putInt("castingHand", castingHand.ordinal());
        var list = new ListTag();
        for (var element : nodes) {
            if (element == null || ++budget.nodes > MAX_NODES) throw new IllegalArgumentException("Invalid focus node count");
            var entry = new CompoundTag(); entry.putString("type", element.getType().name()); entry.putString("key", element.getKey());
            if (element instanceof FocusPackage child) entry.put("package", child.write(depth + 1, budget, visited));
            else if (element instanceof FocusNode node) {
                for (var key : node.getSettingList()) entry.putInt("setting." + key, node.getSettingValue(key));
                if (node instanceof FocusModSplit split) {
                    var branches = new ListTag();
                    for (var child : split.getSplitPackages()) branches.add(child.write(depth + 1, budget, visited));
                    var branchTag = new CompoundTag(); branchTag.put("packages", branches); entry.put("packages", branchTag);
                }
            }
            list.add(entry);
        }
        tag.put("nodes", list); visited.remove(this); return tag;
    }
    public FocusPackage copy(LivingEntity caster) {
        var copy = new FocusPackage(caster); copy.deserialize(serialize());
        if (caster != null) { copy.world = caster.level(); copy.caster = caster; copy.casterUUID = caster.getUUID(); }
        return copy;
    }
    public void initialize(LivingEntity caster) {
        if (caster == null || nodes.isEmpty()) return;
        this.caster = caster; casterUUID = caster.getUUID(); world = caster.level();
        if (nodes.getFirst() instanceof FocusMediumRoot root && root.supplyTargets() == null) root.setupFromCaster(caster);
    }
    public int getSortingHelper() {
        var value = new StringBuilder();
        for (var node : nodes) {
            value.append(node.getKey());
            if (node instanceof FocusNode focus) for (var key : focus.getSettingList()) value.append(focus.getSettingValue(key));
        }
        return value.toString().hashCode();
    }

    /** Source FocalManipulator evaluates duplicate key costs in editor-node order. */
    public static int calculateComplexity(Collection<? extends IFocusElement> nodes) {
        int total = 0; var occurrences = new HashMap<String, Integer>();
        for (var element : nodes) if (element instanceof FocusNode node) {
            int occurrence = occurrences.merge(node.getKey(), 1, Integer::sum);
            total = (int) (total + node.getComplexity() * (.5f * (occurrence + 1)));
        }
        return total;
    }
    public static Map<Aspect, Integer> crystals(Collection<? extends IFocusElement> nodes) {
        var result = new LinkedHashMap<Aspect, Integer>();
        for (var element : nodes) if (element instanceof FocusNode node && node.getAspect() != null) result.merge(node.getAspect(), 1, Integer::sum);
        return Collections.unmodifiableMap(result);
    }
    public static int constructionVis(int complexity, int maximumComplexity) { return complexity * 10 + maximumComplexity / 5; }
    public static int experienceCost(int complexity) { return (int) Math.max(1L, Math.round(Math.sqrt(complexity))); }

    public record Validation(boolean valid, String reason, int complexity, int nodeCount) { }
    public boolean isValid() { return validate(Integer.MAX_VALUE, null).valid(); }
    public Validation validate(int maximumComplexity, Predicate<String> knowsResearch) {
        if (!decodeError.isEmpty()) return new Validation(false, decodeError, complexity, 0);
        if (complexity < 0 || complexity > maximumComplexity) return new Validation(false, "Focus exceeds its capacity", complexity, 0);
        var budget = new Budget();
        try {
            validateProgram(this, null, knowsResearch, 0, budget, Collections.newSetFromMap(new IdentityHashMap<>()));
            if (budget.effects == 0) throw new IllegalArgumentException("Focus has no effect");
            return new Validation(true, "", complexity, budget.nodes);
        } catch (IllegalArgumentException exception) { return new Validation(false, exception.getMessage(), complexity, budget.nodes); }
    }
    private static void validateProgram(FocusPackage pack, FocusNode previous, Predicate<String> knows, int depth, Budget budget, Set<FocusPackage> visited) {
        if (depth > MAX_DEPTH || !visited.add(pack) || pack.nodes.isEmpty()) throw new IllegalArgumentException("Empty/cyclic/deep focus program");
        for (int i = 0; i < pack.nodes.size(); i++) {
            var element = pack.nodes.get(i);
            if (++budget.nodes > MAX_NODES) throw new IllegalArgumentException("Too many focus nodes");
            if (element instanceof FocusPackage child) { validateProgram(child, previous, knows, depth + 1, budget, visited); continue; }
            if (!(element instanceof FocusNode node)) throw new IllegalArgumentException("Unknown focus element");
            if (node instanceof FocusMediumRoot) {
                if (i != 0) throw new IllegalArgumentException("Misplaced focus root");
            } else {
                if (previous == null) throw new IllegalArgumentException("Focus must start with a root");
                var needed = node.mustBeSupplied();
                if (needed != null && needed.length > 0) {
                    boolean supplied = false;
                    for (var supply : needed) supplied |= previous.canSupply(supply);
                    if (!supplied) throw new IllegalArgumentException("Missing supply for " + node.getKey());
                }
            }
            if (knows != null && node.getResearch() != null && !knows.test(node.getResearch())) throw new IllegalArgumentException("Unknown research " + node.getResearch());
            for (var key : node.getSettingList()) {
                var setting = node.getSetting(key);
                if (!setting.isValidValue(setting.getValue())) throw new IllegalArgumentException("Invalid focus setting " + key);
                if (knows != null && setting.getResearch() != null && !knows.test(setting.getResearch())) {
                    var defaults = FocusEngine.getElement(node.getKey());
                    if (!(defaults instanceof FocusNode base) || base.getSettingValue(key) != setting.getValue()) throw new IllegalArgumentException("Unknown setting research " + setting.getResearch());
                }
            }
            if (node instanceof FocusEffect) budget.effects++;
            if (node instanceof FocusModSplit split) {
                if (split.getSplitPackages().size() != 2 || i != pack.nodes.size() - 1) throw new IllegalArgumentException("Invalid focus split");
                for (var branch : split.getSplitPackages()) validateProgram(branch, split, knows, depth + 1, budget, visited);
            }
            previous = node;
        }
        visited.remove(pack);
    }
    private static final class Budget { int nodes, effects; }
}
