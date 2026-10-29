package thaumcraft.port.verification;

import com.google.gson.Gson;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;
import thaumcraft.port.aspect.RecipeAspects;
import thaumcraft.port.client.AspectTooltip;
import thaumcraft.port.client.ClientJournal;
import thaumcraft.port.config.ClientVisualConfig;
import thaumcraft.port.content.PortContent;

/** Automation for a disposable reference world; enabled only by an explicit test JVM property. */
final class ShiftTooltipRuntime {
    private record Case(String name, boolean chest, Item item, boolean shift, int scale) {}
    private final List<Case> cases = List.of(
            new Case("inventory_stone_no_shift", false, Items.STONE, false, 2),
            new Case("inventory_stone_shift_unscanned", false, Items.STONE, true, 2),
            new Case("inventory_diamond_shift_unscanned", false, Items.DIAMOND, true, 2),
            new Case("inventory_copper_shift_unscanned", false, Items.COPPER_INGOT, true, 2),
            new Case("inventory_breeze_shift_unscanned", false, Items.BREEZE_ROD, true, 2),
            new Case("inventory_copper_no_shift", false, Items.COPPER_INGOT, false, 2),
            new Case("chest_stone_shift_unscanned", true, Items.STONE, true, 2),
            new Case("chest_diamond_shift_unscanned", true, Items.DIAMOND, true, 2),
            new Case("chest_copper_shift_unscanned", true, Items.COPPER_INGOT, true, 2),
            new Case("chest_breeze_shift_unscanned", true, Items.BREEZE_ROD, true, 2),
            new Case("chest_breeze_no_shift", true, Items.BREEZE_ROD, false, 2),
            new Case("inventory_copper_shift_gui_scale_1", false, Items.COPPER_INGOT, true, 1));
    private final Path output = Path.of(System.getProperty("thaumcraft.probe.output", "tooltip-probe"));
    private final List<Object> results = new ArrayList<>();
    private final Gson gson = new Gson();
    private boolean worldRequested, setupRequested, caseStarted, finished;
    private volatile boolean prepared;
    private BlockPos chestPos;
    private int tick, nextTick, index, lastGatherTick = -100;
    private boolean validGather;
    private LinkedHashMap<String, Object> current;

    ShiftTooltipRuntime() { NeoForge.EVENT_BUS.register(this); }

    @SubscribeEvent
    public void tick(ClientTickEvent.Post event) {
        if (finished) return;
        tick++;
        var minecraft = Minecraft.getInstance();
        try {
            if (!worldRequested && minecraft.screen instanceof TitleScreen && minecraft.getOverlay() == null) {
                worldRequested = true;
                var rules = new GameRules();
                rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
                rules.getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null);
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
                var settings = new LevelSettings("Thaumometer parity", GameType.SURVIVAL, false,
                        Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT);
                minecraft.createWorldOpenFlows().createFreshLevel("thaumometer-parity-" + System.currentTimeMillis(),
                        settings, new WorldOptions(917L, false, false), registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), minecraft.screen);
                return;
            }
            if (minecraft.level == null || minecraft.player == null || minecraft.getSingleplayerServer() == null) return;
            if (!setupRequested) {
                setupRequested = true;
                var id = minecraft.player.getUUID();
                var server = minecraft.getSingleplayerServer();
                server.execute(() -> {
                    var player = server.getPlayerList().getPlayer(id);
                    if (player == null) return;
                    player.setGameMode(GameType.SURVIVAL);
                    player.getInventory().clearContent();
                    player.getInventory().selected = 0;
                    var samples = List.of(Items.STONE, Items.DIAMOND, Items.COPPER_INGOT, Items.BREEZE_ROD);
                    for (int i = 0; i < samples.size(); i++) player.getInventory().setItem(9 + i, new ItemStack(samples.get(i), 1));
                    player.getInventory().setChanged();
                    player.inventoryMenu.broadcastChanges();
                    chestPos = player.blockPosition().offset(2, 0, 0);
                    player.serverLevel().setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
                    var chest = (ChestBlockEntity) player.serverLevel().getBlockEntity(chestPos);
                    for (int i = 0; i < samples.size(); i++) chest.setItem(i, new ItemStack(samples.get(i), 1));
                    chest.setChanged();
                    prepared = true;
                });
                nextTick = tick + 80;
                return;
            }
            if (!prepared || tick < nextTick) return;
            if (index >= cases.size()) { finish(); return; }
            var scenario = cases.get(index);
            if (!caseStarted) {
                minecraft.options.guiScale().set(scenario.scale());
                minecraft.resizeDisplay();
                ClientVisualConfig.SHOW_TAGS.set(false);
                if (scenario.chest() && !(minecraft.screen instanceof ContainerScreen)) {
                    minecraft.player.closeContainer();
                    var id = minecraft.player.getUUID();
                    var server = minecraft.getSingleplayerServer();
                    server.execute(() -> {
                        var player = server.getPlayerList().getPlayer(id);
                        player.openMenu((ChestBlockEntity) player.serverLevel().getBlockEntity(chestPos));
                    });
                    nextTick = tick + 10;
                    return;
                }
                if (!scenario.chest() && !(minecraft.screen instanceof InventoryScreen)) {
                    minecraft.player.closeContainer();
                    minecraft.setScreen(new InventoryScreen(minecraft.player));
                    nextTick = tick + 10;
                    return;
                }
                var screen = (AbstractContainerScreen<?>) minecraft.screen;
                var slot = screen.getMenu().slots.stream().filter(candidate -> candidate.getItem().is(scenario.item())).findFirst().orElseThrow();
                double x = (screen.getGuiLeft() + slot.x + 8d) * minecraft.getWindow().getScreenWidth() / minecraft.getWindow().getGuiScaledWidth();
                double y = (screen.getGuiTop() + slot.y + 8d) * minecraft.getWindow().getScreenHeight() / minecraft.getWindow().getGuiScaledHeight();
                GLFW.glfwFocusWindow(minecraft.getWindow().getWindow());
                GLFW.glfwSetCursorPos(minecraft.getWindow().getWindow(), x - 1, y - 1);
                GLFW.glfwSetCursorPos(minecraft.getWindow().getWindow(), x, y);
                shift(scenario.shift());
                current = new LinkedHashMap<>();
                current.put("case", scenario.name());
                current.put("item", BuiltInRegistries.ITEM.getKey(scenario.item()).toString());
                current.put("gui_scale", scenario.scale());
                current.put("mouse_pixels", List.of(x, y));
                current.put("slot_coordinates", List.of(slot.x, slot.y));
                validGather = false;
                caseStarted = true;
                nextTick = tick + 12;
            } else if (tick > nextTick + 100) {
                throw new IllegalStateException("No actual tooltip rendered for " + scenario.name() + " actual_mouse="
                        + minecraft.mouseHandler.xpos() + "," + minecraft.mouseHandler.ypos()
                        + " screen=" + minecraft.screen.getClass().getSimpleName() + " cursor=" + current);
            }
        } catch (Throwable failure) { fail(failure); }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void gathered(RenderTooltipEvent.GatherComponents event) {
        if (!caseStarted || finished || index >= cases.size()) return;
        var scenario = cases.get(index);
        if (!event.getItemStack().is(scenario.item())) return;
        var minecraft = Minecraft.getInstance();
        if (Screen.hasShiftDown() != scenario.shift()) return;
        var data = event.getTooltipElements().stream().map(part -> part.right().orElse(null))
                .filter(AspectTooltip.Data.class::isInstance).map(AspectTooltip.Data.class::cast).findFirst();
        var calculated = RecipeAspects.get(event.getItemStack(), minecraft.level);
        var expected = new LinkedHashMap<String, Integer>();
        calculated.forEach((aspect, amount) -> expected.put(aspect.id(), amount));
        var shown = new LinkedHashMap<String, Integer>();
        data.ifPresent(row -> row.entries().forEach(entry -> shown.put(entry.aspect().id(), entry.amount())));
        boolean noTool = !minecraft.player.getMainHandItem().is(PortContent.THAUMOMETER.get())
                && !minecraft.player.getOffhandItem().is(PortContent.THAUMOMETER.get());
        boolean unknown = ClientJournal.state.scanned().isEmpty() && ClientJournal.state.discovered().isEmpty();
        validGather = noTool && unknown && !calculated.isEmpty()
                && (scenario.shift() ? data.isPresent() && shown.equals(expected) : data.isEmpty());
        current.put("shift", Screen.hasShiftDown());
        current.put("no_held_thaumometer", noTool);
        current.put("scanned_subjects", ClientJournal.state.scanned().size());
        current.put("discovered_aspects", ClientJournal.state.discovered().size());
        current.put("calculated_aspects", expected);
        current.put("shown_aspects", shown);
        current.put("aspect_row_present", data.isPresent());
        current.put("pass", validGather);
        lastGatherTick = tick;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void rendered(ScreenEvent.Render.Post event) {
        if (finished || !caseStarted || tick < nextTick || tick - lastGatherTick > 1) return;
        try {
            var scenario = cases.get(index);
            if (!validGather) throw new IllegalStateException("Tooltip evidence failed: " + gson.toJson(current));
            event.getGuiGraphics().flush();
            Files.createDirectories(output);
            var path = output.resolve(String.format("%02d-%s.png", index + 1, scenario.name()));
            try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) { image.writeToFile(path); }
            current.put("screenshot", path.getFileName().toString());
            current.put("screen", event.getScreen().getClass().getSimpleName());
            results.add(current);
            System.out.println("SHIFT_ASPECTS_CASE " + gson.toJson(current));
            index++;
            caseStarted = false;
            nextTick = tick + 8;
        } catch (Throwable failure) { fail(failure); }
    }

    private void shift(boolean held) throws Exception {
        String executable = System.getProperty("thaumcraft.probe.xdotool", "xdotool");
        var process = new ProcessBuilder(executable, held ? "keydown" : "keyup", "Shift_L").inheritIO().start();
        if (process.waitFor() != 0) throw new IllegalStateException("xdotool failed to set Shift");
    }

    private void finish() throws Exception {
        finished = true;
        shift(false);
        Files.createDirectories(output);
        Files.writeString(output.resolve("shift-tooltip-runtime.json"), gson.toJson(results) + "\n");
        System.out.println("SHIFT_ASPECTS_COMPLETE cases=" + results.size() + " passed=" + results.size());
        if (Boolean.getBoolean("thaumcraft.probe.scanner")) {
            Class.forName("thaumcraft.port.verification.ThaumometerRuntimeProbe").getMethod("start").invoke(null);
        } else if (Boolean.parseBoolean(System.getProperty("thaumcraft.probe.exit", "true"))) Minecraft.getInstance().stop();
    }

    private void fail(Throwable failure) {
        finished = true;
        failure.printStackTrace();
        try {
            shift(false);
            Files.createDirectories(output);
            Files.writeString(output.resolve("failure.txt"), failure.toString() + "\n");
            try (var image = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
                image.writeToFile(output.resolve("failure.png"));
            }
        } catch (Exception ignored) {}
        System.out.println("SHIFT_ASPECTS_FAILED " + failure);
        Minecraft.getInstance().stop();
    }
}
