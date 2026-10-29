package thaumcraft.port.research;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.capabilities.Capabilities;
import thaumcraft.port.aspect.*;
import thaumcraft.port.content.*;

public final class Scanner {
    public static HitResult target(Player player,double entityRange,double blockRange){
        var entity=entityTarget(player,entityRange,0);if(entity!=null)return entity;
        var from=player.getEyePosition();return player.level().clip(new ClipContext(from,from.add(player.getViewVector(1).scale(blockRange)),ClipContext.Block.OUTLINE,ClipContext.Fluid.ANY,player));
    }
    /** TC6 target tolerance is at least 0.8 blocks; line of sight still excludes walls. */
    public static EntityHitResult entityTarget(Player player,double range,float padding){
        var from=player.getEyePosition();var look=player.getViewVector(1);var end=from.add(look.scale(range));EntityHitResult result=null;double nearest=Double.MAX_VALUE;
        for(var entity:player.level().getEntities(player,player.getBoundingBox().expandTowards(look.scale(range)).inflate(padding),e->!e.isSpectator())){
            if(from.distanceTo(entity.position())<1)continue;
            if(player.level().clip(new ClipContext(from,entity.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player)).getType()!=HitResult.Type.MISS)continue;
            var box=entity.getBoundingBox().inflate(Math.max(.8f,entity.getPickRadius()));var hit=box.contains(from)?java.util.Optional.of(from):box.clip(from,end);
            if(hit.isPresent()){double distance=from.distanceToSqr(hit.get());if(distance<nearest){nearest=distance;result=new EntityHitResult(entity,hit.get());}}
        }return result;
    }
    public static String subject(Player p,HitResult hit){
        if(hit instanceof EntityHitResult e){var target=e.getEntity();return target instanceof ItemEntity item?Scanning.itemSubject(item.getItem()):"entity:"+BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());}
        if(hit instanceof BlockHitResult b&&hit.getType()!=HitResult.Type.MISS){var stack=blockStack(p,b);return !stack.isEmpty()?Scanning.itemSubject(stack):"block:"+BuiltInRegistries.BLOCK.getKey(p.level().getBlockState(b.getBlockPos()).getBlock());}
        return "";
    }
    /** HUD cache identity is finer than the once-only research identity. */
    public static String readoutKey(Player p,HitResult hit){
        String key=subject(p,hit);
        if(hit instanceof EntityHitResult e){
            key+="/"+e.getEntity().getUUID();
            if(e.getEntity() instanceof ItemEntity item&&!item.getItem().isEmpty())key+="/"+item.getItem().save(p.level().registryAccess());
            else if(e.getEntity() instanceof thaumcraft.port.world.Wisp wisp)key+="/"+wisp.aspect().id();
        }
        return key;
    }
    public static java.util.Map<Aspect,Integer> aspects(Player p,HitResult hit){
        if(hit instanceof EntityHitResult e)return e.getEntity() instanceof ItemEntity item?itemAspects(p,item.getItem()):AspectTags.forEntity(e.getEntity());
        if(hit instanceof BlockHitResult b&&hit.getType()!=HitResult.Type.MISS){var s=p.level().getBlockState(b.getBlockPos());var a=AspectTags.forBlock(s);return a.isEmpty()?itemAspects(p,blockStack(p,b)):a;}
        return java.util.Map.of();
    }
    public static ItemStack blockFallback(net.minecraft.world.level.block.state.BlockState state){
        if(state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock){
            if(state.getFluidState().is(net.minecraft.tags.FluidTags.WATER))return new ItemStack(Items.WATER_BUCKET);
            if(state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA))return new ItemStack(Items.LAVA_BUCKET);
        }
        return new ItemStack(state.getBlock().asItem());
    }
    private static ItemStack blockStack(Player player,BlockHitResult hit){
        var state=player.level().getBlockState(hit.getBlockPos());
        var stack=state.getCloneItemStack(hit,player.level(),hit.getBlockPos(),player);
        return stack.isEmpty()?blockFallback(state):stack;
    }
    public static ScanObservation observation(Player player,HitResult hit){
        java.util.Set<String> flags=java.util.Set.of();
        if(hit instanceof EntityHitResult entity)flags=ScanDiscoveries.entityFlags(entity.getEntity());
        else if(hit instanceof BlockHitResult block&&hit.getType()!=HitResult.Type.MISS)
            flags=ScanDiscoveries.blockFlags(player.level().getBlockState(block.getBlockPos()),blockStack(player,block));
        return new ScanObservation(subject(player,hit),aspects(player,hit),flags);
    }
    private static java.util.Map<Aspect,Integer> itemAspects(Player p,ItemStack s){return RecipeAspects.get(s,p.level());}
    public static void use(ServerPlayer p){
        if(!p.isAlive())return;
        var hit=target(p,9,p.blockInteractionRange());
        if(hit.getType()==HitResult.Type.MISS){
            if(!sky(p))p.displayClientMessage(Scanning.feedback(false),true);
            ResearchProgress.sync(p);return;
        }
        Scanning.scan(p,observation(p,hit),true);
        if(hit instanceof BlockHitResult b){
            // Same UP-facing item capability as the original scanner. Never extracts items.
            var inv=p.level().getCapability(Capabilities.ItemHandler.BLOCK,b.getBlockPos(),Direction.UP);
            if(inv!=null&&visitInventory(inv,s->Scanning.scan(p,new ScanObservation(Scanning.itemSubject(s),RecipeAspects.get(s,p.serverLevel()),ScanDiscoveries.itemFlags(s)),true))>=100)
                p.displayClientMessage(Component.translatable("tc.invtoolarge").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE,net.minecraft.ChatFormatting.ITALIC),true);
        }
        ResearchProgress.sync(p);
    }
    /** The original limit counts occupied slots, never item counts or empty slots. */
    public static int visitInventory(net.neoforged.neoforge.items.IItemHandler inventory,java.util.function.Consumer<ItemStack> scan){
        int scanned=0;
        for(int slot=0;slot<inventory.getSlots();slot++){
            var stack=inventory.getStackInSlot(slot);if(stack.isEmpty())continue;
            scan.accept(stack);if(++scanned>=100)break;
        }
        return scanned;
    }
    /** True means ScanSky handled the miss and suppresses the ordinary scan status. */
    private static boolean sky(ServerPlayer p){
        var storage=ResearchStorage.get(p.getServer());var state=storage.player(p.getUUID());
        if(p.level().dimension()!=Level.OVERWORLD||!p.level().canSeeSky(p.blockPosition().above())||!state.knows("CELESTIALSCANNING"))return false;
        var t=CelestialTarget.find(p.getYRot(),p.getXRot(),p.level().getTimeOfDay(0),p.level().getMoonPhase(),p.getDirection().get3DDataValue()-2);if(t==null)return false;
        String key=t.key(p.level().getGameTime());if(state.knows(key)){p.displayClientMessage(Component.translatable("tc.celestial.fail.1",""),true);return true;}
        int paper=-1;boolean ink=false;var inv=p.getInventory();for(int i=0;i<inv.getContainerSize();i++){var s=inv.getItem(i);if(s.is(PortContent.SCRIBING_TOOLS.get()))ink=true;if(s.is(Items.PAPER))paper=i;}
        if(!ink||paper<0)p.displayClientMessage(Component.translatable("tc.celestial.fail.2",""),true);
        else {
            inv.removeItem(paper,1);var note=CelestialNotesItem.stack(t.variant());if(!inv.add(note))p.drop(note,false);inv.setChanged();state.flag(key);
        }
        // Original ScanSky cleans old day keys after both success and missing materials.
        state.pruneCelestial(p.level().getGameTime()/24000);storage.setDirty();return true;
    }
    private Scanner(){}
}
