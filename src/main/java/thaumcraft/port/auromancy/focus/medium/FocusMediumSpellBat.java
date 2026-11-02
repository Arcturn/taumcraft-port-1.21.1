package thaumcraft.port.auromancy.focus.medium;


import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.auromancy.focus.FocusMedium;
import thaumcraft.port.auromancy.focus.FocusNode;
import thaumcraft.port.auromancy.focus.NodeSetting;
import thaumcraft.port.auromancy.focus.Trajectory;
import thaumcraft.port.auromancy.entity.EntitySpellBat;

public class FocusMediumSpellBat
extends FocusMedium {
    @Override
    public String getResearch() {
        return "FOCUSSPELLBAT";
    }

    @Override
    public String getKey() {
        return "thaumcraft.SPELLBAT";
    }

    @Override
    public Aspect getAspect() {
        return Aspect.BEAST;
    }

    @Override
    public int getComplexity() {
        return 8;
    }

    @Override
    public FocusNode.EnumSupplyType[] willSupply() {
        return new FocusNode.EnumSupplyType[]{FocusNode.EnumSupplyType.TARGET};
    }

    @Override
    public boolean execute(Trajectory trajectory) {
        EntitySpellBat bat = new EntitySpellBat(this.getRemainingPackage(), this.getSettingValue("target") == 1);
        bat.setPos(trajectory.source.x, trajectory.source.y, trajectory.source.z);
        return this.getPackage().getCaster().level().addFreshEntity(bat);
    }

    @Override
    public boolean hasIntermediary() {
        return true;
    }

    @Override
    public float getPowerMultiplier() {
        return 0.33f;
    }

    @Override
    public NodeSetting[] createSettings() {
        int[] friend = new int[]{0, 1};
        String[] friendDesc = new String[]{"focus.common.enemy", "focus.common.friend"};
        return new NodeSetting[]{new NodeSetting("target", "focus.common.target", new NodeSetting.NodeSettingIntList(friend, friendDesc))};
    }
}
