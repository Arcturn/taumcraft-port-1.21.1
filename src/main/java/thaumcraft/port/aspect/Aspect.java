package thaumcraft.port.aspect;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The 37 original TC6 aspects, colors and component pairs. Original definitions by Azanor. */
public enum Aspect {
    AIR("aer", 0xFFFF7E),
    EARTH("terra", 5685248),
    FIRE("ignis", 16734721),
    WATER("aqua", 3986684),
    ORDER("ordo", 14013676),
    ENTROPY("perditio", 0x404040),
    VOID("vacuos", 0x888888, AIR, ENTROPY),
    LIGHT("lux", 0xFFFFC0, AIR, FIRE),
    MOTION("motus", 13487348, AIR, ORDER),
    COLD("gelum", 0xE1FFFF, FIRE, ENTROPY),
    CRYSTAL("vitreus", 0x80FFFF, EARTH, AIR),
    METAL("metallum", 11908557, EARTH, ORDER),
    LIFE("victus", 14548997, EARTH, WATER),
    DEATH("mortuus", 6946821, WATER, ENTROPY),
    ENERGY("potentia", 0xC0FFFF, ORDER, FIRE),
    EXCHANGE("permutatio", 5735255, ENTROPY, ORDER),
    MAGIC("praecantatio", 0xCF00FF, ENERGY, AIR),
    AURA("auram", 0xFFC0FF, MAGIC, AIR),
    ALCHEMY("alkimia", 2337949, MAGIC, WATER),
    FLUX("vitium", 0x800080, ENTROPY, MAGIC),
    DARKNESS("tenebrae", 0x222222, VOID, LIGHT),
    ELDRITCH("alienis", 0x805080, VOID, DARKNESS),
    FLIGHT("volatus", 0xE7E7D7, AIR, MOTION),
    PLANT("herba", 109568, LIFE, EARTH),
    TOOL("instrumentum", 0x4040EE, METAL, ENERGY),
    CRAFT("fabrico", 8428928, EXCHANGE, TOOL),
    MECHANISM("machina", 0x8080A0, MOTION, TOOL),
    TRAP("vinculum", 10125440, MOTION, ENTROPY),
    SOUL("spiritus", 0xEBEBFB, LIFE, DEATH),
    MIND("cognitio", 16356991, FIRE, SOUL),
    SENSES("sensus", 0xC0FFC0, AIR, SOUL),
    AVERSION("aversio", 0xC05050, SOUL, ENTROPY),
    PROTECT("praemunio", 49344, SOUL, EARTH),
    DESIRE("desiderium", 15121988, SOUL, VOID),
    UNDEAD("exanimis", 3817472, MOTION, DEATH),
    BEAST("bestia", 10445833, MOTION, LIFE),
    MAN("humanus", 16766912, SOUL, LIFE);

    private final String id;
    private final int color;
    private final List<Aspect> components;
    Aspect(String id, int color, Aspect... components) {
        this.id = id;
        this.color = color;
        this.components = List.of(components);
    }
    public String id() { return id; }
    /** TC6 getName(): the capitalized Latin tag, independent of the translated description. */
    public String displayName() { return Character.toUpperCase(id.charAt(0)) + id.substring(1); }
    public static String describe(Map<Aspect, Integer> amounts) {
        return amounts.entrySet().stream().map(e -> e.getKey().displayName() + " ×" + e.getValue())
                .collect(java.util.stream.Collectors.joining(", "));
    }
    public int color() { return color; }
    public List<Aspect> components() { return components; }
    public boolean isPrimal() { return components.isEmpty(); }
    public static Optional<Aspect> byId(String id) {
        return Arrays.stream(values()).filter(aspect -> aspect.id.equals(id)).findFirst();
    }
    public static Optional<Aspect> combine(Aspect first, Aspect second) {
        return Arrays.stream(values()).filter(a -> a.components.size() == 2 &&
                ((a.components.get(0) == first && a.components.get(1) == second) ||
                 (a.components.get(0) == second && a.components.get(1) == first))).findFirst();
    }
}
