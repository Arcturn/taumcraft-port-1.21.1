package thaumcraft.port.gametest;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.aspect.AspectTags;
import thaumcraft.port.mob.MobContent;
import thaumcraft.port.research.ScanDiscoveries;
import thaumcraft.port.world.WorldConfig;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class MobIntegrationGameTests {
    @GameTest(template="elemental_tools_empty", timeoutTicks=40)
    public static void mobsUnlockOriginalScanDiscoveriesAndAspectObservations(GameTestHelper h) {
        verify(h, MobContent.BRAINY_ZOMBIE.get(), Map.of(Aspect.UNDEAD,20,Aspect.MAN,10,Aspect.MIND,5,Aspect.AVERSION,5), "!BrainyZombie", "f_BRAIN");
        verify(h, MobContent.GIANT_BRAINY_ZOMBIE.get(), Map.of(Aspect.UNDEAD,25,Aspect.MAN,15,Aspect.MIND,5,Aspect.AVERSION,10), "!BrainyZombie", "f_BRAIN");
        verify(h, MobContent.ELDRITCH_CRAB.get(), Map.of(Aspect.ELDRITCH,10,Aspect.BEAST,10,Aspect.TRAP,10), "!EldritchCrab", "!ORMOB");
        verify(h, MobContent.INHABITED_ZOMBIE.get(), Map.of(Aspect.ELDRITCH,10,Aspect.UNDEAD,10,Aspect.MAN,5), "!EldritchCrab", "!ORMOB");
        verify(h, MobContent.TAINTACLE.get(), Map.of(Aspect.FLUX,15,Aspect.BEAST,10), "!Taintacle");
        verify(h, MobContent.TAINTACLE_SMALL.get(), Map.of(Aspect.FLUX,5,Aspect.BEAST,5), "!Taintacle");
        verify(h, MobContent.SWARM.get(), Map.of(Aspect.FLUX,15,Aspect.AIR,5), "!TaintSwarm", "f_FLY");
        var crawler=MobContent.CRAWLER.get().create(h.getLevel());
        h.assertTrue(ScanDiscoveries.entityFlags(crawler).contains("!TaintCrawler"), "Crawler scan does not unlock its research");
        crawler.discard();
        h.succeed();
    }

    @GameTest(template="elemental_tools_empty", timeoutTicks=40)
    public static void classificationAndSpawnEggsResolveRegisteredMobTypes(GameTestHelper h) {
        for(var type:new EntityType<?>[]{MobContent.BRAINY_ZOMBIE.get(),MobContent.GIANT_BRAINY_ZOMBIE.get(),MobContent.INHABITED_ZOMBIE.get()})
            h.assertTrue(type.is(EntityTypeTags.UNDEAD), "Zombie missing undead classification: "+type);
        for(var type:new EntityType<?>[]{MobContent.ELDRITCH_CRAB.get(),MobContent.CRAWLER.get()})
            h.assertTrue(type.is(EntityTypeTags.ARTHROPOD), "Arthropod missing classification: "+type);
        Map<SpawnEggItem,EntityType<?>> eggs=Map.of(
                MobContent.BRAINY_ZOMBIE_EGG.get(),MobContent.BRAINY_ZOMBIE.get(),
                MobContent.GIANT_BRAINY_ZOMBIE_EGG.get(),MobContent.GIANT_BRAINY_ZOMBIE.get(),
                MobContent.ELDRITCH_CRAB_EGG.get(),MobContent.ELDRITCH_CRAB.get(),
                MobContent.INHABITED_ZOMBIE_EGG.get(),MobContent.INHABITED_ZOMBIE.get(),
                MobContent.CRAWLER_EGG.get(),MobContent.CRAWLER.get(),
                MobContent.SWARM_EGG.get(),MobContent.SWARM.get(),
                MobContent.TAINTACLE_EGG.get(),MobContent.TAINTACLE.get());
        eggs.forEach((egg,type)->h.assertTrue(egg.getType(egg.getDefaultInstance())==type,"Spawn egg resolves the wrong entity: "+type));
        h.succeed();
    }

    @GameTest(template="elemental_tools_empty", timeoutTicks=40)
    public static void brainyZombiesUseOriginalSpawnWeightAndDoNotPopulateMushroomIslands(GameTestHelper h) {
        var biomes=h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        var spawns=biomes.getOrThrow(Biomes.PLAINS).getMobSettings().getMobs(MobCategory.MONSTER).unwrap();
        var brainy=spawns.stream().filter(spawn->spawn.type==MobContent.BRAINY_ZOMBIE.get()).toList();
        h.assertValueEqual(brainy.size(),1,"Plains must contain one angry zombie spawn entry");
        h.assertValueEqual(brainy.getFirst().getWeight().asInt(),10,"Angry zombie spawn weight");
        h.assertTrue(brainy.getFirst().minCount==1&&brainy.getFirst().maxCount==1,"Angry zombies must spawn alone");
        h.assertFalse(biomes.getOrThrow(Biomes.MUSHROOM_FIELDS).getMobSettings().getMobs(MobCategory.MONSTER).unwrap().stream()
                .anyMatch(spawn->spawn.type==MobContent.BRAINY_ZOMBIE.get()),"Angry zombies invaded a monster-free biome");
        h.succeed();
    }

    @GameTest(template="elemental_tools_empty", timeoutTicks=100)
    public static void brainyZombieConfigDisablesAnOtherwiseValidNaturalSpawn(GameTestHelper h) {
        for(int x=0;x<7;x++)for(int y=1;y<6;y++)for(int z=0;z<7;z++)
            h.setBlock(new BlockPos(x,y,z),x==0||x==6||z==0||z==6||y==1||y==5?Blocks.STONE:Blocks.AIR);
        h.runAfterDelay(20,()->{
            var pos=h.absolutePos(new BlockPos(3,2,3));
            boolean previous=WorldConfig.BRAINY_ZOMBIES.get();
            try {
                WorldConfig.BRAINY_ZOMBIES.set(true);
                h.assertTrue(SpawnPlacements.checkSpawnRules(MobContent.BRAINY_ZOMBIE.get(),h.getLevel(),MobSpawnType.NATURAL,pos,RandomSource.create(7)),"Dark chamber did not allow the baseline spawn");
                WorldConfig.BRAINY_ZOMBIES.set(false);
                h.assertFalse(SpawnPlacements.checkSpawnRules(MobContent.BRAINY_ZOMBIE.get(),h.getLevel(),MobSpawnType.NATURAL,pos,RandomSource.create(7)),"Disabled angry zombies still spawn naturally");
                h.assertTrue(SpawnPlacements.checkSpawnRules(MobContent.BRAINY_ZOMBIE.get(),h.getLevel(),MobSpawnType.SPAWNER,pos,RandomSource.create(7)),"Natural-spawn setting disabled a monster spawner");
            } finally {WorldConfig.BRAINY_ZOMBIES.set(previous);}
            h.succeed();
        });
    }

    private static void verify(GameTestHelper h, EntityType<? extends Entity> type, Map<Aspect,Integer> aspects, String... discoveries) {
        var entity=type.create(h.getLevel());
        h.assertTrue(AspectTags.forEntity(entity).equals(aspects),"Incorrect scan aspects for "+type);
        var flags=ScanDiscoveries.entityFlags(entity);
        for(String flag:discoveries)h.assertTrue(flags.contains(flag),"Missing "+flag+" for "+type);
        entity.discard();
    }

    private MobIntegrationGameTests() {}
}
