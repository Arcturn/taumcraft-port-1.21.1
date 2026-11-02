package thaumcraft.port.gametest;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.auromancy.firebat.FireBat;
import thaumcraft.port.auromancy.firebat.FireBatContent;
import thaumcraft.port.research.ResearchStorage;
import thaumcraft.port.research.Scanner;
import thaumcraft.port.research.Scanning;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class FireBatGameTests {
    @GameTest(template="empty",timeoutTicks=60)
    public static void onlyTheRealWildFireBatSuppliesTheSpellBatDiscovery(GameTestHelper h){
        var player=GameTestPlayers.create(h);var state=ResearchStorage.get(h.getLevel().getServer()).player(player.getUUID());
        var ordinary=EntityType.BAT.create(h.getLevel());
        Scanning.scan(player,Scanner.observation(player,new EntityHitResult(ordinary)),false);
        h.assertTrue(state.knows("f_BAT"),"Vanilla bat did not supply the ordinary bat discovery");
        h.assertFalse(state.knows("!Firebat"),"A generic bat incorrectly unlocked the firebat prerequisite");
        var firebat=FireBatContent.FIREBAT.get().create(h.getLevel());
        var observation=Scanner.observation(player,new EntityHitResult(firebat));
        h.assertTrue(observation.aspects().equals(Map.of(Aspect.BEAST,5,Aspect.FLIGHT,5,Aspect.FIRE,10)),"Firebat aspects differ from ConfigAspects");
        Scanning.scan(player,observation,false);
        h.assertTrue(state.knows("!Firebat")&&state.knows("f_BAT")&&state.knows("f_FLY"),"Scanning the firebat failed to unlock its exact research predicates");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=60)
    public static void fireAndExplosionsCannotWakeOrDamageAHangingBatButPhysicalDamageCan(GameTestHelper h){
        var bat=FireBatContent.FIREBAT.get().create(h.getLevel());var damage=h.getLevel().damageSources();
        h.assertValueEqual(bat.getMaxHealth(),5f,"Original firebat health changed");
        h.assertTrue(bat.hanging(),"Wild firebat must begin hanging");
        h.assertFalse(bat.hurt(damage.inFire(),20),"Fire damaged a firebat");
        h.assertFalse(bat.hurt(damage.explosion(bat,bat),20),"Explosion damaged a firebat");
        h.assertTrue(bat.hanging()&&bat.getHealth()==5,"Rejected damage woke the bat or changed health");
        h.assertTrue(bat.hurt(damage.generic(),1),"Physical damage was incorrectly rejected");
        h.assertFalse(bat.hanging(),"Physical damage did not release the hanging bat");
        bat.setHanging(true);bat.damageBonus=3;var data=new CompoundTag();bat.addAdditionalSaveData(data);
        var loaded=FireBatContent.FIREBAT.get().create(h.getLevel());loaded.readAdditionalSaveData(data);
        h.assertTrue(loaded.hanging()&&loaded.damageBonus==3,"Firebat lost original hang/damBonus fields on save");
        h.assertFalse(loaded.causeFallDamage(100,1,damage.fall()),"Flying bat took fall damage");
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=60)
    public static void batAttackUsesTheOriginalRangeAndRandomCooldown(GameTestHelper h){
        var origin=Vec3.atCenterOf(h.absolutePos(new BlockPos(1,20,1)));boolean attacked=false;
        for(int seed=0;seed<32&&!attacked;seed++){
            var bat=FireBatContent.FIREBAT.get().create(h.getLevel());bat.setPos(origin);bat.setNoGravity(true);bat.setHanging(false);
            var target=EntityType.IRON_GOLEM.create(h.getLevel());target.setNoAi(true);target.setPos(origin.add(2.5,0,0));bat.setTarget(target);
            bat.getRandom().setSeed(seed);bat.tick();
            h.assertValueEqual(target.getHealth(),target.getMaxHealth(),"Firebat attacked at the excluded 2.5-block boundary");
            bat.setPos(origin);bat.setDeltaMovement(Vec3.ZERO);target.setPos(origin.add(2,0,0));bat.getRandom().setSeed(seed);bat.tick();
            if(bat.isRemoved())continue;
            h.assertValueEqual(target.getMaxHealth()-target.getHealth(),1f,"Firebat bite did not use its original one-point attack attribute");
            int delay=cooldown(bat);h.assertTrue(delay>=20&&delay<=39,"Firebat attack cooldown is outside 20..39 ticks");attacked=true;
        }
        h.assertTrue(attacked,"All fixture attacks exploded or failed to reach the target");h.succeed();
    }
    private static int cooldown(FireBat bat){try{var field=FireBat.class.getDeclaredField("attackTime");field.setAccessible(true);return field.getInt(bat);}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
    private FireBatGameTests(){}
}
