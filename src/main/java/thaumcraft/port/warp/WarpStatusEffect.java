package thaumcraft.port.warp;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import thaumcraft.port.world.WorldContent;

/** Server behavior of the five BETA26 warp potions; their visual effects live on the client. */
final class WarpStatusEffect extends MobEffect {
    enum Kind { THAUMARHIA, UNNATURAL_HUNGER, BLURRED_VISION, SUN_SCORNED, DEATH_GAZE }
    private final Kind kind;
    private final String translation;

    WarpStatusEffect(Kind kind, int color, String translation) {
        super(MobEffectCategory.HARMFUL, color);
        this.kind = kind;
        this.translation = translation;
    }

    @Override public String getDescriptionId() { return translation; }

    @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return switch (kind) {
            case THAUMARHIA -> duration % 20 == 0;
            case UNNATURAL_HUNGER -> true;
            case SUN_SCORNED -> duration % 40 == 0;
            // Death gaze runs once per player second, not on the effect's remaining duration.
            case BLURRED_VISION, DEATH_GAZE -> false;
        };
    }

    @Override public boolean applyEffectTick(LivingEntity target, int amplifier) {
        var level = target.level();
        if (level.isClientSide) return true;
        switch (kind) {
            case THAUMARHIA -> {
                if (level.random.nextInt(15) == 0 && level.isEmptyBlock(target.blockPosition()))
                    level.setBlockAndUpdate(target.blockPosition(), WorldContent.block("flux_goo").defaultBlockState());
            }
            case UNNATURAL_HUNGER -> {
                if (target instanceof Player player) player.causeFoodExhaustion(.025f * (amplifier + 1));
            }
            case SUN_SCORNED -> {
                float brightness = target.getLightLevelDependentMagicValue();
                if (brightness > .5f && level.random.nextFloat() * 30 < (brightness - .4f) * 2
                        && level.canSeeSky(target.blockPosition())) target.igniteForSeconds(4);
                else if (brightness < .25f && level.random.nextFloat() > brightness * 2) target.heal(1);
            }
            default -> { }
        }
        return true;
    }
}
