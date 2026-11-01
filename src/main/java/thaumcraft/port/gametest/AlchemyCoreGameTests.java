package thaumcraft.port.gametest;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.machine.CrucibleEntity;
import thaumcraft.port.machine.MachineContent;
import thaumcraft.port.research.LegacyRequirement;
import thaumcraft.port.research.ResearchStorage;
import thaumcraft.port.world.AlumentumProjectile;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class AlchemyCoreGameTests {
    @GameTest(template="empty",timeoutTicks=80)
    public static void crucibleCraftsFireForgeEventAndRecordResearchExactlyOnce(GameTestHelper helper){
        var pos=helper.absolutePos(new BlockPos(2,3,2));
        helper.getLevel().setBlock(pos,MachineContent.CRUCIBLE.get().defaultBlockState(),3);
        var crucible=(CrucibleEntity)helper.getLevel().getBlockEntity(pos);
        var player=GameTestPlayers.create(helper);
        var state=ResearchStorage.get(player.getServer()).player(player.getUUID());state.stage("UNLOCKALCHEMY",3,false);
        crucible.state.restore(1000,200,-100,Map.of(Aspect.ENERGY,10,Aspect.FIRE,10,Aspect.LIGHT,10));
        var observer=new CraftedObserver(player);NeoForge.EVENT_BUS.register(observer);
        try{
            var catalyst=new ItemStack(Items.GLOWSTONE_DUST);
            helper.assertTrue(crucible.acceptItem(catalyst,player),"Known Nitor recipe was not crafted");
            helper.assertTrue(catalyst.isEmpty(),"Crafting did not consume catalyst");
            helper.assertValueEqual(observer.count,1,"Crucible failed to fire exactly one Forge crafting event");
            helper.assertTrue(observer.output.is(MachineContent.NITOR.get("nitor_yellow").get().asItem()),"Crafting event had the wrong output");
            helper.assertTrue(state.crafted().contains(LegacyRequirement.craftKey(observer.output)),"Crafted event did not record original research requirement");
        }finally{NeoForge.EVENT_BUS.unregister(observer);}
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=80)
    public static void thrownAlumentumConsumesOneAndExplodesOnBlockCollision(GameTestHelper helper){
        var level=helper.getLevel();var player=GameTestPlayers.create(helper);
        var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:alumentum"));
        var origin=helper.absolutePos(new BlockPos(1,3,1));player.setPos(origin.getX()+.5,origin.getY(),origin.getZ()+.5);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item,2));
        item.use(level,player,InteractionHand.MAIN_HAND);
        helper.assertValueEqual(player.getMainHandItem().getCount(),1,"Survival throw did not consume exactly one Alumentum");
        var projectiles=level.getEntitiesOfClass(AlumentumProjectile.class,new AABB(origin).inflate(3));
        helper.assertValueEqual(projectiles.size(),1,"Throw did not spawn exactly one registered projectile");
        var projectile=projectiles.getFirst();helper.assertTrue(projectile.getOwner()==player,"Projectile lost its thrower");
        var glass=origin.east(2);level.setBlock(glass,Blocks.GLASS.defaultBlockState(),3);
        projectile.setPos(glass.getX()-.5,glass.getY()+.5,glass.getZ()+.5);projectile.setDeltaMovement(.75,0,0);
        for(int tick=0;tick<4&&projectile.isAlive();tick++)projectile.tick();
        helper.assertFalse(projectile.isAlive(),"Alumentum passed through a solid block without exploding");
        helper.assertFalse(level.getBlockState(glass).is(Blocks.GLASS),"Original block-damaging explosion left its directly hit glass intact");
        helper.succeed();
    }

    @GameTest(template="empty",timeoutTicks=80)
    public static void creativeAlumentumThrowPreservesItem(GameTestHelper helper){
        var player=GameTestPlayers.create(helper);player.setGameMode(GameType.CREATIVE);
        var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:alumentum"));
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));
        item.use(helper.getLevel(),player,InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().is(item)&&player.getMainHandItem().getCount()==1,"Creative throw consumed Alumentum");
        var spawned=helper.getLevel().getEntitiesOfClass(AlumentumProjectile.class,player.getBoundingBox().inflate(3));
        helper.assertValueEqual(spawned.size(),1,"Creative throw failed to create its projectile");spawned.forEach(AlumentumProjectile::discard);
        helper.succeed();
    }

    public static final class CraftedObserver {
        private final ServerPlayer player;int count;ItemStack output=ItemStack.EMPTY;
        CraftedObserver(ServerPlayer player){this.player=player;}
        @SubscribeEvent public void crafted(PlayerEvent.ItemCraftedEvent event){if(event.getEntity()==player){count++;output=event.getCrafting().copy();}}
    }
    private AlchemyCoreGameTests(){}
}
