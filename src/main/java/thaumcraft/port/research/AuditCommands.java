package thaumcraft.port.research;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.content.ResearchDisplayItem;

/** Diagnostics only: never reads/writes a player's progress, inventory, aura or world. */
public final class AuditCommands {
    public static void register(RegisterCommandsEvent event) {
        var root = Commands.literal("audit").requires(source -> source.hasPermission(2)).executes(c -> {
            int items = PortContent.ITEMS.getEntries().size();
            long hidden = PortContent.ITEMS.getEntries().stream().filter(e -> e.get() instanceof ResearchDisplayItem || e.get() == PortContent.RESEARCH_ICON.get()).count();
            long entities = BuiltInRegistries.ENTITY_TYPE.keySet().stream().filter(id -> id.getNamespace().equals("thaumcraft")).count();
            long recipes = c.getSource().getServer().getRecipeManager().getRecipes().stream().filter(r -> r.id().getNamespace().equals("thaumcraft")).count();
            var requirements = ResearchRequirementAudit.collect();
            long blocked = requirements.stream().filter(r -> !r.supported()).count();
            long research = requirements.stream().filter(r -> !r.supported()).map(ResearchRequirementAudit.Requirement::research).distinct().count();
            c.getSource().sendSuccess(() -> Component.translatable("command.thaumcraft.audit.counts", items - hidden, hidden,
                    PortContent.BLOCKS.getEntries().size(), entities, recipes), false);
            c.getSource().sendSuccess(() -> Component.translatable("command.thaumcraft.audit.requirements", blocked, requirements.size(), research), false);
            c.getSource().sendSuccess(() -> Component.translatable("command.thaumcraft.audit.limit"), false);
            return 1;
        });
        root.then(Commands.literal("research")
                .executes(c -> {
                    var blocked = ResearchRequirementAudit.collect().stream().filter(r -> !r.supported()).toList();
                    for (var row : blocked) c.getSource().sendSuccess(() -> line(row), false);
                    if (blocked.isEmpty()) c.getSource().sendSuccess(() -> Component.translatable("command.thaumcraft.audit.no_unsupported"), false);
                    return blocked.size();
                })
                .then(Commands.argument("key", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(ResearchCatalog.all().stream().map(ResearchCatalog.Entry::key), b))
                        .executes(c -> {
                            String key = StringArgumentType.getString(c, "key");
                            if (ResearchCatalog.get(key) == null) {
                                c.getSource().sendFailure(Component.translatable("command.thaumcraft.audit.unknown", key));
                                return 0;
                            }
                            var rows = ResearchRequirementAudit.collect().stream().filter(r -> r.research().equals(key)).toList();
                            for (var row : rows) c.getSource().sendSuccess(() -> line(row), false);
                            if (rows.isEmpty()) c.getSource().sendSuccess(() -> Component.translatable("command.thaumcraft.audit.no_item_requirements", key), false);
                            return rows.size();
                        })));
        event.getDispatcher().register(Commands.literal("thaumcraft").then(root));
    }

    private static Component line(ResearchRequirementAudit.Requirement r) {
        return Component.translatable("command.thaumcraft.audit.requirement", r.research(), r.stage(), r.field(), r.source(),
                Component.translatable(r.supported() ? "command.thaumcraft.audit.supported" : "command.thaumcraft.audit.unsupported"));
    }

    private AuditCommands() {}
}
