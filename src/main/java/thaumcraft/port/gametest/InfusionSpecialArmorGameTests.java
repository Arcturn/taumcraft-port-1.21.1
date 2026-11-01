package thaumcraft.port.gametest;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.infusion.armor.InfusionArmor;
import thaumcraft.port.infusion.content.InfusionCraftingContent;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class InfusionSpecialArmorGameTests {
    @GameTest(template="empty",batch="infusion_content",timeoutTicks=30)
    public static void voidRobesUseOriginalSpecialAbsorptionAndDurability(GameTestHelper helper){
        var player=helper.makeMockPlayer(GameType.SURVIVAL);
        var helmet=new ItemStack(InfusionCraftingContent.VOID_ROBE_HELM.get());
        var chest=new ItemStack(InfusionCraftingContent.VOID_ROBE_CHEST.get());
        var legs=new ItemStack(InfusionCraftingContent.VOID_ROBE_LEGS.get());
        player.setItemSlot(EquipmentSlot.HEAD,helmet);player.setItemSlot(EquipmentSlot.CHEST,chest);player.setItemSlot(EquipmentSlot.LEGS,legs);
        // This mock is not world-ticked. Equipment attribute modifiers are applied
        // by LivingEntity's next equipment update, rather than by setItemSlot.
        player.tick();
        helper.assertValueEqual(player.getArmorValue(),20,"Void robe fixture did not apply equipped armor attributes");
        helper.assertValueEqual(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS),6d,"Void robe fixture toughness");
        // minecraft:generic bypasses armor; use an unscaled, blockable player hit.
        var source=player.damageSources().playerAttack(helper.makeMockPlayer(GameType.SURVIVAL));
        helper.assertFalse(source.is(DamageTypeTags.BYPASSES_ARMOR),"Physical damage fixture must exercise armor absorption");
        // BETA26: (4 + 9 + 7) / 25 special absorption, followed by normal armor.
        float expected=CombatRules.getDamageAfterAbsorb(player,2,source,20,6);
        float beforeHit=player.getHealth();int beforeArmor=player.getArmorValue();double beforeToughness=player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        helper.assertTrue(player.hurt(source,10),"Fixture rejected the initial hit");
        helper.assertTrue(Math.abs(player.getHealth()-(beforeHit-expected))<.0001f,"Void robes special absorption: beforeHealth="+beforeHit
                +", afterHealth="+player.getHealth()+", expectedDamage="+expected+", actualDamage="+(beforeHit-player.getHealth())
                +", rawDamage=10, beforeArmor="+beforeArmor+", beforeToughness="+beforeToughness
                +", afterArmor="+player.getArmorValue()+", afterToughness="+player.getAttributeValue(Attributes.ARMOR_TOUGHNESS)
                +", source="+source.getMsgId()+", wear="+helmet.getDamageValue()+"/"+chest.getDamageValue()+"/"+legs.getDamageValue());
        helper.assertValueEqual(helmet.getDamageValue(),2,"Void hood must pay special plus ordinary armor wear");
        helper.assertValueEqual(chest.getDamageValue(),4,"Void chest must pay special plus ordinary armor wear");
        helper.assertValueEqual(legs.getDamageValue(),3,"Void legs must pay special plus ordinary armor wear");
        // The original Forge default handleUnblockableDamage is false for these items.
        player.invulnerableTime=0;float before=player.getHealth();
        helper.assertTrue(player.hurt(player.damageSources().magic(),5),"Fixture rejected magic hit");
        helper.assertTrue(Math.abs(player.getHealth()-(before-5))<.0001f,"Void robes invented protection against unblockable magic");
        helper.assertValueEqual(chest.getDamageValue(),4,"Unblockable magic incorrectly damaged void robe armor");
        helper.succeed();
    }

    @GameTest(template="empty",batch="infusion_content",timeoutTicks=30)
    public static void mixedFortressAndVoidRobesRespectSourceArmorPriority(GameTestHelper helper){
        var player=helper.makeMockPlayer(GameType.SURVIVAL);
        var helmet=new ItemStack(InfusionArmor.FORTRESS_HELM.get());
        var chest=new ItemStack(InfusionCraftingContent.VOID_ROBE_CHEST.get());
        var legs=new ItemStack(InfusionCraftingContent.VOID_ROBE_LEGS.get());
        player.setItemSlot(EquipmentSlot.HEAD,helmet);player.setItemSlot(EquipmentSlot.CHEST,chest);player.setItemSlot(EquipmentSlot.LEGS,legs);
        player.tick();
        helper.assertTrue(player.getArmorValue()>=19,"Mixed armor fixture did not apply equipped armor attributes");
        helper.assertTrue(player.getAttributeValue(Attributes.ARMOR_TOUGHNESS)>=7,"Mixed armor fixture toughness");
        var source=player.damageSources().inFire();
        helper.assertTrue(player.hurt(source,10),"Fixture rejected fire hit");
        // Priority 1 fortress: 3/20; priority 0 robes: (9+7)/25 of the remainder.
        float remaining=10*(1-3/20f)*(1-16/25f);
        float expected=CombatRules.getDamageAfterAbsorb(player,remaining,source,player.getArmorValue(),
                (float)player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        helper.assertTrue(Math.abs(player.getHealth()-(20-expected))<.0001f,"Mixed special armor did not absorb in source priority order");
        helper.assertValueEqual(helmet.getDamageValue(),2,"Fortress special armor wear changed");
        helper.assertValueEqual(chest.getDamageValue(),4,"Void robe wear used unabsorbed high-priority damage");
        helper.assertValueEqual(legs.getDamageValue(),3,"Void leggings wear used unabsorbed high-priority damage");
        helper.succeed();
    }
    private InfusionSpecialArmorGameTests() { }
}
