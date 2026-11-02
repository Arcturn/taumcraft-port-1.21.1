package thaumcraft.port.auromancy.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;

/** One anatomical-arm mapping shared by both camera perspectives. Never equate off hand with left. */
public final class CasterHand {
    public static HumanoidArm arm(HumanoidArm mainArm, InteractionHand hand) { return hand == InteractionHand.MAIN_HAND ? mainArm : mainArm.getOpposite(); }
    public static InteractionHand hand(HumanoidArm mainArm, HumanoidArm arm) { return arm == mainArm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND; }
    public static float armWidth(boolean slim) { return slim ? 3 : 4; }
    public static float armCenter(boolean slim, HumanoidArm arm) { return (arm == HumanoidArm.RIGHT ? -1 : 1) * (slim ? .5f : 1f); }
    private CasterHand() { }
}
