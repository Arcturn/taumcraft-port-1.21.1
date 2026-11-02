package thaumcraft.port.auromancy.focus.medium;


import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.auromancy.focus.FocusMedium;
import thaumcraft.port.auromancy.focus.FocusNode;
import thaumcraft.port.auromancy.focus.NodeSetting;
import thaumcraft.port.auromancy.focus.Trajectory;
import thaumcraft.port.auromancy.entity.EntityFocusCloud;

public class FocusMediumCloud
extends FocusMedium {
    @Override
    public String getResearch() {
        return "FOCUSCLOUD";
    }

    @Override
    public String getKey() {
        return "thaumcraft.CLOUD";
    }

    @Override
    public Aspect getAspect() {
        return Aspect.ALCHEMY;
    }

    @Override
    public int getComplexity() {
        return 4 + this.getSettingValue("radius") * 2 + this.getSettingValue("duration") / 5;
    }

    @Override
    public FocusNode.EnumSupplyType[] willSupply() {
        return new FocusNode.EnumSupplyType[]{FocusNode.EnumSupplyType.TARGET};
    }

    @Override
    public boolean execute(Trajectory trajectory) {
        EntityFocusCloud cloud = new EntityFocusCloud(this.getRemainingPackage(), trajectory, this.getSettingValue("radius"), this.getSettingValue("duration"));
        return this.getPackage().getCaster().level().addFreshEntity(cloud);
    }

    @Override
    public boolean hasIntermediary() {
        return true;
    }

    @Override
    public NodeSetting[] createSettings() {
        return new NodeSetting[]{new NodeSetting("radius", "focus.common.radius", new NodeSetting.NodeSettingIntRange(1, 3)), new NodeSetting("duration", "focus.common.duration", new NodeSetting.NodeSettingIntRange(5, 30))};
    }

    @Override
    public float getPowerMultiplier() {
        return 0.5f;
    }
}
