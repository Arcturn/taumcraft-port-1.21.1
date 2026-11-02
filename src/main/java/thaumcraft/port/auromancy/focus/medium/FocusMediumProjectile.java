package thaumcraft.port.auromancy.focus.medium;


import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.auromancy.focus.FocusMedium;
import thaumcraft.port.auromancy.focus.FocusNode;
import thaumcraft.port.auromancy.focus.FocusPackage;
import thaumcraft.port.auromancy.focus.NodeSetting;
import thaumcraft.port.auromancy.focus.Trajectory;
import thaumcraft.port.auromancy.entity.EntityFocusProjectile;

public class FocusMediumProjectile
extends FocusMedium {
    @Override
    public String getResearch() {
        return "FOCUSPROJECTILE@2";
    }

    @Override
    public String getKey() {
        return "thaumcraft.PROJECTILE";
    }

    @Override
    public int getComplexity() {
        int c = 4 + (this.getSettingValue("speed") - 1) / 2;
        switch (this.getSettingValue("option")) {
            case 1: {
                c += 3;
                break;
            }
            case 2:
            case 3: {
                c += 5;
            }
        }
        return c;
    }

    @Override
    public FocusNode.EnumSupplyType[] willSupply() {
        return new FocusNode.EnumSupplyType[]{FocusNode.EnumSupplyType.TARGET, FocusNode.EnumSupplyType.TRAJECTORY};
    }

    @Override
    public boolean execute(Trajectory trajectory) {
        float speed = (float)this.getSettingValue("speed") / 3.0f;
        FocusPackage p = this.getRemainingPackage();
        if (p.getCaster() != null) {
            EntityFocusProjectile projectile = new EntityFocusProjectile(p, speed, trajectory, this.getSettingValue("option"));
            return this.getPackage().getCaster().level().addFreshEntity(projectile);
        }
        return false;
    }

    @Override
    public boolean hasIntermediary() {
        return true;
    }

    @Override
    public NodeSetting[] createSettings() {
        int[] option = new int[]{0, 1, 2, 3};
        String[] optionDesc = new String[]{"focus.common.none", "focus.projectile.bouncy", "focus.projectile.seeking.hostile", "focus.projectile.seeking.friendly"};
        return new NodeSetting[]{new NodeSetting("option", "focus.common.options", new NodeSetting.NodeSettingIntList(option, optionDesc), "FOCUSPROJECTILE"), new NodeSetting("speed", "focus.projectile.speed", new NodeSetting.NodeSettingIntRange(1, 5))};
    }

    @Override
    public Aspect getAspect() {
        return Aspect.MOTION;
    }
}
