package thaumcraft.port.verification;

import com.google.gson.Gson;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import thaumcraft.port.aspect.RecipeAspects;
import thaumcraft.port.client.AspectTooltip;
import thaumcraft.port.client.ClientJournal;
import thaumcraft.port.config.ClientVisualConfig;
import thaumcraft.port.content.PortContent;

/** Isolated verification mod: put only in the disposable client test profile, never the release JAR. */
@Mod("thaumcraft_tooltip_probe")
public final class ShiftTooltipProbe {
    private final Set<String> seen = new HashSet<>();
    private final Gson gson = new Gson();

    public ShiftTooltipProbe() {
        NeoForge.EVENT_BUS.register(this);
        if (Boolean.getBoolean("thaumcraft.probe.automate")) new ShiftTooltipRuntime();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void gathered(RenderTooltipEvent.GatherComponents event) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || event.getItemStack().isEmpty()) return;
        var data = event.getTooltipElements().stream().map(part -> part.right().orElse(null))
                .filter(AspectTooltip.Data.class::isInstance).map(AspectTooltip.Data.class::cast).findFirst();
        var aspects = RecipeAspects.get(event.getItemStack(), minecraft.level);
        boolean shift = Screen.hasShiftDown();
        boolean held = minecraft.player.getMainHandItem().is(PortContent.THAUMOMETER.get())
                || minecraft.player.getOffhandItem().is(PortContent.THAUMOMETER.get());
        boolean expected = minecraft.screen instanceof AbstractContainerScreen<?>
                && !minecraft.mouseHandler.isMouseGrabbed()
                && shift != ClientVisualConfig.SHOW_TAGS.get() && !aspects.isEmpty();
        var report = new LinkedHashMap<String, Object>();
        report.put("phase", "gather");
        report.put("item", BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()).toString());
        report.put("screen", minecraft.screen == null ? "none" : minecraft.screen.getClass().getSimpleName());
        report.put("shift", shift);
        report.put("show_tags", ClientVisualConfig.SHOW_TAGS.get());
        report.put("scanned_subjects", ClientJournal.state.scanned().size());
        report.put("discovered_aspects", ClientJournal.state.discovered().size());
        report.put("held_thaumometer", held);
        report.put("aspect_row_present", data.isPresent());
        report.put("visibility_pass", expected == data.isPresent());
        var shown = new LinkedHashMap<String, Integer>();
        data.ifPresent(row -> row.entries().forEach(entry -> shown.put(entry.aspect().id(), entry.amount())));
        report.put("shown_aspects", shown);
        var expectedAmounts = new LinkedHashMap<String, Integer>();
        aspects.forEach((aspect, amount) -> expectedAmounts.put(aspect.id(), amount));
        report.put("amounts_pass", data.isEmpty() || shown.equals(expectedAmounts));
        log(report);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void rendering(RenderTooltipEvent.Color event) {
        if (event.getItemStack().isEmpty()) return;
        var displays = event.getComponents().stream().filter(AspectTooltip.Display.class::isInstance)
                .map(AspectTooltip.Display.class::cast).toList();
        var report = new LinkedHashMap<String, Object>();
        report.put("phase", "render");
        report.put("item", BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()).toString());
        report.put("shift", Screen.hasShiftDown());
        report.put("render_components", displays.size());
        report.put("sizes", displays.stream().map(display -> display.getWidth(event.getFont()) + "x" + display.getHeight()).toList());
        log(report);
    }

    private void log(Map<String, Object> report) {
        var json = gson.toJson(report);
        if (seen.add(json)) System.out.println("SHIFT_ASPECTS_RUNTIME " + json);
    }
}
