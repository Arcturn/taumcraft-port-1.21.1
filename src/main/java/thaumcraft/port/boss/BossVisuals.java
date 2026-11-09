package thaumcraft.port.boss;

import java.util.function.Consumer;

/** Client effects are bound by the client setup without loading renderer classes on a server. */
public final class BossVisuals {
    public static Consumer<EldritchGolem> charge=entity->{},golemTick=entity->{};
    public static Consumer<EldritchWarden> wisp=entity->{},sonic=entity->{};
    public static Consumer<GolemOrb> orbTrail=entity->{},impact=entity->{};
    private BossVisuals(){}
}
