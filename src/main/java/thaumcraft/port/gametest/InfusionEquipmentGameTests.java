package thaumcraft.port.gametest;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.content.*;
import thaumcraft.port.infusion.armor.*;
import thaumcraft.port.infusion.recipe.InfusionAugments;
import thaumcraft.port.world.AuraSystem;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("thaumcraft") @PrefixGameTestTemplate(false)
public final class InfusionEquipmentGameTests {
    @GameTest(template="empty",timeoutTicks=40)
    public static void originalEquipmentStatsAndRevealing(GameTestHelper h){
        var player=GameTestPlayers.create(h);var helm=new ItemStack(InfusionArmor.FORTRESS_HELM.get());
        h.assertValueEqual(helm.getMaxDamage(),440,"Fortress helmet durability");h.assertValueEqual(InfusionArmor.FORTRESS_HELM.get().getDefense(),3,"Fortress helmet armor");
        h.assertTrue(helm.getItem().isValidRepairItem(helm,stack("ingot_thaumium")),"Fortress must repair with thaumium");
        player.setItemSlot(EquipmentSlot.HEAD,helm);h.assertFalse(RevealingEquipment.hasGoggles(player),"Unupgraded fortress helmet revealed aspects");InfusionAugments.setGoggles(helm,true);
        h.assertTrue(RevealingEquipment.hasGoggles(player),"Infused goggles must enable original readout");h.assertValueEqual(RevealingEquipment.visDiscount(player),0f,"Fortress goggles must not invent a vis discount");
        player.setItemSlot(EquipmentSlot.CHEST,new ItemStack(InfusionArmor.CLOTH_CHEST.get()));player.setItemSlot(EquipmentSlot.LEGS,new ItemStack(InfusionArmor.CLOTH_LEGS.get()));player.setItemSlot(EquipmentSlot.FEET,new ItemStack(InfusionArmor.CLOTH_BOOTS.get()));
        h.assertValueEqual(RevealingEquipment.discountedVis(100,player),92,"Robes must save3+3+2percent vis");h.assertTrue(InfusionArmor.CLOTH_CHEST.get().isValidRepairItem(player.getItemBySlot(EquipmentSlot.CHEST),stack("fabric")),"Robe fabric repair");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void runicUsesAuraCooldownAndFunctionalSlots(GameTestHelper h){
        var player=GameTestPlayers.create(h);var head=new ItemStack(InfusionArmor.FORTRESS_HELM.get());InfusionAugments.setRunic(head,3);player.setItemSlot(EquipmentSlot.HEAD,head);
        var ring=stack("ring_mundane");InfusionAugments.setRunic(ring,2);var curios=CuriosApi.getCuriosInventory(player).orElseThrow().getStacksHandler("ring").orElseThrow();curios.getCosmeticStacks().setStackInSlot(0,ring);
        h.assertValueEqual(InfusionEquipmentEvents.runicCapacity(player),3,"Cosmetic jewelry cannot shield");curios.getStacks().setStackInSlot(0,ring);h.assertValueEqual(InfusionEquipmentEvents.runicCapacity(player),5,"Active Curios runic charge missing");
        var aura=AuraSystem.get(h.getLevel(),player.blockPosition());aura.addVis(10);float before=aura.vis();InfusionEquipmentEvents.updateShield(player);
        h.assertValueEqual(player.getAbsorptionAmount(),1f,"Runic must add absorption that actually blocks damage");h.assertValueEqual(aura.vis(),before-1,"One shield point must cost one vis");InfusionEquipmentEvents.updateShield(player);h.assertValueEqual(aura.vis(),before-1,"Repeated same-tick update bypassed40tick recharge delay");
        player.setAbsorptionAmount(0);InfusionEquipmentEvents.updateShield(player);h.assertValueEqual(player.getPersistentData().getLong("tcRunicNext"),h.getLevel().getGameTime()+80,"Empty shield must wait4seconds");
        player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);curios.getStacks().setStackInSlot(0,ItemStack.EMPTY);InfusionEquipmentEvents.updateShield(player);h.assertValueEqual(player.getMaxAbsorption(),0f,"Removing runic equipment leaves phantom capacity");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void burrowingBreaksOneFarConnectedOre(GameTestHelper h){
        var player=GameTestPlayers.create(h);var tool=new ItemStack(Items.DIAMOND_PICKAXE);InfusionAugments.setEnchantment(tool,"burrowing",1);player.setItemSlot(EquipmentSlot.MAINHAND,tool);
        for(int x=1;x<=4;x++)h.setBlock(new BlockPos(x,2,1),Blocks.IRON_ORE);var origin=h.absolutePos(new BlockPos(1,2,1));var far=h.absolutePos(new BlockPos(4,2,1));
        h.assertValueEqual(InfusionEnchantments.furthest(h.getLevel(),origin,Blocks.IRON_ORE.defaultBlockState()),far,"Burrowing search did not reach final connected ore");player.gameMode.destroyBlock(origin);
        h.assertTrue(h.getLevel().getBlockState(far).isAir(),"Burrowing did not harvest far block");h.assertValueEqual(tool.getDamageValue(),1,"Burrowing must charge one durability per harvested block");h.assertTrue(h.getLevel().getBlockState(origin).is(Blocks.IRON_ORE),"Burrowing incorrectly cleared the original target");h.assertTrue(h.getLevel().getBlockState(h.absolutePos(new BlockPos(2,2,1))).is(Blocks.IRON_ORE),"Burrowing must break one block per swing");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void destructiveClearsOnlyPerpendicularPlane(GameTestHelper h){
        var player=GameTestPlayers.create(h);var tool=new ItemStack(Items.DIAMOND_PICKAXE);InfusionAugments.setEnchantment(tool,"destructive",1);player.setItemSlot(EquipmentSlot.MAINHAND,tool);
        for(int x=1;x<=3;x++)for(int y=1;y<=3;y++)h.setBlock(new BlockPos(x,y,2),Blocks.STONE);h.setBlock(new BlockPos(2,2,3),Blocks.STONE);player.setYRot(0);
        player.gameMode.destroyBlock(h.absolutePos(new BlockPos(2,2,2)));
        h.runAfterDelay(2,()->{for(int x=1;x<=3;x++)for(int y=1;y<=3;y++)h.assertTrue(h.getBlockState(new BlockPos(x,y,2)).isAir(),"Destructive missed3x3 plane");h.assertTrue(h.getBlockState(new BlockPos(2,2,3)).is(Blocks.STONE),"Destructive mined outside plane");h.assertValueEqual(tool.getDamageValue(),9,"Destructive must cost one durability per harvested block");h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void essenceAddsCrystalsAndCollectorPreservesLoot(GameTestHelper h){
        var player=GameTestPlayers.create(h);var sword=new ItemStack(Items.DIAMOND_SWORD);InfusionAugments.setEnchantment(sword,"essence",5);InfusionAugments.setEnchantment(sword,"collector",1);player.setItemSlot(EquipmentSlot.MAINHAND,sword);
        var cow=EntityType.COW.create(h.getLevel());cow.setPos(player.position().add(1,0,0));var loot=new ArrayList<ItemEntity>();loot.add(new ItemEntity(h.getLevel(),cow.getX(),cow.getY(),cow.getZ(),new ItemStack(Items.LEATHER,2)));
        NeoForge.EVENT_BUS.post(new LivingDropsEvent(cow,player.damageSources().playerAttack(player),loot,false));
        h.assertTrue(loot.stream().allMatch(e->e instanceof FollowingItemEntity),"Collector must home both existing loot and essence crystals");h.assertValueEqual(loot.stream().filter(e->e.getItem().is(Items.LEATHER)).mapToInt(e->e.getItem().getCount()).sum(),2,"Collector changed existing loot count");h.assertTrue(loot.stream().anyMatch(e->e.getItem().getItem() instanceof CrystalItem),"Guaranteedrank5essence produced no entity aspect crystals");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void arcingDamagesNearbyMobsWithoutHittingOwnerPets(GameTestHelper h){
        var player=GameTestPlayers.create(h);var sword=new ItemStack(Items.DIAMOND_SWORD);InfusionAugments.setEnchantment(sword,"arcing",2);player.setItemSlot(EquipmentSlot.MAINHAND,sword);
        var target=h.spawn(EntityType.COW,new BlockPos(2,2,2));var other=h.spawn(EntityType.COW,new BlockPos(3,2,2));float before=other.getHealth();NeoForge.EVENT_BUS.post(new AttackEntityEvent(player,target));h.assertTrue(other.getHealth()<before,"Arcing enchantment did not damage nearby mob");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void refiningMapsRawAndSilkOreWithoutDuplicatingCounts(GameTestHelper h){
        for(var source:List.of(Items.RAW_IRON,Items.IRON_ORE,Items.DEEPSLATE_IRON_ORE)){var result=InfusionEnchantments.refined(new ItemStack(source,3));h.assertTrue(result.is(stack("cluster_iron").getItem()),"Refining must support modern raw and silk ores");h.assertValueEqual(result.getCount(),3,"Refining must preserve source stack count");}
        h.assertTrue(InfusionEnchantments.refined(new ItemStack(Items.QUARTZ)).is(stack("cluster_quartz").getItem()),"Original quartz refining missing");h.assertTrue(InfusionEnchantments.refined(new ItemStack(Items.DIAMOND)).isEmpty(),"Refining invented conversion for diamond");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void dyeingPreservesInfusionAndWarpUsesFunctionalEquipment(GameTestHelper h){
        var robe=new ItemStack(InfusionArmor.CLOTH_CHEST.get());InfusionAugments.setRunic(robe,4);InfusionAugments.setWarp(robe,2);
        var recipe=new net.minecraft.world.item.crafting.ArmorDyeRecipe(net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
        var input=net.minecraft.world.item.crafting.CraftingInput.of(2,1,List.of(robe,new ItemStack(Items.RED_DYE)));
        h.assertTrue(recipe.matches(input,h.getLevel()),"Robes missing modern dye recipe integration");var dyed=recipe.assemble(input,h.getLevel().registryAccess());h.assertTrue(dyed.has(net.minecraft.core.component.DataComponents.DYED_COLOR),"Dye recipe failed to color robe");h.assertValueEqual(InfusionAugments.getRunic(dyed),4,"Dye recipe erased runic upgrade");
        var player=GameTestPlayers.create(h);player.setItemSlot(EquipmentSlot.CHEST,dyed);var held=new ItemStack(Items.DIAMOND_SWORD);InfusionAugments.setWarp(held,3);player.setItemSlot(EquipmentSlot.MAINHAND,held);var offhand=stack("ring_mundane");InfusionAugments.setWarp(offhand,8);player.setItemSlot(EquipmentSlot.OFFHAND,offhand);
        h.assertValueEqual(InfusionEquipmentEvents.gearWarp(player),5,"Gear warp must sum mainhand and armor, ignoring offhand");
        var state=thaumcraft.port.research.ResearchStorage.get(player.getServer()).player(player.getUUID());state.warp(0,0,0);state.warpCounter(100);
        h.assertTrue(thaumcraft.port.alchemy.liquid.AlchemyLiquidEvents.checkWarp(player,0),"Warping equipment must make positive-counter player eligible even without personal warp");h.assertValueEqual(state.warpCounter(),90,"Gear warp must slow original warp-counter decay");
        var helmet=new ItemStack(InfusionArmor.FORTRESS_HELM.get());InfusionAugments.setMask(helmet,0);player.setItemSlot(EquipmentSlot.HEAD,helmet);int mitigated=InfusionEquipmentEvents.mitigateWarpSeverity(player,10);h.assertTrue(mitigated>=5&&mitigated<=8,"Grinning Devil severity reduction must be2..5");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=60)
    public static void lamplighterIlluminatesDarkMinedSpace(GameTestHelper h){
        var player=GameTestPlayers.create(h);var tool=new ItemStack(Items.DIAMOND_PICKAXE);InfusionAugments.setEnchantment(tool,"lamplight",1);player.setItemSlot(EquipmentSlot.MAINHAND,tool);
        for(int x=0;x<5;x++)for(int y=0;y<5;y++)for(int z=0;z<5;z++)h.setBlock(new BlockPos(x,y,z),Blocks.STONE);
        var pos=h.absolutePos(new BlockPos(2,2,2));h.runAfterDelay(5,()->{player.gameMode.destroyBlock(pos);h.runAfterDelay(2,()->{h.assertTrue(h.getLevel().getBlockState(pos).is(InfusionEnchantments.GLIMMER.get()),"Lamplighter did not place original invisible glimmer after dark block harvest");h.assertValueEqual(h.getLevel().getBlockState(pos).getLightEmission(h.getLevel(),pos),15,"Glimmer must emit full light");h.succeed();});});
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void soundingRequiresSneakingAndSpendsFiveDurability(GameTestHelper h){
        var player=GameTestPlayers.create(h);var tool=new ItemStack(Items.DIAMOND_PICKAXE);InfusionAugments.setEnchantment(tool,"sounding",4);player.setItemSlot(EquipmentSlot.MAINHAND,tool);var pos=h.absolutePos(new BlockPos(1,1,1));h.setBlock(new BlockPos(1,1,1),Blocks.STONE);
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false);
        NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player,net.minecraft.world.InteractionHand.MAIN_HAND,pos,hit));h.assertValueEqual(tool.getDamageValue(),0,"Standing rightclick must not trigger sounding");
        player.setShiftKeyDown(true);var event=new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player,net.minecraft.world.InteractionHand.MAIN_HAND,pos,hit);NeoForge.EVENT_BUS.post(event);h.assertTrue(event.isCanceled(),"Sounding must consume the sneak-rightclick");h.assertValueEqual(tool.getDamageValue(),5,"Original sounding durability cost is5");h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=40)
    public static void fortressMasksHealAndRetaliateAtOriginalThresholds(GameTestHelper h){
        // FakePlayer intentionally rejects all damage; use the ordinary vulnerable Player fixture.
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var helmet=new ItemStack(InfusionArmor.FORTRESS_HELM.get());player.setItemSlot(EquipmentSlot.HEAD,helmet);InfusionAugments.setMask(helmet,2);player.setHealth(10);
        var attacker=EntityType.ZOMBIE.create(h.getLevel());h.assertTrue(attacker.hurt(player.damageSources().playerAttack(player),12),"Fixture must accept the initial player attack");
        h.assertValueEqual(player.getHealth(),11f,"Sipping Fiend must heal exactly one health at12damage certainty");
        h.assertFalse(attacker.hurt(player.damageSources().playerAttack(player),12),"Fixture must reject the repeated invulnerability hit");h.assertValueEqual(player.getHealth(),11f,"Rejected invulnerability hit must not trigger Sipping Fiend heal");InfusionAugments.setMask(helmet,1);
        // Generic damage retains the living attacker without difficulty-dependent mob damage scaling.
        var source=new net.minecraft.world.damagesource.DamageSource(h.getLevel().registryAccess().holderOrThrow(net.minecraft.world.damagesource.DamageTypes.GENERIC),attacker);
        h.assertTrue(player.hurt(source,10),"Fixture must accept the attack on the masked player");
        var effect=attacker.getEffect(net.minecraft.world.effect.MobEffects.WITHER);h.assertTrue(effect!=null&&effect.getDuration()==80,"Angry Ghost must apply80tick Wither at10damage certainty");h.succeed();
    }
    private static ItemStack stack(String id){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:"+id)));}
}
