package thaumcraft.port.gametest;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.alchemy.material.AlchemyMaterials;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.RevealingReadout;
import thaumcraft.port.essentia.*;
import thaumcraft.port.infusion.matrix.*;
import thaumcraft.port.infusion.recipe.*;
import thaumcraft.port.infusion.structure.*;
import thaumcraft.port.research.ResearchStorage;
import thaumcraft.port.world.AuraSystem;
import thaumcraft.port.world.WorldContent;

/** Real registered altar recipes, item stands, remote capabilities and persisted in-progress rituals. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class InfusionMatrixGameTests {
    private static final String TEMPLATE="infusion_matrix_empty";
    private static final BlockPos MATRIX=new BlockPos(16,12,16);
    private static final int[][] COMPONENTS={{-3,0},{3,0},{-3,-3},{3,3},{-3,3},{3,-3},{0,-3},{0,3}};
    private record Ritual(InfusionMatrixEntity matrix,PedestalEntity center,ServerPlayer player,InfusionRecipe recipe,List<PedestalEntity> pedestals,List<JarEntity> jars){}

    @GameTest(template=TEMPLATE,batch="infusion_matrix")
    public static void casterActivationRequiresCentralPedestalAndAllFourPillars(GameTestHelper helper){
        helper.setBlock(MATRIX,MatrixContent.MATRIX.get());var matrix=matrix(helper);var player=GameTestPlayers.create(helper);
        helper.assertFalse(matrix.activateOrStart(player),"Unformed matrix activated");
        altar(helper,StructureContent.PILLAR_ARCANE.get());helper.assertTrue(matrix.validLocation(),"Correct altar rejected");
        helper.assertTrue(matrix.activateOrStart(player),"First caster click did not activate");helper.assertTrue(matrix.active&&!matrix.crafting,"Activation unexpectedly started an empty ritual");
        helper.assertFalse(matrix.activateOrStart(player),"Empty altar accepted a recipe");
        helper.setBlock(MATRIX.offset(1,-2,1),Blocks.AIR);helper.assertFalse(matrix.validLocation(),"Missing pillar accepted");
        for(int tick=0;tick<100;tick++)matrix.tick();helper.assertFalse(matrix.active,"Broken altar remained active after structure check");
        helper.succeed();
    }
    @GameTest(template=TEMPLATE,batch="infusion_matrix")
    public static void unknownResearchCannotStartOrConsumeItems(GameTestHelper helper){
        var ritual=ritual(helper,"traveller_boots",new ItemStack(Items.LEATHER_BOOTS),false,true);
        helper.assertFalse(ritual.matrix.activateOrStart(ritual.player),"Unknown research bypassed the matrix gate");
        helper.assertFalse(ritual.matrix.crafting,"Unresearched recipe started");
        helper.assertTrue(ritual.pedestals.stream().noneMatch(PedestalEntity::isEmpty),"Failed start consumed components");
        helper.assertTrue(ritual.jars.stream().allMatch(j->j.storedAmount()==240),"Failed start drained essentia");helper.succeed();
    }
    @GameTest(template=TEMPLATE,batch="infusion_matrix",timeoutTicks=120)
    public static void staticInfusionConsumesExactEssentiaAndKeepsProportionalDamage(GameTestHelper helper){
        ItemStack input=new ItemStack(Items.LEATHER_BOOTS);input.setDamageValue(20);input.set(DataComponents.CUSTOM_NAME,Component.literal("Old boots"));
        var ritual=ritual(helper,"traveller_boots",input,true,true);int expected=(int)(item("traveller_boots").getDefaultInstance().getMaxDamage()*(20f/input.getMaxDamage()));
        helper.assertTrue(ritual.matrix.activateOrStart(ritual.player),"Known boots recipe did not start");
        var costs=ritual.matrix.remainingEssentia();int total=costs.values().stream().mapToInt(Integer::intValue).sum();
        helper.assertValueEqual(total,200,"Ordinary altar changed original 100+100 essentia cost");
        for(int i=0;i<total;i++)ritual.matrix.craftCycle();
        helper.assertTrue(ritual.matrix.remainingEssentia().isEmpty(),"Essentia phase did not finish after exactly its unit count");
        helper.assertTrue(ritual.pedestals.stream().noneMatch(PedestalEntity::isEmpty),"Component consumed before all essentia");
        helper.assertValueEqual(ritual.jars.stream().mapToInt(JarEntity::storedAmount).sum(),ritual.jars.size()*240-total,"Essentia conservation failed");
        finish(helper,ritual.matrix,100);
        helper.assertTrue(ritual.center.getItem(0).is(item("traveller_boots")),"Actual static output incorrect");
        helper.assertValueEqual(ritual.center.getItem(0).getDamageValue(),expected,"Static result lost proportional input damage");
        helper.assertFalse(ritual.center.getItem(0).has(DataComponents.CUSTOM_NAME),"Static recipe copied arbitrary input components");
        helper.assertTrue(ritual.pedestals.stream().allMatch(PedestalEntity::isEmpty),"Finished ritual left consumed ingredients");
        helper.assertValueEqual(ritual.matrix.instability(),0,"Finished ritual retained instability");helper.succeed();
    }
    @GameTest(template=TEMPLATE,batch="infusion_matrix",timeoutTicks=120)
    public static void dynamicRunicAndInfusionEnchantmentsPreserveCentralComponents(GameTestHelper helper){
        ItemStack armor=new ItemStack(Items.IRON_HELMET);armor.setDamageValue(17);armor.set(DataComponents.CUSTOM_NAME,Component.literal("The sentinel"));
        var runic=ritual(helper,"runic_armor",armor,true,true);helper.assertTrue(runic.matrix.activateOrStart(runic.player),"Runic recipe did not start");finish(helper,runic.matrix,200);
        var result=runic.center.getItem(0);helper.assertValueEqual(InfusionAugments.getRunic(result),1,"Runic shielding did not increase");helper.assertValueEqual(result.getDamageValue(),17,"Runic augmentation repaired input");helper.assertValueEqual(result.getHoverName().getString(),"The sentinel","Runic augmentation discarded name");
        clearContents(helper,runic);
        ItemStack pick=new ItemStack(Items.DIAMOND_PICKAXE);pick.setDamageValue(53);pick.set(DataComponents.CUSTOM_NAME,Component.literal("Collector"));
        var enchant=ritual(helper,"ie_collector",pick,true,true);helper.assertTrue(enchant.matrix.activateOrStart(enchant.player),"Infusion enchantment did not start");finish(helper,enchant.matrix,300);
        result=enchant.center.getItem(0);helper.assertValueEqual(InfusionAugments.getEnchantment(result,"collector"),1,"Actual matrix lost infusion enchantment");helper.assertValueEqual(result.getDamageValue(),53,"Infusion enchantment discarded damage");helper.assertValueEqual(result.getHoverName().getString(),"Collector","Infusion enchantment discarded name");helper.succeed();
    }
    @GameTest(template=TEMPLATE,batch="infusion_matrix",timeoutTicks=120)
    public static void ingredientAbsorptionWaitsFiveCyclesAndKeepsMilkBucketRemainder(GameTestHelper helper){
        var recipe=recipe(helper,"verdant_charm");var ritual=ritual(helper,"verdant_charm",recipe.input().getItems()[0].copy(),true,true);
        helper.assertTrue(ritual.matrix.activateOrStart(ritual.player),"Verdant charm recipe did not start");
        int cost=ritual.matrix.remainingEssentia().values().stream().mapToInt(Integer::intValue).sum();for(int i=0;i<cost;i++)ritual.matrix.craftCycle();
        ritual.matrix.craftCycle();for(int i=0;i<4;i++)ritual.matrix.craftCycle();
        helper.assertTrue(ritual.pedestals.stream().noneMatch(PedestalEntity::isEmpty),"Ingredient disappeared before five absorption delay cycles");
        ritual.matrix.craftCycle();helper.assertValueEqual(ritual.matrix.remainingIngredients().size(),3,"First ingredient was not consumed after absorption delay");
        finish(helper,ritual.matrix,100);helper.assertTrue(ritual.center.getItem(0).is(item("verdant_charm")),"Charm output missing");
        helper.assertValueEqual(ritual.pedestals.stream().filter(p->p.getItem(0).is(Items.BUCKET)).count(),1L,"Milk bucket crafting remainder was lost or duplicated");helper.succeed();
    }
    @GameTest(template=TEMPLATE,batch="infusion_matrix",timeoutTicks=120)
    public static void unfinishedRitualSurvivesUnloadAndDoesNotChargeTwice(GameTestHelper helper){
        var ritual=ritual(helper,"runic_armor",new ItemStack(Items.IRON_CHESTPLATE),true,true);ritual.matrix.activateOrStart(ritual.player);
        for(int i=0;i<23;i++)ritual.matrix.craftCycle();int paid=ritual.jars.size()*240-ritual.jars.stream().mapToInt(JarEntity::storedAmount).sum();
        helper.assertValueEqual(paid,23,"Pre-save phase failed to drain one unit each cycle");
        var saved=ritual.matrix.saveWithFullMetadata(helper.getLevel().registryAccess());var restored=new InfusionMatrixEntity(ritual.matrix.getBlockPos(),ritual.matrix.getBlockState());
        restored.loadWithComponents(saved,helper.getLevel().registryAccess());helper.getLevel().setBlockEntity(restored);restored.scanSurroundings();
        helper.assertTrue(restored.active&&restored.crafting,"Saved ritual did not resume");helper.assertValueEqual(restored.remainingEssentia(),ritual.matrix.remainingEssentia(),"Saved remaining essentia changed");
        finish(helper,restored,200);helper.assertValueEqual(InfusionAugments.getRunic(ritual.center.getItem(0)),1,"Reload lost dynamic output");
        helper.assertValueEqual(ritual.jars.size()*240-ritual.jars.stream().mapToInt(JarEntity::storedAmount).sum(),80,"Reload duplicated cost or refunded consumed essentia");helper.succeed();
    }
    @GameTest(template=TEMPLATE,batch="infusion_matrix")
    public static void missingAndBracedEssentiaStallsWithoutTakingItems(GameTestHelper helper){
        var ritual=ritual(helper,"runic_armor",new ItemStack(Items.IRON_BOOTS),true,true);for(var jar:ritual.jars)jar.braced=true;
        ritual.matrix.activateOrStart(ritual.player);var demand=ritual.matrix.remainingEssentia();for(int i=0;i<20;i++)ritual.matrix.craftCycle();
        helper.assertTrue(ritual.matrix.crafting,"Insufficient essentia should stall the ritual");helper.assertValueEqual(ritual.matrix.remainingEssentia(),demand,"Braced jars were drained");helper.assertTrue(ritual.pedestals.stream().noneMatch(PedestalEntity::isEmpty),"Stalled ritual ate components");
        helper.assertValueEqual(InfusionAugments.getRunic(ritual.center.getItem(0)),0,"Stalled ritual granted output");helper.succeed();
    }
    @GameTest(template=TEMPLATE,batch="infusion_matrix")
    public static void repairedAltarCanReactivateAnInterruptedRitual(GameTestHelper helper){
        var ritual=ritual(helper,"runic_armor",new ItemStack(Items.IRON_BOOTS),true,true);ritual.matrix.activateOrStart(ritual.player);
        helper.setBlock(MATRIX.offset(1,-2,1),Blocks.AIR);for(int tick=0;tick<20;tick++)ritual.matrix.tick();
        helper.assertTrue(!ritual.matrix.active&&ritual.matrix.crafting,"Broken altar should pause the existing ritual");var remaining=ritual.matrix.remainingEssentia();
        helper.setBlock(MATRIX.offset(1,-2,1),StructureContent.PILLAR_ARCANE.get());helper.assertTrue(ritual.matrix.activateOrStart(ritual.player),"Caster could not reactivate a repaired altar");
        helper.assertValueEqual(ritual.matrix.remainingEssentia(),remaining,"Reactivation recalculated already paid costs");helper.assertTrue(ritual.matrix.active&&ritual.matrix.crafting,"Reactivation lost the existing ritual");helper.succeed();
    }
    @GameTest(template=TEMPLATE,batch="infusion_matrix")
    public static void instabilityDiscoveryReachesNearbyPlayersOnlyAndIsIdempotent(GameTestHelper helper){
        var matrix=altar(helper,StructureContent.PILLAR_ARCANE.get());var player=GameTestPlayers.create(helper);player.setPos(matrix.getBlockPos().getX()+1,matrix.getBlockPos().getY(),matrix.getBlockPos().getZ());helper.getLevel().addNewPlayer(player);
        try{
            var state=ResearchStorage.get(helper.getLevel().getServer()).player(player.getUUID());helper.assertFalse(state.knows("!INSTABILITY"),"Fixture already knew instability");
            matrix.discoverInstability();helper.assertTrue(state.knows("!INSTABILITY"),"Instability incident did not unlock hidden discovery");int flags=state.flags().size();matrix.discoverInstability();helper.assertValueEqual(state.flags().size(),flags,"Discovery was awarded twice");
        }finally{helper.getLevel().removePlayerImmediately(player,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);}
        helper.succeed();
    }
    @GameTest(template=TEMPLATE,batch="infusion_matrix")
    public static void damagingAndHarmfulMishapsUseMagicDamageAndUncurableVisExhaust(GameTestHelper helper){
        var matrix=altar(helper,StructureContent.PILLAR_ARCANE.get());var victim=helper.spawnWithNoFreeWill(EntityType.COW,MATRIX.offset(2,0,0));float before=victim.getHealth();matrix.instabilityEvent(7);
        float lost=before-victim.getHealth();helper.assertTrue(lost>=4&&lost<=7,"Zap did not deal original 4..7 magic damage");
        for(int i=0;i<100;i++)matrix.instabilityEvent(18);
        var exhaust=victim.getEffect(thaumcraft.port.world.WorldEntities.VIS_EXHAUST);var taint=victim.getEffect(thaumcraft.port.world.WorldEntities.TAINT);
        helper.assertTrue(exhaust!=null&&exhaust.getDuration()==2400&&exhaust.getCures().isEmpty(),"Vis exhaustion lacks original duration or milk immunity");
        helper.assertTrue(taint!=null&&taint.getDuration()==120&&!taint.isAmbient(),"Flux taint lacks original duration/visibility");helper.succeed();
    }
    @GameTest(template=TEMPLATE,batch="infusion_matrix")
    public static void removedIngredientReintroducesPaidAspectAndNeverCompletes(GameTestHelper helper){
        var ritual=ritual(helper,"runic_armor",new ItemStack(Items.IRON_BOOTS),true,true);ritual.matrix.activateOrStart(ritual.player);
        for(int i=0;i<80;i++)ritual.matrix.craftCycle();for(var pedestal:ritual.pedestals)pedestal.setItem(0,ItemStack.EMPTY);
        ritual.matrix.craftCycle();helper.assertTrue(ritual.matrix.crafting,"Missing components aborted instead of stalling");helper.assertTrue(!ritual.matrix.remainingEssentia().isEmpty(),"Missing component did not reintroduce a previously paid aspect");
        helper.assertValueEqual(InfusionAugments.getRunic(ritual.center.getItem(0)),0,"Missing component nevertheless produced result");helper.succeed();
    }
    @GameTest(template=TEMPLATE,batch="infusion_matrix")
    public static void replacedCentralItemAbortsWithoutProducingCapturedOutput(GameTestHelper helper){
        var ritual=ritual(helper,"runic_armor",new ItemStack(Items.IRON_BOOTS),true,true);ritual.matrix.activateOrStart(ritual.player);ritual.matrix.craftCycle();
        ritual.center.setItem(0,new ItemStack(Items.DIAMOND));ritual.matrix.craftCycle();
        helper.assertFalse(ritual.matrix.crafting,"Changed central item left ritual running");helper.assertTrue(ritual.matrix.remainingEssentia().isEmpty(),"Aborted ritual retained aspect demand");helper.assertTrue(ritual.center.getItem(0).isEmpty()||ritual.center.getItem(0).is(Items.DIAMOND),"Abort replaced a foreign item with captured output");helper.succeed();
    }
    @GameTest(template=TEMPLATE,batch="infusion_matrix")
    public static void stabilityUsesMirrorPairsDiminishingReturnsAndOccupancy(GameTestHelper helper){
        var matrix=altar(helper,StructureContent.PILLAR_ARCANE.get());BlockPos a=MATRIX.offset(4,-2,0),b=MATRIX.offset(-4,-2,0);
        helper.setBlock(a,Blocks.SKELETON_SKULL);matrix.scanSurroundings();close(helper,matrix.stabilityReplenish(),-.1f,"Unpaired skull penalty");
        helper.setBlock(b,Blocks.ZOMBIE_HEAD);matrix.scanSurroundings();close(helper,matrix.stabilityReplenish(),.1f,"Legacy skull variants should match");
        helper.setBlock(a.north(),Blocks.SKELETON_SKULL);helper.setBlock(b.south(),Blocks.ZOMBIE_HEAD);matrix.scanSurroundings();close(helper,matrix.stabilityReplenish(),.175f,"Second skull pair must contribute .075");
        var pedestal=pedestal(helper,MATRIX.offset(3,-2,0),StructureContent.PEDESTAL_ARCANE.get());pedestal(helper,MATRIX.offset(-3,-2,0),StructureContent.PEDESTAL_ARCANE.get());pedestal.setItem(0,new ItemStack(Items.DIAMOND));matrix.scanSurroundings();close(helper,matrix.stabilityReplenish(),.075f,"Occupied pedestal mismatch penalty");
        helper.assertTrue(!matrix.problemBlocks.isEmpty(),"Unbalanced stand was not identified for sparks");helper.succeed();
    }
    @GameTest(template=TEMPLATE,batch="infusion_matrix")
    public static void pillarMaterialAndFourBoostersHaveOriginalCostAndSpeed(GameTestHelper helper){
        var matrix=altar(helper,StructureContent.PILLAR_ANCIENT.get());matrix.scanSurroundings();helper.assertValueEqual(matrix.cycleTime(),9,"Ancient pillar speed");close(helper,matrix.costMultiplier(),.9f,"Ancient pillar cost");close(helper,matrix.stabilityReplenish(),-.1f,"Ancient pillar stability");
        altar(helper,StructureContent.PILLAR_ELDRITCH.get());matrix=matrix(helper);matrix.scanSurroundings();helper.assertValueEqual(matrix.cycleTime(),7,"Eldritch pillar speed");close(helper,matrix.costMultiplier(),1.05f,"Eldritch pillar cost");close(helper,matrix.stabilityReplenish(),.2f,"Eldritch pillar stability");
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})helper.setBlock(MATRIX.offset(x,-3,z),StructureContent.MATRIX_SPEED.get());matrix.scanSurroundings();helper.assertValueEqual(matrix.cycleTime(),3,"Four speed boosters");close(helper,matrix.costMultiplier(),1.09f,"Four speed booster cost");
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})helper.setBlock(MATRIX.offset(x,-3,z),StructureContent.MATRIX_COST.get());matrix.scanSurroundings();helper.assertValueEqual(matrix.cycleTime(),11,"Four cost boosters");close(helper,matrix.costMultiplier(),.97f,"Four cost booster discount");helper.succeed();
    }
    @GameTest(template=TEMPLATE,batch="infusion_matrix")
    public static void inlayPoweredStabilizerProtectsItemAndSpendsFiveToTenEnergy(GameTestHelper helper){
        var matrix=altar(helper,StructureContent.PILLAR_ARCANE.get());BlockPos position=MATRIX.offset(3,-2,0);var pedestal=pedestal(helper,position,StructureContent.PEDESTAL_ARCANE.get());pedestal.setItem(0,new ItemStack(Items.DIAMOND));
        helper.setBlock(position.east(),StructureContent.STABILIZER.get());var stabilizer=(StabilizerEntity)helper.getLevel().getBlockEntity(helper.absolutePos(position.east()));var tag=new CompoundTag();tag.putInt("Energy",15);stabilizer.loadWithComponents(tag,helper.getLevel().registryAccess());InlayNetwork.update(helper.getLevel(),stabilizer.getBlockPos());matrix.scanSurroundings();
        helper.assertTrue(pedestal.findInstabilityMitigator()!=null,"Powered neighboring stabilizer not reachable");matrix.ejectItem(4);
        helper.assertTrue(pedestal.getItem(0).is(Items.DIAMOND),"Stabilizer failed to protect item destruction");helper.assertTrue(stabilizer.getEnergy()>=5&&stabilizer.getEnergy()<=10,"Mitigation did not consume exactly 5..10 energy");helper.succeed();
    }
    @GameTest(template=TEMPLATE,batch="infusion_matrix")
    public static void ejectionDropsItemAndFluxMishapUsesActualGooAndAura(GameTestHelper helper){
        var matrix=altar(helper,StructureContent.PILLAR_ARCANE.get());BlockPos position=MATRIX.offset(3,-2,0);var pedestal=pedestal(helper,position,StructureContent.PEDESTAL_ARCANE.get());pedestal.setItem(0,new ItemStack(Items.DIAMOND));matrix.scanSurroundings();matrix.ejectItem(1);
        helper.assertTrue(pedestal.isEmpty(),"Ejection did not empty pedestal");helper.assertTrue(helper.getLevel().getBlockState(pedestal.getBlockPos().above()).is(WorldContent.block("flux_goo")),"Flux goo mishap produced no goo");
        helper.assertValueEqual(helper.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pedestal.getBlockPos()).inflate(2)).stream().filter(e->e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum(),1,"Ejection lost or duplicated ingredient");
        pedestal.setItem(0,new ItemStack(Items.EMERALD));float before=AuraSystem.get(helper.getLevel(),pedestal.getBlockPos()).flux();matrix.ejectItem(4);float delta=AuraSystem.get(helper.getLevel(),pedestal.getBlockPos()).flux()-before;
        helper.assertTrue(pedestal.isEmpty()&&delta>=5&&delta<=9,"Destruction mishap did not pollute 5..9 flux");helper.succeed();
    }
    @GameTest(template=TEMPLATE,batch="infusion_matrix")
    public static void matrixReadoutReportsRemainingCostAndExactStabilityBands(GameTestHelper helper){
        var ritual=ritual(helper,"runic_armor",new ItemStack(Items.IRON_BOOTS),true,true);ritual.matrix.activateOrStart(ritual.player);
        helper.assertValueEqual(RevealingReadout.aspects(ritual.matrix),ritual.matrix.remainingEssentia(),"Goggles do not show ritual demand");
        float[] values={13,12.5f,12,0,-.1f,-24.99f,-25};String[] labels={"VERY_STABLE","VERY_STABLE","STABLE","STABLE","UNSTABLE","UNSTABLE","VERY_UNSTABLE"};
        for(int i=0;i<values.length;i++){ritual.matrix.stability=values[i];helper.assertValueEqual(ritual.matrix.stabilityName(),labels[i],"Wrong original stability threshold "+values[i]);}
        helper.succeed();
    }
    @GameTest(template=TEMPLATE,batch="infusion_matrix")
    public static void largeRunicCostsCannotOverflowAndSkipEssentiaPhase(GameTestHelper helper){
        var ritual=ritual(helper,"runic_armor",new ItemStack(Items.IRON_BOOTS),true,true);ritual.matrix.activateOrStart(ritual.player);
        var saved=ritual.matrix.saveWithoutMetadata(helper.getLevel().registryAccess());var costs=saved.getCompound("Essentia");for(String key:costs.getAllKeys())costs.putInt(key,Integer.MAX_VALUE);
        ritual.matrix.loadWithComponents(saved,helper.getLevel().registryAccess());ritual.matrix.scanSurroundings();ritual.matrix.craftCycle();
        helper.assertTrue(ritual.matrix.crafting&&ritual.pedestals.stream().noneMatch(PedestalEntity::isEmpty),"Overflow skipped the essentia phase");
        helper.assertValueEqual(ritual.matrix.remainingEssentia().values().stream().filter(n->n==Integer.MAX_VALUE-1).count(),1L,"Large costs did not consume exactly one essentia");helper.succeed();
    }
    @GameTest(template=TEMPLATE,batch="infusion_matrix")
    public static void remoteMatrixRangeIncludesAllDirectionsAndExcludesUpperTwelfthLayer(GameTestHelper helper){
        BlockPos origin=helper.absolutePos(MATRIX);
        helper.assertTrue(MatrixSources.inRange(origin,origin.offset(-12,-12,-12)),"Negative corner excluded");helper.assertTrue(MatrixSources.inRange(origin,origin.offset(12,11,12)),"Positive corner excluded");
        helper.assertFalse(MatrixSources.inRange(origin,origin.above(12)),"Original upper range is exclusive");helper.assertFalse(MatrixSources.inRange(origin,origin.east(13)),"Outside source accepted");helper.assertFalse(MatrixSources.inRange(origin,origin),"Matrix must not draw from itself");
        var jar=jar(helper,MATRIX.offset(-10,-4,10),Aspect.MAGIC,17);var target=new MatrixSources().providing(helper.getLevel(),origin,Aspect.MAGIC);
        helper.assertTrue(target!=null&&target.source()==jar,"Matrix did not discover rear/below remote jar");helper.succeed();
    }
    private static Ritual ritual(GameTestHelper helper,String id,ItemStack input,boolean known,boolean supply){
        var matrix=altar(helper,StructureContent.PILLAR_ARCANE.get());var recipe=recipe(helper,id);var player=GameTestPlayers.create(helper);
        if(known)ResearchStorage.get(helper.getLevel().getServer()).player(player.getUUID()).stage(recipe.research().split("@")[0],256,false);
        player.setPos(matrix.getBlockPos().getX()+.5,matrix.getBlockPos().getY()+1,matrix.getBlockPos().getZ()+.5);
        var center=(PedestalEntity)helper.getLevel().getBlockEntity(helper.absolutePos(MATRIX.below(2)));center.setItem(0,input.copy());
        List<PedestalEntity> pedestals=new ArrayList<>();int index=0;
        for(var component:recipe.components(input)){int[] offset=COMPONENTS[index++];var pedestal=pedestal(helper,MATRIX.offset(offset[0],-2,offset[1]),StructureContent.PEDESTAL_ARCANE.get());pedestal.setItem(0,component.getItems()[0].copyWithCount(1));pedestals.add(pedestal);}
        int color=0;for(var candle:AlchemyMaterials.CANDLES.values()){int z=color++-8;for(int sign:new int[]{-1,1}){BlockPos p=MATRIX.offset(sign*7,-2,sign*z);helper.setBlock(p.below(),Blocks.STONE);helper.setBlock(p,candle.get());}}
        List<JarEntity> jars=new ArrayList<>();index=0;if(supply)for(Aspect aspect:recipe.aspects(player,input,pedestals.stream().map(p->p.getItem(0)).toList()).keySet())jars.add(jar(helper,MATRIX.offset(10,-2,index++-3),aspect,240));
        helper.assertTrue(matrix.activateOrStart(player),"Fixture activation failed");matrix.stability=25;return new Ritual(matrix,center,player,recipe,pedestals,jars);
    }
    private static void clearContents(GameTestHelper helper,Ritual ritual){for(var pedestal:ritual.pedestals)helper.getLevel().removeBlock(pedestal.getBlockPos(),false);for(var jar:ritual.jars)helper.getLevel().removeBlock(jar.getBlockPos(),false);helper.getLevel().removeBlock(ritual.matrix.getBlockPos(),false);}
    private static void finish(GameTestHelper helper,InfusionMatrixEntity matrix,int limit){for(int n=0;matrix.crafting&&n<limit;n++)matrix.craftCycle();helper.assertFalse(matrix.crafting,"Ritual did not complete within "+limit+" craft cycles");}
    private static InfusionMatrixEntity altar(GameTestHelper helper,Block pillar){
        if(!(helper.getLevel().getBlockEntity(helper.absolutePos(MATRIX)) instanceof InfusionMatrixEntity))helper.setBlock(MATRIX,MatrixContent.MATRIX.get());
        helper.setBlock(MATRIX.below(2),StructureContent.PEDESTAL_ARCANE.get());for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})helper.setBlock(MATRIX.offset(x,-2,z),pillar);return matrix(helper);
    }
    private static InfusionMatrixEntity matrix(GameTestHelper helper){return (InfusionMatrixEntity)helper.getLevel().getBlockEntity(helper.absolutePos(MATRIX));}
    private static PedestalEntity pedestal(GameTestHelper helper,BlockPos pos,Block block){helper.setBlock(pos,block);return (PedestalEntity)helper.getLevel().getBlockEntity(helper.absolutePos(pos));}
    private static JarEntity jar(GameTestHelper helper,BlockPos pos,Aspect aspect,int amount){helper.setBlock(pos,EssentiaContent.JAR.get());var jar=(JarEntity)helper.getLevel().getBlockEntity(helper.absolutePos(pos));jar.fill(aspect,amount,false);return jar;}
    private static InfusionRecipe recipe(GameTestHelper helper,String id){return (InfusionRecipe)helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse("thaumcraft:"+id)).orElseThrow().value();}
    private static Item item(String id){return BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:"+id));}
    private static void close(GameTestHelper helper,float actual,float expected,String label){helper.assertTrue(Math.abs(actual-expected)<.0001,label+": expected "+expected+", actual "+actual);}
    private InfusionMatrixGameTests(){}
}
