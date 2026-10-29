package thaumcraft.port.gametest;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.world.CrystalClusterBlock;
import thaumcraft.port.world.CrystalBiomeAffinity;
import thaumcraft.port.world.WorldContent;

/** Real tags, neighboring block faces, and loot tables for naturally generated crystals. */
@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class CrystalSupportGameTests {
    @GameTest(template="empty", timeoutTicks=40)
    public static void crystalsAttachToRockAndOreFacesIncludingDeepslate(GameTestHelper helper) {
        var level=helper.getLevel();
        var pos=helper.absolutePos(new BlockPos(1,1,1));
        var crystal=WorldContent.CRYSTALS.get(Aspect.AIR).get();
        var rocks=List.of("minecraft:stone","minecraft:deepslate","minecraft:tuff",
                "minecraft:cobblestone","minecraft:bedrock","minecraft:netherrack",
                "minecraft:blackstone","minecraft:basalt","minecraft:diamond_ore",
                "minecraft:deepslate_diamond_ore","thaumcraft:ore_amber","thaumcraft:ore_cinnabar",
                "thaumcraft:ore_quartz","thaumcraft:deepslate_ore_amber",
                "thaumcraft:deepslate_ore_cinnabar","thaumcraft:deepslate_ore_quartz");
        int checked=0;
        for(String id:rocks) {
            var support=BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
            helper.assertFalse(support==Blocks.AIR,"Missing rock support block: "+id);
            for(Direction face:Direction.values()) {
                for(Direction other:Direction.values()) level.setBlock(pos.relative(other),Blocks.AIR.defaultBlockState(),2);
                level.setBlock(pos.relative(face),support.defaultBlockState(),2);
                var state=crystal.attached(level,pos,crystal.defaultBlockState());
                helper.assertTrue(state.canSurvive(level,pos),"Crystal rejected rock support "+id+" on "+face);
                for(Direction other:Direction.values()) helper.assertValueEqual(state.getValue(CrystalClusterBlock.FACES.get(other)),
                        other==face,"Crystal attachment points at wrong face for "+id);
                checked++;
            }
        }
        for(Block unsuitable:List.of(Blocks.AIR,Blocks.DIRT,Blocks.OAK_PLANKS,Blocks.WATER)) {
            for(Direction face:Direction.values()) level.setBlock(pos.relative(face),unsuitable.defaultBlockState(),2);
            var state=crystal.attached(level,pos,crystal.defaultBlockState());
            helper.assertFalse(state.canSurvive(level,pos),"Crystal used a non-rock support: "+unsuitable);
        }
        // Partial masonry not listed as a support must not act like a solid rock block.
        for(Direction face:Direction.values()) level.setBlock(pos.relative(face),Blocks.AIR.defaultBlockState(),2);
        level.setBlock(pos.below(),Blocks.STONE_BRICK_STAIRS.defaultBlockState(),2);
        helper.assertFalse(crystal.attached(level,pos,crystal.defaultBlockState()).canSurvive(level,pos),
                "Crystal attached to a non-sturdy stair top");
        System.out.println("CRYSTAL_SUPPORT valid_rock_faces="+checked+" unsupported_materials=4");
        helper.succeed();
    }

    @GameTest(template="empty", timeoutTicks=40)
    public static void generatedSizeRemainsDistinctFromEssenceDropCount(GameTestHelper helper) {
        var level=helper.getLevel();
        var pos=helper.absolutePos(new BlockPos(1,1,1));
        for(Aspect aspect:List.of(Aspect.AIR,Aspect.FIRE,Aspect.WATER,Aspect.EARTH,Aspect.ORDER,Aspect.ENTROPY)) {
            var crystal=WorldContent.CRYSTALS.get(aspect).get();
            for(int size=0;size<=3;size++) {
                var state=crystal.defaultBlockState().setValue(CrystalClusterBlock.SIZE,size);
                helper.assertValueEqual(state.getValue(CrystalClusterBlock.GEN),1,"New crystal changed generation metadata");
                var drops=Block.getDrops(state,level,pos,null);
                helper.assertValueEqual(drops.stream().mapToInt(stack->stack.getCount()).sum(),size+1,
                        "Crystal loot must remain SIZE+1, independent of the worldgen SIZE budget");
                helper.assertTrue(drops.stream().allMatch(stack->CrystalItem.aspect(stack)==aspect),
                        "Crystal loot lost its primal aspect");
            }
        }
        helper.succeed();
    }

    @GameTest(template="empty", timeoutTicks=40)
    public static void loadedBiomeTagsRetainOverlappingTypesAndUnknownTypeFallback(GameTestHelper helper) {
        var biomes=helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        var taiga=biomes.getHolderOrThrow(Biomes.TAIGA);
        var mushrooms=biomes.getHolderOrThrow(Biomes.MUSHROOM_FIELDS);
        var random=RandomSource.create(0x6C0FFEE);
        var taigaAspects=java.util.EnumSet.noneOf(Aspect.class);
        boolean rareFallback=false,mushroomOrder=false;
        for(int i=0;i<1000;i++) {
            var aspect=CrystalBiomeAffinity.select(taiga,random);
            if(aspect!=null)taigaAspects.add(aspect);
            aspect=CrystalBiomeAffinity.select(mushrooms,random);
            rareFallback|=aspect==null;
            mushroomOrder|=aspect==Aspect.ORDER;
        }
        helper.assertTrue(taigaAspects.containsAll(List.of(Aspect.EARTH,Aspect.ORDER)),
                "Actual taiga tags lost their forest/coniferous or cold aspect");
        helper.assertTrue(rareFallback&&mushroomOrder,
                "Actual rare mushroom biome must retain both its ORDER bias and random-primal fallback");
        helper.succeed();
    }
}
