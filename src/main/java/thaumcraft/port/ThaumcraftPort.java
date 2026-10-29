package thaumcraft.port;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import thaumcraft.port.world.*;
import terrablender.api.Regions;
import net.neoforged.neoforge.common.NeoForge;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.integration.CuriosEvents;
import thaumcraft.port.network.PortNetwork;

/** Unofficial, incremental migration. Original Thaumcraft by Azanor. */
@Mod(ThaumcraftPort.MOD_ID)
public final class ThaumcraftPort {
    public static final String MOD_ID = "thaumcraft";

    public ThaumcraftPort(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, WorldConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, thaumcraft.port.config.ClientVisualConfig.SPEC);
        thaumcraft.port.content.PortSounds.register(modBus);
        thaumcraft.port.content.PortParticles.register(modBus);
        WorldContent.register(modBus);
        WorldEntities.register(modBus);
        AuraSystem.register(modBus);
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(() -> {
            if (WorldConfig.MAGIC_FOREST.get()) Regions.register(new MagicalForestRegion());
        }));
        thaumcraft.port.machine.MachineContent.register(modBus);
        thaumcraft.port.essentia.EssentiaContent.register(modBus);
        thaumcraft.port.theory.TheoryContent.register(modBus);
        PortContent.register(modBus);
        thaumcraft.port.research.ResearchAdvancements.register(modBus);
        modBus.addListener(PortNetwork::register);
        NeoForge.EVENT_BUS.addListener(CuriosEvents::onDeath);
        NeoForge.EVENT_BUS.addListener(thaumcraft.port.theory.TheoryDrops::drops);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent e)->{
            if(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)thaumcraft.port.research.ResearchProgress.craft(player,e.getCrafting());
        });
        NeoForge.EVENT_BUS.addListener(WorldCommands::register);
        NeoForge.EVENT_BUS.addListener(thaumcraft.port.research.ResearchCommands::register);
        NeoForge.EVENT_BUS.addListener(thaumcraft.port.research.AuditCommands::register);
        NeoForge.EVENT_BUS.addListener(thaumcraft.port.research.ResearchEvents::tick);
        NeoForge.EVENT_BUS.addListener(thaumcraft.port.content.SalisMundusRituals::tick);
        NeoForge.EVENT_BUS.addListener(thaumcraft.port.content.SalisMundusRituals::breakBlock);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.OnDatapackSyncEvent e)->thaumcraft.port.aspect.RecipeAspects.clear());
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.TagsUpdatedEvent e)->thaumcraft.port.aspect.RecipeAspects.clear());
    }
}
