package thaumcraft.port.golem.seal;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import thaumcraft.port.golem.IGolemAPI;

/** Owner-attributed world actions. All mutable fake-player inventories belong to one action. */
public final class GolemInteractionHelper {
    private static final UUID UNOWNED = UUID.nameUUIDFromBytes("thaumcraft:golem".getBytes(StandardCharsets.UTF_8));
    private static final String RETURNS = "thaumcraft:golem_returns";

    public static FakePlayer fakePlayer(ServerLevel level, IGolemAPI golem) {
        UUID uuid = golem.getGolemEntity() instanceof OwnableEntity owned ? owned.getOwnerUUID() : null;
        if (uuid == null) uuid = UNOWNED;
        var online = level.getServer().getPlayerList().getPlayer(uuid);
        var profile = online == null ? new GameProfile(uuid,"ThaumcraftGolem") : online.getGameProfile();
        var player = new FakePlayer(level,profile);
        player.setGameMode(GameType.SURVIVAL);
        var entity = golem.getGolemEntity();
        player.moveTo(entity.getX(),entity.getY(),entity.getZ(),entity.getYRot(),entity.getXRot());
        return player;
    }

    /** clickStack has already been removed from carrying: its remaining contents are always returned. */
    public static InteractionResult golemClick(Level level, IGolemAPI golem, BlockPos pos, Direction face,
                                               ItemStack clickStack, boolean sneaking, boolean rightClick) {
        return golemClick(level,golem,pos,face,clickStack,sneaking,rightClick,true);
    }
    static InteractionResult golemClick(Level level, IGolemAPI golem, BlockPos pos, Direction face,
                                       ItemStack clickStack, boolean sneaking, boolean rightClick, boolean rewardAttempt) {
        if (!(level instanceof ServerLevel server) || !level.hasChunkAt(pos)) {
            returnStack(golem,clickStack); return InteractionResult.FAIL;
        }
        flushReturns(golem);
        var player = fakePlayer(server,golem);
        player.setItemInHand(InteractionHand.MAIN_HAND,clickStack);
        player.setShiftKeyDown(sneaking);
        try {
            if (!level.mayInteract(player,pos) || player.blockActionRestricted(level,pos,GameType.SURVIVAL)) return InteractionResult.FAIL;
            InteractionResult result;
            if (rightClick) {
                if (clickStack.getItem() instanceof BlockItem blockItem
                        && !level.isUnobstructed(blockItem.getBlock().defaultBlockState(),pos,CollisionContext.empty())) {
                    var entity = golem.getGolemEntity();
                    entity.setPos(entity.getX()+face.getStepX(),entity.getY()+face.getStepY(),entity.getZ()+face.getStepZ());
                }
                result = player.gameMode.useItemOn(player,level,clickStack,InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos),face,pos,false));
            } else {
                var before = level.getBlockState(pos);
                // BETA26 sends one left-click/start action; sustained mining belongs to the breaker seal.
                player.gameMode.handleBlockBreakAction(pos,ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK,
                        face,level.getMaxBuildHeight(),0);
                level.destroyBlockProgress(player.getId(),pos,-1);
                result = level.getBlockState(pos)!=before ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }
            if (rewardAttempt || result.consumesAction()) golem.addRankXp(1);
            golem.swingArm(); return result;
        } finally {
            for (int i=0;i<player.getInventory().getContainerSize();i++) {
                var stack = player.getInventory().removeItemNoUpdate(i);
                if (!stack.isEmpty()) returnStack(golem,stack);
            }
            var cursor=player.containerMenu.getCarried(); player.containerMenu.setCarried(ItemStack.EMPTY);
            if (!cursor.isEmpty()) returnStack(golem,cursor);
        }
    }

    /** The harvest seal's built-in hoe is an ability, not a newly-created carried item. */
    public static boolean tillSoil(Level level, IGolemAPI golem, BlockPos soil) {
        if (!(level instanceof ServerLevel server) || !level.hasChunkAt(soil)) return false;
        var player=fakePlayer(server,golem);
        if (!level.mayInteract(player,soil) || player.blockActionRestricted(level,soil,GameType.SURVIVAL)) return false;
        var hoe=new ItemStack(Items.IRON_HOE); player.setItemInHand(InteractionHand.MAIN_HAND,hoe);
        return player.gameMode.useItemOn(player,level,hoe,InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(soil),Direction.UP,soil,false)).consumesAction();
    }

    public static boolean harvestBlock(Level level, IGolemAPI golem, BlockPos pos, boolean alwaysDrop, boolean silk) {
        if (!(level instanceof ServerLevel server) || !level.hasChunkAt(pos)) return false;
        var state=level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level,pos)<0 || state.getBlock() instanceof GameMasterBlock) return false;
        var player=fakePlayer(server,golem);
        if (!level.mayInteract(player,pos) || player.blockActionRestricted(level,pos,GameType.SURVIVAL)) return false;
        // Unlike the original skipEvent path, owner protection gets the final say before any mutation.
        if (CommonHooks.fireBlockBreak(server,GameType.SURVIVAL,player,pos,state).isCanceled()) return false;
        var blockEntity=level.getBlockEntity(pos);
        boolean drops=alwaysDrop || state.canHarvestBlock(level,pos,player);
        var tool=ItemStack.EMPTY;
        if (silk) {
            tool=new ItemStack(Items.STICK);
            tool.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),1);
        }
        var harvested=state.getBlock().playerWillDestroy(level,pos,state,player);
        boolean removed=harvested.onDestroyedByPlayer(level,pos,player,drops,level.getFluidState(pos));
        if (!removed) return false;
        harvested.getBlock().destroy(level,pos,harvested);
        if (drops) harvested.getBlock().playerDestroy(level,player,pos,harvested,blockEntity,tool);
        return true;
    }

    /** Source greedy outward search, bounded to 49 x 97 x 49 and never loading a chunk. */
    public static BlockPos furthestLog(Level level, BlockPos origin) {
        var source=level.getBlockState(origin); var last=origin; double distance=0;
        while (true) {
            BlockPos next=null;
            search: for (int x=-2;x<=2;x++) for (int y=2;y>=-2;y--) for (int z=-2;z<=2;z++) {
                var candidate=last.offset(x,y,z);
                if (Math.abs(candidate.getX()-origin.getX())>24 || Math.abs(candidate.getY()-origin.getY())>48
                        || Math.abs(candidate.getZ()-origin.getZ())>24) return last;
                if (!level.hasChunkAt(candidate)) continue;
                var state=level.getBlockState(candidate); double d=candidate.distSqr(origin);
                // Modern log species are separate blocks; AXIS is not part of the legacy dropped metadata.
                if (state.is(source.getBlock()) && state.getDestroySpeed(level,candidate)>=0 && d>distance) {
                    next=candidate; distance=d; break search;
                }
            }
            if (next==null) return last;
            last=next;
        }
    }
    public static boolean breakFurthestBlock(Level level, IGolemAPI golem, BlockPos origin) {
        if (!level.hasChunkAt(origin) || !level.getBlockState(origin).is(BlockTags.LOGS)) return false;
        var last=furthestLog(level,origin);
        if (!harvestBlock(level,golem,last,false,false)) return false;
        if (level.getBlockState(origin).is(BlockTags.LOGS)) {
            for (var pos : BlockPos.betweenClosed(last.offset(-3,-3,-3),last.offset(3,3,3))) {
                if (!level.hasChunkAt(pos)) continue;
                var state=level.getBlockState(pos);
                level.scheduleTick(pos,state.getBlock(),50+level.random.nextInt(75));
            }
        }
        return true;
    }

    public static void returnStack(IGolemAPI golem, ItemStack stack) {
        if (stack.isEmpty()) return;
        var remaining=golem.holdItem(stack);
        if (remaining.isEmpty() || SealInventory.eject(golem.getGolemWorld(),golem.getGolemEntity().position(),remaining)) return;
        // Keep ownership if another mod cancels item spawning. Entity persistent data survives world reloads.
        var data=golem.getGolemEntity().getPersistentData(); var pending=data.getList(RETURNS,Tag.TAG_COMPOUND);
        pending.add(remaining.save(golem.getGolemWorld().registryAccess())); data.put(RETURNS,pending);
    }
    public static void flushReturns(IGolemAPI golem) {
        var data=golem.getGolemEntity().getPersistentData(); var pending=data.getList(RETURNS,Tag.TAG_COMPOUND);
        if (pending.isEmpty()) return;
        data.remove(RETURNS);
        for (int i=0;i<pending.size();i++) returnStack(golem,ItemStack.parseOptional(golem.getGolemWorld().registryAccess(),pending.getCompound(i)));
    }
    public static boolean hasPendingReturns(IGolemAPI golem) {
        return !golem.getGolemEntity().getPersistentData().getList(RETURNS,Tag.TAG_COMPOUND).isEmpty();
    }
    private GolemInteractionHelper() { }
}
