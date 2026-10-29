package thaumcraft.port.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import thaumcraft.port.ThaumcraftPort;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.content.PortContent;
import thaumcraft.port.world.*;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = ThaumcraftPort.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent public static void dustShader(net.neoforged.neoforge.client.event.RegisterShadersEvent e){SalisMundusParticle.registerShader(e);}
    @SubscribeEvent public static void auditIcons(net.neoforged.neoforge.client.event.ModelEvent.BakingCompleted e){IconModelAudit.check(e);}
    @SubscribeEvent public static void bookModels(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional e){BookAssets.registerModels(e);e.register(ResearchTableRenderer.QUILL);}
    @SubscribeEvent public static void menus(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e){e.register(thaumcraft.port.theory.TheoryContent.MENU.get(),TheoryScreen::new);e.register(thaumcraft.port.machine.MachineContent.WORKBENCH_MENU.get(),WorkbenchScreen::new);e.register(thaumcraft.port.essentia.EssentiaContent.SMELTER_MENU.get(),SmelterScreen::new);e.register(thaumcraft.port.essentia.EssentiaContent.THAUMATORIUM_MENU.get(),ThaumatoriumScreen::new);}
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->{
        PortContent.RESEARCH_DISPLAY_ICONS.values().forEach(item->net.minecraft.client.renderer.item.ItemProperties.register(item.get(),net.minecraft.resources.ResourceLocation.parse("thaumcraft:research_frame"),new ResearchFrameProperty(item.get().variants().size(),System::currentTimeMillis)));
        net.minecraft.client.renderer.item.ItemProperties.register(PortContent.CELESTIAL_NOTES.get(),net.minecraft.resources.ResourceLocation.parse("thaumcraft:celestial"),(net.minecraft.client.renderer.item.ItemPropertyFunction)(stack,level,entity,seed)->stack.getOrDefault(PortContent.CELESTIAL_KIND.get(),0));
        var fill=net.minecraft.resources.ResourceLocation.parse("thaumcraft:fill");var filled=net.minecraft.resources.ResourceLocation.parse("thaumcraft:filled");
        for(var block:java.util.List.of(thaumcraft.port.essentia.EssentiaContent.JAR,thaumcraft.port.essentia.EssentiaContent.VOID_JAR))net.minecraft.client.renderer.item.ItemProperties.register(block.get().asItem(),fill,(net.minecraft.client.renderer.item.ItemPropertyFunction)(stack,level,entity,seed)->{
            var data=stack.getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();int n=Math.clamp(data.getInt("Amount"),0,250);return n==0?0:(float)Math.ceil(n/62.5)/4;
        });
        for(var item:java.util.List.of(thaumcraft.port.essentia.EssentiaContent.PHIAL,thaumcraft.port.essentia.EssentiaContent.LABEL))net.minecraft.client.renderer.item.ItemProperties.register(item.get(),filled,(net.minecraft.client.renderer.item.ItemPropertyFunction)(stack,level,entity,seed)->thaumcraft.port.essentia.EssentiaItem.aspect(stack)==null?0:1);
    });}

    @SubscribeEvent public static void particles(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent e){
        thaumcraft.port.content.PortParticles.ALL.forEach((kind,type)->e.registerSprite(type.get(),kind.startsWith("nitor_")?new NitorParticle.Provider(kind):kind.equals("scan_rune")?new ScanRuneParticle.Provider():kind.equals("theory_rune")?new TheoryRuneParticle.Provider():kind.equals("salis_mundus")||kind.equals("scan_spark")?new SalisMundusParticle.Provider():new LegacyParticle.Provider(kind)));
    }
    @SubscribeEvent public static void geometry(net.neoforged.neoforge.client.event.ModelEvent.RegisterGeometryLoaders e){e.register(net.minecraft.resources.ResourceLocation.parse("thaumcraft:crystal_cluster"),new CrystalModel.Loader());}
    @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e){e.registerLayerDefinition(ResearchTableRenderer.LAYER,ResearchTableRenderer::layer);WorldRenderers.layers(e);EssentiaRenderers.layers(e);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(thaumcraft.port.theory.TheoryContent.ENTITY.get(),ResearchTableRenderer::new);WorldRenderers.register(e);EssentiaRenderers.register(e);e.registerBlockEntityRenderer(thaumcraft.port.machine.MachineContent.CRUCIBLE_ENTITY.get(),CrucibleRenderer::new);}
    @SubscribeEvent public static void blocks(RegisterColorHandlersEvent.Block e){
        e.register((s,l,p,tint)->l!=null&&p!=null?net.minecraft.client.renderer.BiomeColors.getAverageGrassColor(l,p):6750149,WorldContent.block("grass_ambient"));
        // TC6 ships grayscale greatwood leaves and colors them with the biome's foliage tint.
        // Silverwood's pixels already contain their color and must remain untinted.
        e.register((s,l,p,tint)->0xff000000|(l!=null&&p!=null?net.minecraft.client.renderer.BiomeColors.getAverageFoliageColor(l,p):net.minecraft.world.level.FoliageColor.getDefaultColor()),WorldContent.block("leaves_greatwood"));
        e.register((s,l,p,tint)->0xffffffff,WorldContent.block("leaves_silverwood"));
        thaumcraft.port.machine.MachineContent.NITOR.forEach((name,block)->e.register((s,l,p,tint)->0xff000000|net.minecraft.world.item.DyeColor.byName(name.substring(6),net.minecraft.world.item.DyeColor.YELLOW).getTextureDiffuseColor(),block.get()));
        WorldContent.CRYSTALS.forEach((a,b)->e.register((s,l,p,tint)->0xFF000000|a.color(),b.get()));
    }
    @SubscribeEvent
    public static void colors(RegisterColorHandlersEvent.Item event) {
        PortContent.RESEARCH_DISPLAY_ICONS.values().forEach(item->event.register((stack,tint)->item.get().colour(tint,System.currentTimeMillis()),item.get()));
        event.register(BookAssets::advancementColor,PortContent.RESEARCH_ICON.get());
        event.register((stack,tint)->{var aspect=thaumcraft.port.essentia.EssentiaItem.aspect(stack);return tint!=1||aspect==null?0xffffffff:0xff000000|aspect.color();},thaumcraft.port.essentia.EssentiaContent.PHIAL.get(),thaumcraft.port.essentia.EssentiaContent.LABEL.get());
        event.register((stack,tint)->{var tag=stack.getOrDefault(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();return tint!=1?0xffffffff:0xff000000|thaumcraft.port.aspect.Aspect.byId(tag.getString("Aspect")).map(thaumcraft.port.aspect.Aspect::color).orElse(0xffffff);},thaumcraft.port.essentia.EssentiaContent.JAR.get().asItem(),thaumcraft.port.essentia.EssentiaContent.VOID_JAR.get().asItem());
        event.register((stack,tint)->6750149,WorldContent.block("grass_ambient").asItem());
        event.register((stack,tint)->event.getBlockColors().getColor(((net.minecraft.world.item.BlockItem)stack.getItem()).getBlock().defaultBlockState(),null,null,tint),WorldContent.block("leaves_greatwood").asItem(),WorldContent.block("leaves_silverwood").asItem());
        thaumcraft.port.machine.MachineContent.NITOR.forEach((name,block)->event.register((s,tint)->tint==1?0xffffffff:0xff000000|net.minecraft.world.item.DyeColor.byName(name.substring(6),net.minecraft.world.item.DyeColor.YELLOW).getMapColor().col,block.get().asItem()));
        WorldContent.CRYSTALS.forEach((a,b)->event.register((stack,tint)->0xFF000000|a.color(),b.get().asItem()));
        event.register((stack, tint) -> 0xFF000000 | CrystalItem.aspect(stack).color(), PortContent.CRYSTAL_ESSENCE.get());
    }
}
