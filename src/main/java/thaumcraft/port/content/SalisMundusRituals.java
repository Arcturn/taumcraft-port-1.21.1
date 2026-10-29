package thaumcraft.port.content;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import thaumcraft.port.essentia.EssentiaContent;
import thaumcraft.port.essentia.ThaumatoriumBlock;
import thaumcraft.port.machine.CrucibleEntity;
import thaumcraft.port.machine.MachineContent;
import thaumcraft.port.network.SalisMundusFxPayload;
import thaumcraft.port.research.ResearchProgress;
import thaumcraft.port.research.ResearchStorage;

/** Server-owned delayed replacements. Paid dust is held in saved data until commit/refund. */
public final class SalisMundusRituals extends SavedData {
    public static final int DURATION_TICKS = 50;
    private static final String SAVE_KEY = "thaumcraft_salis_rituals_v1";
    private static final Factory<SalisMundusRituals> FACTORY =
            new Factory<>(SalisMundusRituals::new, SalisMundusRituals::load, null);
    private final List<Pending> pending = new ArrayList<>();
    private final Map<UUID, WeakReference<ServerPlayer>> actors = new HashMap<>();

    private enum Kind { BOOK, WORKBENCH, CRUCIBLE, THAUMATORIUM }
    private record Source(BlockPos pos, BlockState state) {}
    private record Pending(Kind kind, UUID actor, InteractionHand hand, Direction face, Direction facing,
                           Vec3 hit, long due, List<Source> sources, ItemStack payment) {
        BlockPos base() { return sources.getFirst().pos; }
        List<BlockPos> changed() { return sources.stream().map(Source::pos).toList(); }
    }

    private static SalisMundusRituals get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, SAVE_KEY);
    }

    public static InteractionResult start(UseOnContext context) {
        var player = context.getPlayer();
        var level = context.getLevel();
        if (player == null || player.isShiftKeyDown() || context.getItemInHand().isEmpty()) return InteractionResult.PASS;
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Kind kind;
        List<Source> sources = new ArrayList<>();
        if (state.is(Blocks.BOOKSHELF)) kind = Kind.BOOK;
        else if (state.is(Blocks.CRAFTING_TABLE)) kind = Kind.WORKBENCH;
        else if (state.is(Blocks.CAULDRON) || state.is(Blocks.WATER_CAULDRON)) kind = Kind.CRUCIBLE;
        else if (state.is(EssentiaContent.CONSTRUCT.get())) {
            kind = Kind.THAUMATORIUM;
            pos = level.getBlockState(pos.below()).is(EssentiaContent.CONSTRUCT.get()) ? pos.below() : pos;
            if (!level.getBlockState(pos.below()).is(MachineContent.CRUCIBLE.get())
                    || !level.getBlockState(pos.above()).is(EssentiaContent.CONSTRUCT.get())) return InteractionResult.PASS;
            state = level.getBlockState(pos);
        } else return InteractionResult.PASS;
        sources.add(new Source(pos.immutable(), state));
        if (kind == Kind.THAUMATORIUM) sources.add(new Source(pos.above(), level.getBlockState(pos.above())));
        for (Source source : sources) {
            if (!level.mayInteract(player, source.pos)
                    || !player.mayUseItemAt(source.pos, context.getClickedFace(), context.getItemInHand())) return InteractionResult.FAIL;
        }
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer actor)) return InteractionResult.SUCCESS;
        if (kind == Kind.THAUMATORIUM && !knowsThaumatorium(actor)) {
            actor.displayClientMessage(Component.translatable("gui.thaumcraft.thaumatorium_research"), true);
            return InteractionResult.FAIL;
        }
        SalisMundusRituals rituals = get(server);
        if (sources.stream().anyMatch(source -> rituals.reserved(source.pos))) return InteractionResult.FAIL;
        ItemStack payment = actor.getAbilities().instabuild ? ItemStack.EMPTY : context.getItemInHand().split(1);
        Pending job = new Pending(kind, actor.getUUID(), context.getHand(), context.getClickedFace(),
                actor.getDirection().getOpposite(), context.getClickLocation(), server.getGameTime() + DURATION_TICKS,
                List.copyOf(sources), payment);
        rituals.pending.add(job);
        rituals.actors.put(actor.getUUID(), new WeakReference<>(actor));
        rituals.setDirty();
        server.playSound(null, context.getClickedPos(), PortSounds.get("dust"), SoundSource.PLAYERS,
                .33f, 1 + (float) server.random.nextGaussian() * .05f);
        new SalisMundusFxPayload(SalisMundusFxPayload.START, actor.getId(), job.hand, context.getClickedPos(), job.hit, job.changed()).send(server);
        return InteractionResult.CONSUME;
    }

    private static boolean knowsThaumatorium(ServerPlayer actor) {
        return ResearchStorage.get(actor.getServer()).player(actor.getUUID()).knows("THAUMATORIUM");
    }

    private boolean reserved(BlockPos pos) {
        return pending.stream().anyMatch(job -> job.sources.stream().anyMatch(source -> source.pos.equals(pos)));
    }

    /** Original multiblock ritual also prevents players dismantling reserved input blocks. */
    public static void breakBlock(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            SalisMundusRituals rituals = level.getDataStorage().get(FACTORY, SAVE_KEY);
            if (rituals != null && rituals.pending.stream().anyMatch(job -> job.kind == Kind.THAUMATORIUM
                    && job.sources.stream().anyMatch(source -> source.pos.equals(event.getPos())))) event.setCanceled(true);
        }
    }

    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        SalisMundusRituals rituals = level.getDataStorage().get(FACTORY, SAVE_KEY);
        if (rituals == null || rituals.pending.isEmpty()) return;
        var iterator = rituals.pending.iterator();
        while (iterator.hasNext()) {
            Pending job = iterator.next();
            if (level.getGameTime() < job.due || job.sources.stream().anyMatch(source -> !level.hasChunkAt(source.pos))
                    || (job.kind == Kind.THAUMATORIUM && !level.hasChunkAt(job.base().below()))) continue;
            ServerPlayer actor = level.getServer().getPlayerList().getPlayer(job.actor);
            if (actor == null) {
                var reference = rituals.actors.get(job.actor);
                if (reference != null) actor = reference.get();
                if (actor != null && actor.isRemoved()) actor = null;
            }
            iterator.remove();
            rituals.setDirty();
            if (!valid(level, actor, job) || !complete(level, actor, job)) refund(level, actor, job);
        }
        rituals.actors.keySet().removeIf(id -> rituals.pending.stream().noneMatch(job -> job.actor.equals(id)));
    }

    private static boolean valid(ServerLevel level, ServerPlayer actor, Pending job) {
        if (actor == null) return false;
        ItemStack permissionItem = job.payment.isEmpty() ? new ItemStack(PortContent.SALIS_MUNDUS.get()) : job.payment;
        for (Source source : job.sources) {
            if (!level.getBlockState(source.pos).equals(source.state) || !level.mayInteract(actor, source.pos)
                    || !actor.mayUseItemAt(source.pos, job.face, permissionItem)) return false;
        }
        return job.kind != Kind.THAUMATORIUM
                || (knowsThaumatorium(actor) && level.getBlockState(job.base().below()).is(MachineContent.CRUCIBLE.get()));
    }

    private static boolean complete(ServerLevel level, ServerPlayer actor, Pending job) {
        BlockPos base = job.base();
        if (job.kind == Kind.THAUMATORIUM) {
            // Both halves must exist before either receives a neighbor notification.
            var facing = ThaumatoriumBlock.FACING;
            boolean top = level.setBlock(base.above(), EssentiaContent.THAUMATORIUM_TOP.get()
                    .defaultBlockState().setValue(facing, job.facing), Block.UPDATE_CLIENTS);
            boolean bottom = top && level.setBlock(base, EssentiaContent.THAUMATORIUM.get()
                    .defaultBlockState().setValue(facing, job.facing), Block.UPDATE_CLIENTS);
            if (!bottom) {
                for (Source source : job.sources) level.setBlock(source.pos, source.state, Block.UPDATE_CLIENTS);
                for (Source source : job.sources) level.updateNeighborsAt(source.pos, source.state.getBlock());
                return false;
            }
            level.updateNeighborsAt(base, level.getBlockState(base).getBlock());
            level.updateNeighborsAt(base.above(), level.getBlockState(base.above()).getBlock());
        } else {
            Block target = switch (job.kind) {
                case BOOK -> Blocks.AIR;
                case WORKBENCH -> MachineContent.WORKBENCH.get();
                case CRUCIBLE -> MachineContent.CRUCIBLE.get();
                default -> throw new IllegalStateException("Unknown simple dust conversion");
            };
            if (!level.setBlock(base, target.defaultBlockState(), Block.UPDATE_ALL)) return false;
            if (job.kind == Kind.BOOK) {
                // This is a materialised ritual result, not a randomly tossed block drop.
                Vec3 center = Vec3.atCenterOf(base);
                ItemEntity book = new ItemEntity(level, center.x, center.y, center.z,
                        new ItemStack(PortContent.THAUMONOMICON.get()), 0, 0, 0);
                book.setNoGravity(true);
                book.setDefaultPickUpDelay();
                level.addFreshEntity(book);
            } else ResearchProgress.craft(actor, new ItemStack(target));
            BlockState old = job.sources.getFirst().state;
            if (job.kind == Kind.CRUCIBLE && old.is(Blocks.WATER_CAULDRON)
                    && level.getBlockEntity(base) instanceof CrucibleEntity crucible) {
                crucible.state.fill(old.getValue(LayeredCauldronBlock.LEVEL) * 1000 / 3, false);
                crucible.sync();
            }
        }
        for (BlockPos part : job.changed()) level.playSound(null, part, PortSounds.get("poof"), SoundSource.BLOCKS,
                .4f, 1 + (float) level.random.nextGaussian() * .05f);
        new SalisMundusFxPayload(SalisMundusFxPayload.FINISH, actor.getId(), job.hand, base, job.hit, job.changed()).send(level);
        return true;
    }

    private static void refund(ServerLevel level, ServerPlayer actor, Pending job) {
        if (job.payment.isEmpty()) return;
        ItemStack refund = job.payment.copy();
        if (actor != null) actor.getInventory().add(refund);
        if (!refund.isEmpty()) {
            // This is a paid-item refund, not block loot; doTileDrops must not discard it.
            var pos = Vec3.atCenterOf(job.base());
            ItemEntity drop = new ItemEntity(level, pos.x, pos.y, pos.z, refund);
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }
    }

    public static SalisMundusRituals load(CompoundTag root, HolderLookup.Provider registries) {
        SalisMundusRituals rituals = new SalisMundusRituals();
        for (Tag entry : root.getList("pending", Tag.TAG_COMPOUND)) {
            CompoundTag tag = (CompoundTag) entry;
            try {
                List<Source> sources = new ArrayList<>();
                for (Tag sourceTag : tag.getList("sources", Tag.TAG_COMPOUND)) {
                    CompoundTag source = (CompoundTag) sourceTag;
                    sources.add(new Source(BlockPos.of(source.getLong("pos")),
                            NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), source.getCompound("state"))));
                }
                Kind kind = Kind.valueOf(tag.getString("kind"));
                if (sources.size() != (kind == Kind.THAUMATORIUM ? 2 : 1)) continue;
                Vec3 hit = new Vec3(tag.getDouble("hitX"), tag.getDouble("hitY"), tag.getDouble("hitZ"));
                if (!Double.isFinite(hit.lengthSqr())) continue;
                rituals.pending.add(new Pending(kind, tag.getUUID("actor"), InteractionHand.valueOf(tag.getString("hand")),
                        Direction.valueOf(tag.getString("face")), Direction.valueOf(tag.getString("facing")), hit,
                        tag.getLong("due"), List.copyOf(sources), ItemStack.parseOptional(registries, tag.getCompound("payment"))));
            } catch (IllegalArgumentException error) {
                org.slf4j.LoggerFactory.getLogger(SalisMundusRituals.class).warn("Invalid saved dust ritual", error);
            }
        }
        return rituals;
    }

    @Override public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        ListTag jobs = new ListTag();
        for (Pending job : pending) {
            CompoundTag tag = new CompoundTag();
            tag.putString("kind", job.kind.name()); tag.putUUID("actor", job.actor);
            tag.putString("hand", job.hand.name()); tag.putString("face", job.face.name()); tag.putString("facing", job.facing.name());
            tag.putDouble("hitX", job.hit.x); tag.putDouble("hitY", job.hit.y); tag.putDouble("hitZ", job.hit.z);
            tag.putLong("due", job.due);
            tag.put("payment", job.payment.saveOptional(registries));
            ListTag sources = new ListTag();
            for (Source source : job.sources) {
                CompoundTag data = new CompoundTag(); data.putLong("pos", source.pos.asLong());
                data.put("state", NbtUtils.writeBlockState(source.state)); sources.add(data);
            }
            tag.put("sources", sources); jobs.add(tag);
        }
        root.put("pending", jobs);
        return root;
    }
}
