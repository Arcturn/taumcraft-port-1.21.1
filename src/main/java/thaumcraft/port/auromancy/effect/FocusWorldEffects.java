package thaumcraft.port.auromancy.effect;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.tags.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import thaumcraft.port.content.CasterItem;
import thaumcraft.port.world.AuraSystem;

/** BETA26 ServerEvents' per-world breaker/swapper queues and portable-hole creation. */
public final class FocusWorldEffects {
    public static final TagKey<Block> HOLE_BLACKLIST=TagKey.create(Registries.BLOCK,ResourceLocation.parse("thaumcraft:portable_hole_blacklist"));
    private static final Map<ServerLevel,List<BreakWork>> BREAKS=new WeakHashMap<>();
    private static final Map<ServerLevel,List<SwapWork>> SWAPS=new WeakHashMap<>();
    private static int nextCrackId=-1_000_000;
    private static final class BreakWork {
        final ServerPlayer player;final BlockPos pos;final BlockState source;final boolean fx,silk;final int fortune,id;final float strength,maximum,cost;final InteractionHand hand;
        float remaining;int delay;
        BreakWork(ServerPlayer player,BlockPos pos,BlockState source,boolean fx,boolean silk,int fortune,float strength,float remaining,float maximum,int delay,float cost,InteractionHand hand){this.player=player;this.pos=pos.immutable();this.source=source;this.fx=fx;this.silk=silk;this.fortune=fortune;this.strength=strength;this.remaining=remaining;this.maximum=maximum;this.delay=delay;this.cost=cost;this.hand=hand;id=nextCrackId--;}
    }
    private record SwapWork(ServerPlayer player,BlockPos pos,BlockState source,ItemStack target,boolean silk,int fortune,float cost,InteractionHand hand){}
    public static void register() {
        NeoForge.EVENT_BUS.addListener(FocusWorldEffects::tick);
        NeoForge.EVENT_BUS.addListener(FocusWorldEffects::unload);
    }
    private static void unload(LevelEvent.Unload event){if(event.getLevel() instanceof ServerLevel level){BREAKS.remove(level);SWAPS.remove(level);}}
    public static boolean mayModify(Level level,LivingEntity caster,BlockPos pos) {
        return !level.isClientSide&&level.hasChunkAt(pos)&&level.getWorldBorder().isWithinBounds(pos)&&!level.isOutsideBuildHeight(pos)
                &&(!(caster instanceof Player player)||player.mayBuild()&&level.mayInteract(player,pos));
    }
    public static void queueBreak(ServerPlayer player,BlockPos pos,BlockState source,boolean fx,boolean silk,int fortune,float strength,float remaining,float maximum,int delay,float cost,InteractionHand hand) {
        if(!(strength>0)||!Float.isFinite(strength)||!Float.isFinite(remaining)||source.isAir())return;
        BREAKS.computeIfAbsent(player.serverLevel(),k->new ArrayList<>()).add(new BreakWork(player,pos,source,fx,silk,fortune,strength,remaining,maximum,Math.max(0,delay),cost,hand));
    }
    public static void queueExchange(ServerPlayer player,BlockPos pos,BlockState source,ItemStack target,boolean silk,int fortune,float cost,InteractionHand hand) {
        if(!target.isEmpty())SWAPS.computeIfAbsent(player.serverLevel(),k->new ArrayList<>()).add(new SwapWork(player,pos.immutable(),source,target.copyWithCount(1),silk,fortune,cost,hand));
    }
    public static ItemStack pickedBlock(ItemStack caster,Player player){return CasterItem.getPickedBlock(caster);}
    private static void tick(LevelTickEvent.Post event){if(event.getLevel() instanceof ServerLevel level)tickWorld(level);}
    /** Public for deterministic server tests; each invocation is one ordinary world tick. */
    public static void tickWorld(ServerLevel level) {
        var swaps=SWAPS.remove(level);if(swaps!=null)for(var work:swaps)swap(level,work);
        var breaks=BREAKS.remove(level);if(breaks==null)return;
        var remaining=new ArrayList<BreakWork>();
        for(var work:breaks) {
            if(work.delay-->0){remaining.add(work);continue;}
            if(work.player.isRemoved()||work.player.level()!=level||!mayModify(level,work.player,work.pos)||!level.getBlockState(work.pos).equals(work.source)||work.source.getDestroySpeed(level,work.pos)<0||AuraSystem.drain(level,work.pos,work.cost,false,true)<work.cost){clearCrack(level,work);continue;}
            if(work.fx)level.destroyBlockProgress(work.id,work.pos,work.maximum>0?(int)((1-work.remaining/work.maximum)*10):9);
            work.remaining-=work.strength;
            if(work.remaining<=0) {
                if(harvest(level,work.player,work.pos,work.silk,work.fortune,work.hand))AuraSystem.drain(level,work.pos,work.cost,false,false);
                clearCrack(level,work);
            }else remaining.add(work);
        }
        if(!remaining.isEmpty())BREAKS.computeIfAbsent(level,k->new ArrayList<>()).addAll(remaining);
    }
    private static void clearCrack(ServerLevel level,BreakWork work){if(work.fx)level.destroyBlockProgress(work.id,work.pos,-1);}
    private static ItemStack lootTool(ServerLevel level,Player player,boolean silk,int fortune,InteractionHand hand) {
        ItemStack tool=player.getItemInHand(hand).copy();if(tool.isEmpty())tool=new ItemStack(Items.DIAMOND_PICKAXE);
        var enchantments=level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        if(silk)tool.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH),1);
        var fortuneType=enchantments.getOrThrow(Enchantments.FORTUNE);
        if(fortune>net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(fortuneType,tool))tool.enchant(fortuneType,fortune);
        return tool;
    }
    public static boolean harvest(ServerLevel level,ServerPlayer player,BlockPos pos,boolean silk,int fortune,InteractionHand hand) {
        if(!mayModify(level,player,pos))return false;var state=level.getBlockState(pos);
        if(state.isAir()||state.getDestroySpeed(level,pos)<0)return false;
        if(NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level,pos,state,player)).isCanceled())return false;
        var tile=level.getBlockEntity(pos);var tool=lootTool(level,player,silk,fortune,hand);
        if(!level.removeBlock(pos,false))return false;
        level.levelEvent(2001,pos,Block.getId(state));
        if(!player.getAbilities().instabuild)state.getBlock().playerDestroy(level,player,pos,state,tile,tool);
        return true;
    }
    private static void swap(ServerLevel level,SwapWork work) {
        var player=work.player;BlockPos pos=work.pos;var source=level.getBlockState(pos);
        if(player.isRemoved()||player.level()!=level||!mayModify(level,player,pos)||!source.equals(work.source)||source.getDestroySpeed(level,pos)<0||AuraSystem.drain(level,pos,work.cost,false,true)<work.cost)return;
        if(!(work.target.getItem() instanceof BlockItem item))return;
        BlockState target=work.target.getOrDefault(DataComponents.BLOCK_STATE,BlockItemStateProperties.EMPTY).apply(item.getBlock().defaultBlockState());
        if(source.equals(target))return;
        int slot=-1;
        for(int i=0;i<player.getInventory().getContainerSize();i++)if(ItemStack.isSameItem(player.getInventory().getItem(i),work.target)&&!player.getInventory().getItem(i).isEmpty()){slot=i;break;}
        if(!player.getAbilities().instabuild&&slot<0)return;
        // The original swapper posts a placement event before committing payment or loot.
        var snapshot=BlockSnapshot.create(level.dimension(),level,pos);
        if(EventHooks.onBlockPlace(player,snapshot,Direction.UP))return;
        var drops=player.getAbilities().instabuild?List.<ItemStack>of():Block.getDrops(source,level,pos,level.getBlockEntity(pos),player,lootTool(level,player,work.silk,work.fortune,work.hand));
        if(!level.setBlockAndUpdate(pos,target))return;
        if(!player.getAbilities().instabuild) {
            player.getInventory().removeItem(slot,1);player.getInventory().setChanged();
            for(ItemStack drop:drops)if(!player.getInventory().add(drop))Block.popResource(level,pos,drop);
            AuraSystem.drain(level,pos,work.cost,false,false);
        }
        FocusEffectVisuals.bamf(level,Vec3.atCenterOf(pos),8038177,true,false);
    }
    public static boolean holeBlacklisted(BlockState state) {
        return state.is(Blocks.BEDROCK)||state.is(HOLE_BLACKLIST)||state.is(BlockTags.BEDS)||state.is(BlockTags.DOORS)
                ||state.is(Blocks.PISTON)||state.is(Blocks.STICKY_PISTON)||state.is(Blocks.PISTON_HEAD)||state.is(Blocks.MOVING_PISTON)
                ||net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(ResourceLocation.parse("thaumcraft:infernal_furnace"));
    }
    public static boolean createHole(ServerLevel level,BlockPos pos,Direction direction,int count,int duration,LivingEntity caster) {
        if(!mayModify(level,caster,pos))return false;var state=level.getBlockState(pos);
        if(level.getBlockEntity(pos)!=null||state.hasBlockEntity()||holeBlacklisted(state)||state.is(FocusEffectContent.HOLE.get())||!state.isAir()&&state.canBeReplaced()||state.getDestroySpeed(level,pos)<0)return false;
        if(caster instanceof Player player&&NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level,pos,state,player)).isCanceled())return false;
        if(!level.setBlock(pos,FocusEffectContent.HOLE.get().defaultBlockState(),3))return false;
        if(level.getBlockEntity(pos) instanceof HoleBlockEntity hole){hole.initialize(state,Math.max(1,duration),Math.max(0,count),direction,caster==null?null:caster.getUUID());return true;}
        level.setBlockAndUpdate(pos,state);return false;
    }
    private FocusWorldEffects(){}
}
