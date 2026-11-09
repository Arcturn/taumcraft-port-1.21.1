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
        container.registerConfig(ModConfig.Type.COMMON, thaumcraft.port.warp.WarpConfig.SPEC, "thaumcraft-warp.toml");
        thaumcraft.port.content.PortSounds.register(modBus);
        thaumcraft.port.content.PortParticles.register(modBus);
        WorldContent.register(modBus);
        WorldEntities.register(modBus);
        thaumcraft.port.mob.MobContent.register(modBus);
        thaumcraft.port.boss.BossContent.register(modBus);
        thaumcraft.port.champion.ChampionContent.register(modBus);
        thaumcraft.port.warp.WarpEffects.register(modBus);
        thaumcraft.port.warp.entity.WarpEntities.register(modBus);
        AuraSystem.register(modBus);
        modBus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(() -> {
            if (WorldConfig.MAGIC_FOREST.get()) Regions.register(new MagicalForestRegion());
        }));
        thaumcraft.port.machine.MachineContent.register(modBus);
        thaumcraft.port.essentia.EssentiaContent.register(modBus);
        thaumcraft.port.theory.TheoryContent.register(modBus);
        thaumcraft.port.alchemy.liquid.AlchemyLiquids.register(modBus);
        thaumcraft.port.alchemy.spa.SpaContent.register(modBus);
        thaumcraft.port.alchemy.devices.AlchemyDevices.register(modBus);
        thaumcraft.port.essentia.EssentiaTransferContent.register(modBus);
        thaumcraft.port.alchemy.material.AlchemyMaterials.register(modBus);
        thaumcraft.port.essentia.CondenserContent.register(modBus);
        thaumcraft.port.infusion.structure.StructureContent.register(modBus);
        thaumcraft.port.infusion.matrix.MatrixContent.register(modBus);
        thaumcraft.port.infusion.recipe.InfusionRecipes.register(modBus);
        thaumcraft.port.infusion.tools.InfusionTools.register(modBus);
        thaumcraft.port.infusion.armor.InfusionArmor.register(modBus);
        thaumcraft.port.infusion.bauble.InfusionBaubles.register(modBus);
        thaumcraft.port.crafting.content.CraftingContent.register(modBus);
        thaumcraft.port.infusion.content.InfusionCraftingContent.register(modBus);
        thaumcraft.port.auromancy.AuromancyContent.register(modBus);
        thaumcraft.port.auromancy.item.CasterContent.register(modBus);
        thaumcraft.port.auromancy.entity.AuromancyEntities.register(modBus);
        thaumcraft.port.auromancy.effect.FocusEffectContent.register(modBus);
        thaumcraft.port.infusion.brain.BrainJarContent.register(modBus);
        thaumcraft.port.auromancy.firebat.FireBatContent.register(modBus);
        thaumcraft.port.auromancy.pech.PechContent.register(modBus);
        thaumcraft.port.artifice.device.ArtificeDevices.register(modBus);
        thaumcraft.port.artifice.mirror.MirrorContent.register(modBus);
        thaumcraft.port.artifice.construct.ConstructContent.register(modBus);
        thaumcraft.port.artifice.furnace.InfernalFurnaceContent.register(modBus);
        thaumcraft.port.artifice.lamp.ArtificeLamps.register(modBus);
        thaumcraft.port.artifice.tool.ArtificeTools.register(modBus);
        thaumcraft.port.artifice.siphon.VoidSiphonContent.register(modBus);
        thaumcraft.port.golem.GolemContent.register(modBus);
        PortContent.register(modBus);
        thaumcraft.port.research.ResearchAdvancements.register(modBus);
        modBus.addListener(PortNetwork::register);
        NeoForge.EVENT_BUS.addListener(CuriosEvents::onDeath);
        NeoForge.EVENT_BUS.addListener(thaumcraft.port.theory.TheoryDrops::drops);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent e)->{
            if(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer player)thaumcraft.port.research.ResearchProgress.craft(player,e.getCrafting());
        });
        NeoForge.EVENT_BUS.addListener(WorldCommands::register);
        NeoForge.EVENT_BUS.addListener(thaumcraft.port.warp.WarpCommands::register);
        NeoForge.EVENT_BUS.addListener(thaumcraft.port.research.ResearchCommands::register);
        NeoForge.EVENT_BUS.addListener(thaumcraft.port.research.AuditCommands::register);
        NeoForge.EVENT_BUS.addListener(thaumcraft.port.research.ResearchEvents::tick);
        NeoForge.EVENT_BUS.addListener(thaumcraft.port.content.SalisMundusRituals::tick);
        NeoForge.EVENT_BUS.addListener(thaumcraft.port.content.SalisMundusRituals::breakBlock);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.OnDatapackSyncEvent e)->thaumcraft.port.aspect.RecipeAspects.clear());
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.TagsUpdatedEvent e)->thaumcraft.port.aspect.RecipeAspects.clear());
    }
}
