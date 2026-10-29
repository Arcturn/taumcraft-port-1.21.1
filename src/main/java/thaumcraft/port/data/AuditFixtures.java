package thaumcraft.port.data;

import com.mojang.brigadier.CommandDispatcher;
import java.util.List;
import net.minecraft.commands.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import thaumcraft.port.research.AuditCommands;
import thaumcraft.port.research.ResearchRequirementAudit;

/** Real Brigadier/registry argument checks; no MinecraftServer instance or world. */
final class AuditFixtures {
    static int validate(RegistryAccess registries) throws Exception {
        var context = CommandBuildContext.simple(registries, FeatureFlags.DEFAULT_FLAGS);
        var dispatcher = new CommandDispatcher<CommandSourceStack>();
        AuditCommands.register(new RegisterCommandsEvent(dispatcher, Commands.CommandSelection.ALL, context));
        var op = source(2);
        int checks = 0;
        for (String command : List.of("thaumcraft audit", "thaumcraft audit research", "thaumcraft audit research BASEAUROMANCY")) {
            var parsed = dispatcher.parse(command, op);
            if (parsed.getReader().canRead() || !parsed.getExceptions().isEmpty()) throw new IllegalStateException("Audit syntax: " + command);
            checks++;
        }
        if (!dispatcher.parse("thaumcraft audit", source(0)).getReader().canRead()) throw new IllegalStateException("Audit must require OP");
        checks++;
        long unsupported = ResearchRequirementAudit.collect().stream().filter(r -> !r.supported()).count();
        if (dispatcher.execute("thaumcraft audit research", op) != unsupported) throw new IllegalStateException("Unsupported count");
        checks++;
        if (dispatcher.execute("thaumcraft audit research BASEAUROMANCY", op) != 2) throw new IllegalStateException("Focus/workbench blockers lost");
        checks++;
        if (dispatcher.execute("thaumcraft audit research UNKNOWN_TEST_KEY", op) != 0) throw new IllegalStateException("Unknown key accepted");
        checks++;
        // Parse representative modern bench commands against actual vanilla argument types.
        var vanilla = new Commands(Commands.CommandSelection.ALL, context).getDispatcher();
        for (String command : List.of("gamemode creative @s", "difficulty normal", "gamerule doDaylightCycle false",
                "gamerule doWeatherCycle false", "time set 6000", "weather clear", "fill -16 64 -16 16 64 16 minecraft:stone",
                "fill -16 65 -16 16 78 16 minecraft:air", "tp @s 0.5 65 14.5 180 10",
                "setblock -12 65 -12 thaumcraft:arcane_workbench", "give @s thaumcraft:thaumometer 1")) {
            var parsed = vanilla.parse(command, op);
            if (parsed.getReader().canRead() || !parsed.getExceptions().isEmpty()) throw new IllegalStateException("Bench syntax: " + command + " " + parsed.getExceptions());
            checks++;
        }
        return checks;
    }

    private static CommandSourceStack source(int permission) {
        return new CommandSourceStack(CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, permission,
                "parity-check", Component.literal("parity-check"), null, null);
    }
    private AuditFixtures() {}
}
