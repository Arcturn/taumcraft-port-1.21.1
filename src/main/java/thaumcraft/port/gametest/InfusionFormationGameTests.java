package thaumcraft.port.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.content.*;
import thaumcraft.port.infusion.matrix.*;
import thaumcraft.port.infusion.structure.*;
import thaumcraft.port.research.ResearchStorage;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class InfusionFormationGameTests {
    private static final BlockPos CENTER = new BlockPos(1, 1, 1), MATRIX = CENTER.above(2);
    @GameTest(template="empty",timeoutTicks=85)
    public static void arcaneAltarFormsWithDustAndActivatesWithCaster(GameTestHelper h) { formation(h,0); }
    @GameTest(template="empty",timeoutTicks=85)
    public static void ancientAltarFormsWithItsOwnResearch(GameTestHelper h) { formation(h,1); }
    @GameTest(template="empty",timeoutTicks=85)
    public static void eldritchAltarFormsWithItsOwnResearch(GameTestHelper h) { formation(h,2); }

    private static void formation(GameTestHelper h,int tier) {
        var p = player(h); ResearchStorage.get(p.getServer()).player(p.getUUID()).flag(research(tier));
        parts(h,tier); var matrix = (InfusionMatrixEntity)h.getBlockEntity(MATRIX);
        h.assertTrue(!matrix.validLocation(),"Unformed stones count as formed pillars");
        h.assertTrue(dust(h,p,CENTER.offset(-1,1,-1)).consumesAction(),"Dust did not recognize a corner of altar");
        h.assertValueEqual(p.getMainHandItem().getCount(),1,"Formation must cost one dust");
        h.assertTrue(!dust(h,p,MATRIX).consumesAction(),"Reserved blueprint accepted a second payment");
        var factory=new SavedData.Factory<>(SalisMundusRituals::new,SalisMundusRituals::load,null);
        var data=h.getLevel().getDataStorage().get(factory,"thaumcraft_salis_rituals_v1");
        var saved=data.save(new CompoundTag(),h.getLevel().registryAccess());
        h.assertValueEqual(SalisMundusRituals.load(saved,h.getLevel().registryAccess()).save(new CompoundTag(),h.getLevel().registryAccess()),saved,"Altar reservation/payment changed on reload");
        h.runAfterDelay(52,()->{
            for(int x:new int[]{-1,1})for(int z:new int[]{-1,1}){
                h.assertBlockPresent(pillar(tier),CENTER.offset(x,0,z));
                h.assertBlockPresent(Blocks.AIR,CENTER.offset(x,1,z));
                var expected=x<0?(z<0?Direction.NORTH:Direction.WEST):(z<0?Direction.EAST:Direction.SOUTH);
                h.assertValueEqual(h.getBlockState(CENTER.offset(x,0,z)).getValue(PillarBlock.FACING),expected,"Pillar does not face as in original TC6 altar");
            }
            h.assertTrue(matrix.validLocation(),"Dust did not form a valid altar");
            h.assertTrue(h.getBlockEntity(MATRIX)==matrix,"Formation replaced matrix and lost its data");
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PortContent.CASTER_BASIC.get()));
            var result=PortContent.CASTER_BASIC.get().onItemUseFirst(p.getMainHandItem(),context(h,p,MATRIX));
            h.assertTrue(result.consumesAction(),"Caster did not activate formed altar");
            h.assertTrue(matrix.active,"Caster left the altar inactive");
            h.succeed();
        });
    }
    @GameTest(template="empty",timeoutTicks=85)
    public static void unresearchedAndIncompleteAltarsDoNotConsumeDust(GameTestHelper h) {
        var p=player(h);parts(h,0);
        dust(h,p,MATRIX);
        h.assertValueEqual(p.getMainHandItem().getCount(),2,"Unresearched altar consumed dust");
        ResearchStorage.get(p.getServer()).player(p.getUUID()).flag("INFUSION");
        h.setBlock(CENTER.offset(1,1,1),Blocks.COBBLESTONE);
        dust(h,p,MATRIX);
        h.assertValueEqual(p.getMainHandItem().getCount(),2,"Wrong material consumed dust");
        h.runAfterDelay(52,()->{h.assertBlockPresent(stone(0),CENTER.offset(-1,0,-1));h.succeed();});
    }
    @GameTest(template="empty",timeoutTicks=85)
    public static void changedAltarRefundsWithoutReplacingOtherParts(GameTestHelper h) {
        var p=player(h);parts(h,0);ResearchStorage.get(p.getServer()).player(p.getUUID()).flag("INFUSION");
        dust(h,p,MATRIX);h.setBlock(CENTER.offset(1,1,1),Blocks.OBSIDIAN);
        h.runAfterDelay(52,()->{
            h.assertBlockPresent(stone(0),CENTER.offset(-1,0,-1));
            h.assertBlockPresent(Blocks.OBSIDIAN,CENTER.offset(1,1,1));
            h.assertValueEqual(p.getInventory().countItem(PortContent.SALIS_MUNDUS.get()),2,"Invalidated formation lost or duplicated dust");h.succeed();
        });
    }
    private static ServerPlayer player(GameTestHelper h){var p=GameTestPlayers.create(h);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(PortContent.SALIS_MUNDUS.get(),2));return p;}
    private static String research(int tier){return new String[]{"INFUSION","INFUSIONANCIENT","INFUSIONELDRITCH"}[tier];}
    private static Block stone(int tier){return tier==0?BuiltInRegistries.BLOCK.get(ResourceLocation.parse("thaumcraft:arcane_stone")):tier==1?StructureContent.STONE_ANCIENT.get():StructureContent.STONE_ELDRITCH_TILE.get();}
    private static Block pillar(int tier){return tier==0?StructureContent.PILLAR_ARCANE.get():tier==1?StructureContent.PILLAR_ANCIENT.get():StructureContent.PILLAR_ELDRITCH.get();}
    private static void parts(GameTestHelper h,int tier){
        h.setBlock(MATRIX,MatrixContent.MATRIX.get());h.setBlock(CENTER,tier==0?StructureContent.PEDESTAL_ARCANE.get():tier==1?StructureContent.PEDESTAL_ANCIENT.get():StructureContent.PEDESTAL_ELDRITCH.get());
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})for(int y=0;y<2;y++)h.setBlock(CENTER.offset(x,y,z),stone(tier));
    }
    private static UseOnContext context(GameTestHelper h,ServerPlayer p,BlockPos relative){var pos=h.absolutePos(relative);return new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}
    private static net.minecraft.world.InteractionResult dust(GameTestHelper h,ServerPlayer p,BlockPos pos){return PortContent.SALIS_MUNDUS.get().useOn(context(h,p,pos));}
}
