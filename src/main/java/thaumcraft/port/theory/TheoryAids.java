package thaumcraft.port.theory;

import java.util.*;

/** Registration order and weighted pools from ConfigResearch.initTheorycraft(). */
public final class TheoryAids {
    public record Aid(String object,List<String> cards){}
    public static final Map<String,Aid> DEFINITIONS;
    static {
        var m=new LinkedHashMap<String,Aid>();
        add(m,"Bookshelf","minecraft:bookshelf","Balance","Notation","Notation","Study","Study","Study");
        add(m,"BrainInAJar","thaumcraft:jar_brain","DarkWhispers");
        add(m,"GlyphedStone","thaumcraft:stone_ancient_glyphed","Glyphs");
        add(m,"PortalEnd","minecraft:end_portal","Portal");add(m,"PortalNether","minecraft:nether_portal","Portal");add(m,"PortalCrimson","entity:thaumcraft:cultist_portal","Portal");
        add(m,"BasicAlchemy","thaumcraft:crucible","Concentrate","Reactions","Synthesis");
        add(m,"BasicArtifice","thaumcraft:arcane_workbench","Calibrate","Tinker","MindOverMatter");
        add(m,"BasicInfusion","thaumcraft:infusion_matrix","Measure","Channel","Infuse");
        add(m,"BasicAuromancy","thaumcraft:wand_workbench","Focus","Awareness","Spellbinding");
        add(m,"BasicGolemancy","thaumcraft:golem_builder","Sculpting","Scripting","Synergy");
        // Truth is referenced by this aid, but was never registered as a card in BETA26.
        add(m,"BasicEldritch","thaumcraft:eldritch","Realization","Revelation","Truth");
        add(m,"EnchantmentTable","minecraft:enchanting_table","Enchantment");add(m,"Beacon","minecraft:beacon","Beacon");
        DEFINITIONS=Collections.unmodifiableMap(m);
    }
    private static void add(Map<String,Aid> m,String id,String object,String... cards){m.put(id,new Aid(object,List.of(cards)));}
    private TheoryAids(){}
}
