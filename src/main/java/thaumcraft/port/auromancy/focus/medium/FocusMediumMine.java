package thaumcraft.port.auromancy.focus.medium;


import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.auromancy.focus.FocusMedium;
import thaumcraft.port.auromancy.focus.FocusNode;
import thaumcraft.port.auromancy.focus.NodeSetting;
import thaumcraft.port.auromancy.focus.Trajectory;
import thaumcraft.port.auromancy.entity.EntityFocusMine;

public class FocusMediumMine
extends FocusMedium {
    @Override
    public String getResearch() {
        return "FOCUSMINE";
    }

    @Override
    public String getKey() {
        return "thaumcraft.MINE";
    }

    @Override
    public int getComplexity() {
        return 4;
    }

    @Override
    public Aspect getAspect() {
        return Aspect.TRAP;
    }

    @Override
    public FocusNode.EnumSupplyType[] willSupply() {
        return new FocusNode.EnumSupplyType[]{FocusNode.EnumSupplyType.TARGET, FocusNode.EnumSupplyType.TRAJECTORY};
    }

    @Override
    public boolean execute(Trajectory trajectory) {
        EntityFocusMine projectile = new EntityFocusMine(this.getRemainingPackage(), trajectory, this.getSettingValue("target") == 1);
        return this.getPackage().getCaster().level().addFreshEntity(projectile);
    }

    @Override
    public boolean hasIntermediary() {
        return true;
    }

    @Override
    public NodeSetting[] createSettings() {
        int[] friend = new int[]{0, 1};
        String[] friendDesc = new String[]{"focus.common.enemy", "focus.common.friend"};
        return new NodeSetting[]{new NodeSetting("target", "focus.common.target", new NodeSetting.NodeSettingIntList(friend, friendDesc))};
    }
}
