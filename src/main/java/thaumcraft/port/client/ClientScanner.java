package thaumcraft.port.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.port.aspect.AspectTags;
import thaumcraft.port.content.*;
import thaumcraft.port.research.*;

/** Original local feedback and world readouts. Research rewards remain server-owned. */
@EventBusSubscriber(modid="thaumcraft", value=Dist.CLIENT)
public final class ClientScanner {
    private static Entity entityReadout;

    @SubscribeEvent public static void recipes(net.neoforged.neoforge.client.event.RecipesUpdatedEvent event) {
        thaumcraft.port.aspect.RecipeAspects.clear();
    }
    static boolean held() {
        var player=Minecraft.getInstance().player;
        return player!=null && (player.getMainHandItem().is(PortContent.THAUMOMETER.get())
                || player.getOffhandItem().is(PortContent.THAUMOMETER.get()));
    }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        ClientJournal.state=new ResearchState();
        ClientJournal.scanTarget="";
        ClientJournal.auraBase=ClientJournal.auraVis=ClientJournal.auraFlux=0;
        ClientJournal.scanAspects=java.util.Map.of();
        entityReadout=null;
        ScannerWorldAspects.reset();
        ResearchScreen.reset();
    }

    /** Called by the client-side use action, including scans that learn nothing. */
    public static void activate() {
        var mc=Minecraft.getInstance();
        if(mc.player==null || mc.level==null) return;
        var hit=Scanner.target(mc.player,9,mc.player.blockInteractionRange());
        Vec3 origin=null;
        int duration=15;
        if(hit instanceof EntityHitResult entity) {
            var target=entity.getEntity();
            origin=ScannerVisualRules.entityRuneOrigin(target.position(),target.getEyeHeight());
            duration=ScannerVisualRules.entityRuneDuration(target.getBbHeight());
        } else if(hit instanceof BlockHitResult block && hit.getType()!=HitResult.Type.MISS) {
            origin=ScannerVisualRules.blockRuneOrigin(block.getBlockPos());
        }
        if(origin!=null) {
            var random=mc.level.random;
            for(int i=0;i<ScannerVisualRules.RUNES_PER_USE;i++) {
                float red=.3f+random.nextFloat()*.7f, blue=.3f+random.nextFloat()*.7f;
                var rune=mc.particleEngine.createParticle(PortParticles.get("scan_rune"),origin.x,origin.y,origin.z,0,duration,0);
                if(rune!=null) rune.setColor(red,0,blue);
            }
        }
        // In TC6 this is local PLAYERS audio on every use, not a server success cue.
        mc.level.playLocalSound(mc.player.getX(),mc.player.getY(),mc.player.getZ(),PortSounds.get("scan"),
                SoundSource.PLAYERS,ScannerVisualRules.SCAN_VOLUME,ScannerVisualRules.SCAN_PITCH,false);
    }

    @SubscribeEvent public static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(!held() || mc.level==null) {entityReadout=null;return;}
        if(mc.isPaused() || mc.player.tickCount%5!=0) return;
        var entity=Scanner.entityTarget(mc.player,16,5);
        entityReadout=entity==null?null:entity.getEntity();
        if(entity!=null) highlight(entity);
        var random=mc.level.random;
        var from=mc.player.getEyePosition();
        var look=Vec3.directionFromRotation(mc.player.getXRot()+random.nextInt(25)-random.nextInt(25),
                mc.player.getYRot()+random.nextInt(25)-random.nextInt(25));
        highlight(mc.level.clip(new net.minecraft.world.level.ClipContext(from,from.add(look.scale(16)),
                net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.ANY,mc.player)));
    }

    private static void highlight(HitResult hit) {
        var mc=Minecraft.getInstance();
        if(!Scanner.observation(mc.player,hit).canLearn(ClientJournal.state)) return;
        AABB box;
        if(hit instanceof EntityHitResult entity) box=entity.getEntity().getBoundingBox();
        else if(hit instanceof BlockHitResult block && hit.getType()!=HitResult.Type.MISS) {
            var shape=mc.level.getBlockState(block.getBlockPos()).getShape(mc.level,block.getBlockPos());
            box=shape.isEmpty()?new AABB(block.getBlockPos()):shape.bounds().move(block.getBlockPos());
        } else return;
        var random=mc.level.random;
        int count=(int)Math.ceil(box.getSize()*2)*2;
        var center=box.getCenter();
        for(var face:Direction.values()) for(int i=0;i<count;i++) {
            double x=Mth.clamp(.5+face.getStepX()*.51+random.nextGaussian()*box.getXsize(),box.minX-center.x,box.maxX-center.x);
            double y=Mth.clamp(.5+face.getStepY()*.51+random.nextGaussian()*box.getYsize(),box.minY-center.y,box.maxY-center.y);
            double z=Mth.clamp(.5+face.getStepZ()*.51+random.nextGaussian()*box.getZsize(),box.minZ-center.z,box.maxZ-center.z);
            float red=(16+random.nextInt(17))/255f, green=(132+random.nextInt(34))/255f, blue=(223+random.nextInt(17))/255f;
            float size=.4f+(float)random.nextGaussian()*.1f;
            int delay=random.nextInt(10);
            var particle=mc.particleEngine.createParticle(PortParticles.get("scan_spark"),center.x+x,center.y+y,center.z+z,0,0,0);
            if(particle instanceof SalisMundusParticle sparkle) sparkle.scannerSparkle(size,red,green,blue,delay);
        }
    }

    @SubscribeEvent public static void hud(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(held() && mc.screen==null && !mc.options.hideGui) ScannerAuraHud.render(event.getGuiGraphics());
    }

    @SubscribeEvent public static void world(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
        if(event.getStage()!=net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        var mc=Minecraft.getInstance();
        var target=entityReadout;
        if(!held() || target==null || target.isRemoved() || target.level()!=mc.level || target instanceof ItemEntity) {
            ScannerWorldAspects.hide();return;
        }
        // Original AspectHelper reads entity profiles, not the dropped item's stack.
        ScannerWorldAspects.render(event,target,AspectTags.forEntity(target));
    }
    private ClientScanner() {}
}
