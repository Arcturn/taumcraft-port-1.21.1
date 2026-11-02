package thaumcraft.port.auromancy.focus;

import net.minecraft.resources.ResourceLocation;
import thaumcraft.port.auromancy.focus.effect.*;
import thaumcraft.port.auromancy.focus.medium.*;
import thaumcraft.port.auromancy.focus.mod.*;

/** ConfigItems' original registration order, texture paths and colors. */
public final class FocusCatalog {
    static void register() {
        add(FocusMediumRoot.class,"root",0x999999);
        add(FocusMediumTouch.class,"touch",11371909);
        add(FocusMediumBolt.class,"bolt",11377029);
        add(FocusMediumProjectile.class,"projectile",11382149);
        add(FocusMediumCloud.class,"cloud",10071429);
        add(FocusMediumMine.class,"mine",8760709);
        add(FocusMediumPlan.class,"plan",8760728);
        add(FocusMediumSpellBat.class,"spellbat",8760748);
        add(FocusEffectFire.class,"fire",16734721);
        add(FocusEffectFrost.class,"frost",0xE1FFFF);
        add(FocusEffectAir.class,"air",0xFFFF7E);
        add(FocusEffectEarth.class,"earth",5685248);
        add(FocusEffectFlux.class,"flux",0x800080);
        add(FocusEffectBreak.class,"break",9063176);
        add(FocusEffectRift.class,"rift",3084645);
        add(FocusEffectExchange.class,"exchange",5735255);
        add(FocusEffectCurse.class,"curse",6946821);
        add(FocusEffectHeal.class,"heal",14548997);
        add(FocusModScatter.class,"scatter",0x999999);
        add(FocusModSplitTarget.class,"split_target",0x999999);
        add(FocusModSplitTrajectory.class,"split_trajectory",0x999999);
    }
    private static void add(Class<? extends IFocusElement> type,String texture,int color) {
        FocusEngine.registerElement(type,ResourceLocation.parse("thaumcraft:textures/foci/"+texture+".png"),color);
    }
    private FocusCatalog() { }
}
