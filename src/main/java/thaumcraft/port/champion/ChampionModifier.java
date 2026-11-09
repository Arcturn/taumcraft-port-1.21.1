package thaumcraft.port.champion;

import net.minecraft.network.chat.Component;

/** Stable BETA26 modifier IDs; these are serialized in the champion attribute. */
public enum ChampionModifier {
    BOLD("bold",-1,"40289aa1-907f-4ac6-ad79-e6681efe2cbc"),
    SPINED("spine",2,"365eead5-3f15-42a8-9e68-36100faef945"),
    ARMORED("armor",2,"4e23758d-348e-42a8-8de6-08ae0a59033c"),
    MIGHTY("mighty",-1,"6d2ffe79-f034-4a06-b288-e1916c21e385"),
    GRIM("grim",1,"0f23321e-f921-4246-90b8-21ef202de224"),
    WARDED("warded",0,"b622c4d8-abc6-4db7-b3ee-5cf71b8e5286"),
    WARPED("warp",1,"107da049-af7a-4409-989a-6de23c8fe036"),
    UNDYING("undying",0,"cb9484d3-6255-4893-a4f2-3ecc375692ee"),
    FIERY("fiery",1,"6b567fdf-9245-48f5-8314-f93fe5db1427"),
    SICKLY("sickly",1,"b5718868-9ab0-424c-af1f-8b35e836b46e"),
    VENOMOUS("venomous",1,"ab9a132e-c619-4c0a-a103-10cbbcfba1a2"),
    VAMPIRIC("vampiric",1,"3412251e-af81-4c3c-93ba-2e1c33b049ea"),
    INFESTED("infested",2,"9c577fbe-ddbc-4ea2-a661-770ea775f43b"),
    TAINTED("tainted",0,"a3bb2595-8221-4140-bc73-538abcd1bbd2");
    public final String key,legacyUuid;
    public final int phase;
    ChampionModifier(String key,int phase,String legacyUuid){this.key=key;this.phase=phase;this.legacyUuid=legacyUuid;}
    public Component displayName(){return Component.translatable("champion.mod."+key);}
    public static ChampionModifier byId(int id){return id>=0&&id<values().length?values()[id]:null;}
}
