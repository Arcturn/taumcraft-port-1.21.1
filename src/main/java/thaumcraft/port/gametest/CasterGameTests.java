package thaumcraft.port.gametest;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import thaumcraft.port.auromancy.focus.*;
import thaumcraft.port.auromancy.focus.medium.FocusMediumTouch;
import thaumcraft.port.auromancy.focus.effect.FocusEffectAir;
import thaumcraft.port.auromancy.item.*;
import thaumcraft.port.content.*;
import thaumcraft.port.world.AuraSystem;

@GameTestHolder("thaumcraft")
@PrefixGameTestTemplate(false)
public final class CasterGameTests {
    private static ItemStack item(String id) { return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("thaumcraft:" + id))); }
    private static ItemStack focus(int complexity, String name) {
        var focus = item("focus_1"); var pack = new FocusPackage(); pack.addNode(new FocusMediumRoot());
        pack.addNode(new FocusMediumTouch()); pack.addNode(new FocusEffectAir()); pack.setComplexity(complexity);
        FocusItem.setPackage(focus, pack); focus.set(DataComponents.CUSTOM_NAME, Component.literal(name)); return focus;
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void capacitiesCostsAndPouchPersistThroughSave(GameTestHelper helper) {
        for (int tier = 1; tier <= 3; tier++) helper.assertValueEqual(((FocusItem)item("focus_" + tier).getItem()).getMaxComplexity(), new int[]{15,25,50}[tier-1], "Focus capacity differs from BETA26");
        var pouch = item("focus_pouch"); var contents = NonNullList.withSize(18, ItemStack.EMPTY); contents.set(17, focus(15, "Last slot"));
        FocusPouchItem.setInventory(pouch, contents);
        var loaded = ItemStack.parseOptional(helper.getLevel().registryAccess(), (CompoundTag)pouch.save(helper.getLevel().registryAccess()));
        var saved = FocusPouchItem.getInventory(loaded).get(17); var pack = FocusItem.getPackage(saved);
        helper.assertTrue(pack != null && pack.getComplexity() == 15, "Pouch last slot lost its configured focus during save/load");
        helper.assertValueEqual(pack.getVisCost(), 3f, "Focus vis cost must be complexity/5");
        helper.assertValueEqual(pack.getActivationTime(), 9, "Focus cooldown must retain integer division formula");
        helper.assertValueEqual(saved.getHoverName().getString(), "Last slot", "Named focus lost its components"); helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void offhandSwapReturnsOldFocusWithoutTouchingMainhand(GameTestHelper helper) {
        var player = GameTestPlayers.create(helper); var main = item("caster_basic"); var off = item("caster_basic");
        var old = focus(5, "Old off-hand"); var replacement = focus(10, "Replacement"); var untouched = focus(7, "Main-hand");
        CasterItem.setFocus(main, untouched); CasterItem.setFocus(off, old);
        player.setItemInHand(InteractionHand.MAIN_HAND, main); player.setItemInHand(InteractionHand.OFF_HAND, off);
        var pouch = item("focus_pouch"); var contents = NonNullList.withSize(18, ItemStack.EMPTY); contents.set(17, replacement); FocusPouchItem.setInventory(pouch, contents); player.getInventory().setItem(1, pouch);
        String key = ((FocusItem)replacement.getItem()).getSortingHelper(replacement);
        helper.assertTrue(CasterManager.changeFocus(player, InteractionHand.OFF_HAND, key), "Off-hand focus swap failed");
        helper.assertTrue(ItemStack.isSameItemSameComponents(CasterItem.getFocusStack(main), untouched), "Off-hand swap modified main-hand caster");
        helper.assertTrue(ItemStack.isSameItemSameComponents(CasterItem.getFocusStack(off), replacement), "Selected focus was not equipped in off hand");
        helper.assertTrue(FocusPouchItem.getInventory(pouch).stream().anyMatch(s -> ItemStack.isSameItemSameComponents(s, old)), "Previous focus was lost instead of returned to pouch");
        helper.assertTrue(CasterManager.changeFocus(player, InteractionHand.OFF_HAND, "REMOVE"), "Off-hand focus remove failed");
        helper.assertTrue(CasterItem.getFocusStack(off).isEmpty(), "Removed focus remained equipped"); helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void fullInventoryRejectsRemovalAndSwapUsesVacatedSlot(GameTestHelper helper) {
        var player = GameTestPlayers.create(helper); var caster = item("caster_basic"); var old = focus(5, "Old"); CasterItem.setFocus(caster, old);
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE,64));
        player.setItemInHand(InteractionHand.OFF_HAND, caster);
        helper.assertFalse(CasterManager.changeFocus(player, InteractionHand.OFF_HAND, "REMOVE"), "Full inventory must reject focus removal");
        helper.assertTrue(ItemStack.isSameItemSameComponents(CasterItem.getFocusStack(caster), old), "Rejected removal lost the equipped focus");
        var next = focus(10, "Next"); player.getInventory().setItem(35, next);
        helper.assertTrue(CasterManager.changeFocus(player, InteractionHand.OFF_HAND, ((FocusItem)next.getItem()).getSortingHelper(next)), "Swap must use vacated focus slot when full");
        helper.assertTrue(ItemStack.isSameItemSameComponents(player.getInventory().getItem(35), old), "Full-inventory swap lost or duplicated old focus"); helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void castingUsesOffhandCostAndSharedCooldownWithoutConsumingFocus(GameTestHelper helper) {
        var player = GameTestPlayers.create(helper); var main = item("caster_basic"); var off = item("caster_basic");
        CasterItem.setFocus(main, focus(15,"Main")); CasterItem.setFocus(off, focus(10,"Off"));
        player.setItemInHand(InteractionHand.MAIN_HAND, main); player.setItemInHand(InteractionHand.OFF_HAND, off);
        var aura = AuraSystem.get(helper.getLevel(),player.blockPosition()); aura.addVis(100); float before = aura.vis();
        var offFocus = CasterItem.getFocusStack(off); var cast = (CasterItem)off.getItem();
        helper.assertTrue(cast.use(helper.getLevel(),player,InteractionHand.OFF_HAND).getResult().consumesAction(), "Off-hand focus failed to cast");
        helper.assertTrue(Math.abs(aura.vis() - (before-2)) < .0001, "Off-hand cast paid main-hand focus cost or paid twice");
        helper.assertTrue(ItemStack.isSameItemSameComponents(CasterItem.getFocusStack(off),offFocus), "Casting consumed or changed its equipped focus");
        ((CasterItem)main.getItem()).use(helper.getLevel(),player,InteractionHand.MAIN_HAND);
        helper.assertTrue(Math.abs(aura.vis() - (before-2)) < .0001, "Switching hands bypassed shared cooldown");
        CasterManager.setCooldown(player,0); aura.drain(false,aura.vis(),false);
        helper.assertFalse(cast.consumeVis(off,player,2,false,false), "Insufficient aura was accepted");
        helper.assertValueEqual(aura.vis(),0f,"Rejected payment altered aura"); helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void predictedClientCastCannotPutEqualServerPlayerOnCooldown(GameTestHelper helper) {
        var serverPlayer = GameTestPlayers.create(helper);
        var clientLevel = new CooldownClientLevel(helper.getLevel());
        net.minecraft.world.entity.player.Player clientPlayer = new net.minecraft.world.entity.player.Player(clientLevel, serverPlayer.blockPosition(), 0, serverPlayer.getGameProfile()) {
            @Override public boolean isSpectator() { return false; }
            @Override public boolean isCreative() { return false; }
        };
        clientPlayer.setId(serverPlayer.getId());
        helper.assertTrue(clientPlayer != serverPlayer && clientPlayer.equals(serverPlayer)
                && clientPlayer.hashCode() == serverPlayer.hashCode(), "Fixture must reproduce Entity equality across logical sides");
        helper.assertTrue(clientPlayer.level().isClientSide && !serverPlayer.level().isClientSide, "Fixture must use different logical sides");
        var caster = item("caster_basic"); CasterItem.setFocus(caster, focus(10, "Side isolation"));
        clientPlayer.setItemInHand(InteractionHand.MAIN_HAND, caster.copy());
        serverPlayer.setItemInHand(InteractionHand.MAIN_HAND, caster);
        var aura = AuraSystem.get(helper.getLevel(), serverPlayer.blockPosition()); aura.addVis(100); float before = aura.vis();
        var item = (CasterItem)caster.getItem();
        try {
            helper.assertTrue(item.use(clientLevel, clientPlayer, InteractionHand.MAIN_HAND).getResult().consumesAction(), "Client prediction failed");
            helper.assertTrue(CasterManager.isOnCooldown(clientPlayer), "Prediction must still show its client cooldown");
            // Keep the regression independent of wall-clock delays in a busy test server.
            CasterManager.setCooldown(clientPlayer, 1200);
            helper.assertFalse(CasterManager.isOnCooldown(serverPlayer), "Client prediction blocked the equal-id server player");
            helper.assertTrue(item.use(helper.getLevel(), serverPlayer, InteractionHand.MAIN_HAND).getResult().consumesAction(), "Server rejected the predicted cast");
            helper.assertTrue(Math.abs(aura.vis() - (before - 2)) < .0001, "The server must execute and pay for exactly one cast");
            helper.assertTrue(CasterManager.isOnCooldown(serverPlayer), "Server cast must retain its own cooldown");
            CasterManager.setCooldown(serverPlayer, 1200);
            CasterManager.setCooldown(clientPlayer, 0);
            helper.assertTrue(CasterManager.isOnCooldown(serverPlayer), "Clearing client cooldown erased the server cooldown");
            CasterManager.setCooldown(clientPlayer, 1200);
            CasterManager.setCooldown(serverPlayer, 0);
            helper.assertTrue(CasterManager.isOnCooldown(clientPlayer), "Clearing server cooldown erased the client cooldown");
        } finally {
            CasterManager.setCooldown(clientPlayer, 0); CasterManager.setCooldown(serverPlayer, 0);
        }
        helper.succeed();
    }
    @GameTest(template="elemental_tools_empty",timeoutTicks=40)
    public static void anatomicalMappingAndOffhandPouchLock(GameTestHelper helper) {
        helper.assertValueEqual(CasterHand.arm(HumanoidArm.LEFT, InteractionHand.MAIN_HAND), HumanoidArm.LEFT, "Left-primary main hand rendered on right");
        helper.assertValueEqual(CasterHand.arm(HumanoidArm.LEFT, InteractionHand.OFF_HAND), HumanoidArm.RIGHT, "Left-primary off hand rendered on left");
        helper.assertValueEqual(CasterHand.arm(HumanoidArm.RIGHT, InteractionHand.OFF_HAND), HumanoidArm.LEFT, "Right-primary off hand rendered on right");
        helper.assertValueEqual(CasterHand.armWidth(true),3f,"Slim gauntlet must use three-pixel arm width");
        helper.assertValueEqual(CasterHand.armCenter(true,HumanoidArm.LEFT),.5f,"Slim left arm center is wrong");
        var player=GameTestPlayers.create(helper); var pouch=item("focus_pouch");player.setItemInHand(InteractionHand.OFF_HAND,pouch);
        var menu=new FocusPouchMenu(1,player.getInventory(),InteractionHand.OFF_HAND);
        helper.assertTrue(menu.stillValid(player),"Off-hand pouch cannot open");
        helper.assertValueEqual(menu.lockedSlot(),40,"Off-hand pouch must lock vanilla inventory slot 40");
        player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
        helper.assertFalse(menu.stillValid(player),"Menu remained active after its off-hand pouch was removed");helper.succeed();
    }

    /** Logical client world usable on a dedicated GameTest server, without loading client classes. */
    private static final class CooldownClientLevel extends net.minecraft.world.level.Level {
        private final net.minecraft.server.level.ServerLevel source;
        CooldownClientLevel(net.minecraft.server.level.ServerLevel source) {
            super((net.minecraft.world.level.storage.WritableLevelData)source.getLevelData(), source.dimension(), source.registryAccess(),
                    source.dimensionTypeRegistration(), source::getProfiler, true, false, 0, 0);
            this.source = source;
        }
        @Override public net.minecraft.world.level.chunk.ChunkSource getChunkSource() { return source.getChunkSource(); }
        @Override public net.minecraft.world.ticks.LevelTickAccess<net.minecraft.world.level.block.Block> getBlockTicks() { return source.getBlockTicks(); }
        @Override public net.minecraft.world.ticks.LevelTickAccess<net.minecraft.world.level.material.Fluid> getFluidTicks() { return source.getFluidTicks(); }
        @Override public net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> getUncachedNoiseBiome(int x,int y,int z) { return source.getUncachedNoiseBiome(x,y,z); }
        @Override public net.minecraft.world.flag.FeatureFlagSet enabledFeatures() { return source.enabledFeatures(); }
        @Override public float getShade(net.minecraft.core.Direction direction,boolean shade) { return source.getShade(direction,shade); }
        @Override public java.util.List<? extends net.minecraft.world.entity.player.Player> players() { return java.util.List.of(); }
        @Override public net.minecraft.world.TickRateManager tickRateManager() { return source.tickRateManager(); }
        @Override public net.minecraft.world.entity.Entity getEntity(int id) { return null; }
        @Override protected net.minecraft.world.level.entity.LevelEntityGetter<net.minecraft.world.entity.Entity> getEntities() { throw new UnsupportedOperationException("No client entity lookup in cooldown fixture"); }
        @Override public net.minecraft.world.level.saveddata.maps.MapItemSavedData getMapData(net.minecraft.world.level.saveddata.maps.MapId id) { return null; }
        @Override public void setMapData(net.minecraft.world.level.saveddata.maps.MapId id,net.minecraft.world.level.saveddata.maps.MapItemSavedData data) { throw new UnsupportedOperationException(); }
        @Override public net.minecraft.world.level.saveddata.maps.MapId getFreeMapId() { throw new UnsupportedOperationException(); }
        @Override public net.minecraft.world.scores.Scoreboard getScoreboard() { return source.getScoreboard(); }
        @Override public net.minecraft.world.item.crafting.RecipeManager getRecipeManager() { return source.getRecipeManager(); }
        @Override public net.minecraft.world.item.alchemy.PotionBrewing potionBrewing() { return source.potionBrewing(); }
        @Override public String gatherChunkSourceStats() { return "Cooldown client fixture"; }
        @Override public float getDayTimeFraction() { return source.getDayTimeFraction(); }
        @Override public float getDayTimePerTick() { return source.getDayTimePerTick(); }
        @Override public void setDayTimeFraction(float value) { }
        @Override public void setDayTimePerTick(float value) { }
        @Override public void sendBlockUpdated(net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState before,net.minecraft.world.level.block.state.BlockState after,int flags) { }
        @Override public void destroyBlockProgress(int id,net.minecraft.core.BlockPos pos,int progress) { }
        @Override public void levelEvent(net.minecraft.world.entity.player.Player player,int event,net.minecraft.core.BlockPos pos,int data) { }
        @Override public void gameEvent(net.minecraft.core.Holder<net.minecraft.world.level.gameevent.GameEvent> event,net.minecraft.world.phys.Vec3 pos,net.minecraft.world.level.gameevent.GameEvent.Context context) { }
        @Override public void playSeededSound(net.minecraft.world.entity.player.Player player,double x,double y,double z,net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound,net.minecraft.sounds.SoundSource category,float volume,float pitch,long seed) { }
        @Override public void playSeededSound(net.minecraft.world.entity.player.Player player,net.minecraft.world.entity.Entity entity,net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> sound,net.minecraft.sounds.SoundSource category,float volume,float pitch,long seed) { }
    }
}
