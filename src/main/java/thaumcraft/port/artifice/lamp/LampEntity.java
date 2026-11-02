package thaumcraft.port.artifice.lamp;

import java.util.ArrayList;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.crafting.content.ArcaneLampBlock;
import thaumcraft.port.essentia.*;
import thaumcraft.port.infusion.tools.ElementalToolVisuals;

/** BETA26 lamp rates, suction and charge budgets, with modern random-tick crop updates. */
public final class LampEntity extends EssentiaEntity {
    public static final TagKey<Block> GROWTH_BLACKLIST = TagKey.create(Registries.BLOCK,ResourceLocation.parse("thaumcraft:lamp_growth_blacklist"));
    private int charges, drawDelay, animalTicks;
    private boolean reserve;
    private final ArrayList<BlockPos> columns = new ArrayList<>();
    private BlockPos pendingPlant;
    private BlockState pendingState;
    public LampEntity(BlockPos pos, BlockState state) { super(ArtificeLamps.ENTITY.get(),pos,state); charges = kind() == ArcaneLampBlock.Kind.GROWTH ? -1 : 0; }
    public ArcaneLampBlock.Kind kind() { return ((ArcaneLampBlock)getBlockState().getBlock()).kind; }
    public int charges() { return charges; }
    public boolean hasReserve() { return reserve; }
    public Direction inputFace() { return getBlockState().getValue(ArcaneLampBlock.FACING); }
    @Override public void tick() {
        if (!(level instanceof ServerLevel server)) return;
        if (kind() == ArcaneLampBlock.Kind.ARCANE) {
            if (level.getGameTime()%5 == 0 && !level.hasNeighborSignal(worldPosition)) spreadLight();
            return;
        }
        if (kind() == ArcaneLampBlock.Kind.GROWTH) {
            if (charges <= 0) {
                if (reserve) { charges = 20; reserve = false; sync(); }
                else if (drawEssentia()) { charges = 20; sync(); }
            }
            if (!reserve && drawEssentia()) { reserve = true; sync(); }
            updateLight();
            // Old scheduled updates use the same crop method as random updates. In
            // modern Minecraft the crop implementation lives in randomTick instead.
            if (pendingPlant != null) {
                if (level.hasChunkAt(pendingPlant) && level.getBlockState(pendingPlant).equals(pendingState)) {
                    pendingState.randomTick(server,pendingPlant,level.random);
                    if (!level.getBlockState(pendingPlant).equals(pendingState)) {
                        BlockPos delta = pendingPlant.subtract(worldPosition);
                        level.blockEvent(worldPosition,getBlockState().getBlock(),2,
                                ((delta.getX()+8)<<8)|((delta.getY()+8)<<4)|(delta.getZ()+8));
                    }
                }
                pendingPlant = null; pendingState = null;
            }
            if (!level.hasNeighborSignal(worldPosition) && charges > 0) growPlant();
        } else {
            if (charges < 10 && drawEssentia()) { charges++; sync(); }
            updateLight();
            if (!level.hasNeighborSignal(worldPosition) && charges > 1 && animalTicks++%300 == 0) breedAnimals();
        }
    }
    public void updateLight() {
        if (level == null || level.isClientSide) return;
        boolean lit = !level.hasNeighborSignal(worldPosition) && (kind() == ArcaneLampBlock.Kind.ARCANE || charges > (kind() == ArcaneLampBlock.Kind.FERTILITY ? 1 : 0));
        if (lit != getBlockState().getValue(ArcaneLampBlock.LIT)) {
            if (!lit) removeLights();
            level.setBlock(worldPosition,getBlockState().setValue(ArcaneLampBlock.LIT,lit),3);
        }
    }
    private void spreadLight() {
        var random=level.random;
        BlockPos target=worldPosition.offset(random.nextInt(16)-random.nextInt(16),random.nextInt(16)-random.nextInt(16),random.nextInt(16)-random.nextInt(16));
        if (!level.hasChunkAt(target)) return;
        int ceiling=level.getHeight(Heightmap.Types.MOTION_BLOCKING,target.getX(),target.getZ())+4;
        target=new BlockPos(target.getX(),Math.clamp(target.getY(),level.getMinBuildHeight()+5,Math.min(ceiling,level.getMaxBuildHeight()-1)),target.getZ());
        tryLight(target);
    }
    /** Separate from the random sampler for deterministic line-of-sight checks. */
    public boolean tryLight(BlockPos target) {
        if (level == null || level.isClientSide || kind()!=ArcaneLampBlock.Kind.ARCANE || level.hasNeighborSignal(worldPosition)
                || !level.hasChunkAt(target) || !level.isEmptyBlock(target) || level.getBlockState(target).is(ArtificeLamps.GLIMMER.get())
                || level.getBrightness(LightLayer.BLOCK,target)>=11 || Math.abs(target.getX()-worldPosition.getX())>15
                || Math.abs(target.getY()-worldPosition.getY())>15 || Math.abs(target.getZ()-worldPosition.getZ())>15) return false;
        var ray=level.clip(new ClipContext(Vec3.atCenterOf(worldPosition),Vec3.atCenterOf(target),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,CollisionContext.empty()) {
            @Override public VoxelShape getBlockShape(BlockState state, BlockGetter world, BlockPos pos) { return pos.equals(worldPosition)?Shapes.empty():super.getBlockShape(state,world,pos); }
        });
        if (ray.getType()!=HitResult.Type.MISS && !ray.getBlockPos().equals(target)) return false;
        return level.setBlock(target,ArtificeLamps.GLIMMER.get().defaultBlockState(),3);
    }
    public void removeLights() {
        if (level == null || level.isClientSide || kind()!=ArcaneLampBlock.Kind.ARCANE) return;
        for (BlockPos pos:BlockPos.betweenClosed(worldPosition.offset(-15,-15,-15),worldPosition.offset(15,15,15)))
            if (level.hasChunkAt(pos) && level.getBlockState(pos).is(ArtificeLamps.GLIMMER.get())) level.removeBlock(pos,false);
    }
    private boolean drawEssentia() {
        if (++drawDelay%5!=0) return false;
        Direction face=inputFace(), other=face.getOpposite();
        EssentiaTransport source=neighbour(face);
        return source!=null && source.connects(other) && source.output(other) && source.suction(other)<suction(face)
                && source.extract(suctionType(face),1,other,false)==1;
    }
    private void growPlant() {
        if (columns.isEmpty()) {
            for (int x=-6;x<=6;x++) for (int z=-6;z<=6;z++) columns.add(worldPosition.offset(x,6,z));
            for(int i=columns.size()-1;i>0;i--) { int j=level.random.nextInt(i+1); var value=columns.get(i); columns.set(i,columns.get(j)); columns.set(j,value); }
        }
        BlockPos column=columns.remove(columns.size()-1);
        for(int y=column.getY();y>=worldPosition.getY()-6;y--) {
            BlockPos pos=new BlockPos(column.getX(),y,column.getZ());
            if (worldPosition.distSqr(pos)>=36 || !level.hasChunkAt(pos) || !canGrow(pos)) continue;
            charges--; setChanged(); pendingPlant=pos; pendingState=level.getBlockState(pos); return;
        }
    }
    private boolean canGrow(BlockPos pos) {
        var state=level.getBlockState(pos); var block=state.getBlock();
        if (state.is(GROWTH_BLACKLIST) || block instanceof GrassBlock || block instanceof MyceliumBlock || !state.isRandomlyTicking()) return false;
        if (block instanceof SugarCaneBlock || block instanceof CactusBlock) return !level.getBlockState(pos.below()).is(block);
        if (block instanceof StemBlock) return true;
        if (block instanceof BonemealableBlock plant) return plant.isValidBonemealTarget(level,pos,state);
        return block instanceof BushBlock || block instanceof GrowingPlantBlock || block instanceof VineBlock;
    }
    private void breedAnimals() {
        var animals=level.getEntitiesOfClass(Animal.class,new AABB(worldPosition).inflate(7));
        for(var animal:animals) {
            if(animal.getAge()!=0 || animal.isInLove()) continue;
            var same=animals.stream().filter(other -> other.getClass()==animal.getClass()).toList();
            if(same.size()>9) continue;
            Animal partner=null;
            for(var other:same) {
                if(other.getAge()!=0 || other.isInLove()) continue;
                if(partner==null) partner=other;
                else {
                    // Source permits a pair with >1 charge, then deducts five. Preserve
                    // that small debt rather than accidentally making later pairs free.
                    charges-=5; other.setInLove(null); partner.setInLove(null); sync(); return;
                }
            }
        }
    }
    @Override public boolean connects(Direction side) { return kind()!=ArcaneLampBlock.Kind.ARCANE && side==inputFace(); }
    @Override public boolean input(Direction side) { return connects(side); }
    @Override public boolean output(Direction side) { return false; }
    @Override public Aspect aspect(Direction side) { return null; }
    @Override public int amount(Direction side) { return 0; }
    @Override public Aspect suctionType(Direction side) { return kind()==ArcaneLampBlock.Kind.GROWTH?Aspect.PLANT:kind()==ArcaneLampBlock.Kind.FERTILITY?Aspect.DESIRE:null; }
    @Override public int suction(Direction side) { return !connects(side)?0:kind()==ArcaneLampBlock.Kind.GROWTH?(!reserve || charges<=0?128:0):128-charges*10; }
    @Override public int minimumSuction() { return 0; }
    @Override public int insert(Aspect aspect,int amount,Direction side,boolean simulate) { return 0; }
    @Override public int extract(Aspect aspect,int amount,Direction side,boolean simulate) { return 0; }
    @Override public boolean triggerEvent(int event,int data) {
        if(event==2) { if(level!=null && level.isClientSide) ElementalToolVisuals.growth.accept(worldPosition.offset(((data>>8)&15)-8,((data>>4)&15)-8,(data&15)-8)); return true; }
        return super.triggerEvent(event,data);
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries); tag.putInt("charges",charges); tag.putBoolean("reserve",reserve); tag.putInt("AnimalTicks",animalTicks);
        if(pendingPlant!=null && pendingState!=null) { tag.putLong("PendingPlant",pendingPlant.asLong()); tag.put("PendingState",NbtUtils.writeBlockState(pendingState)); }
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries); charges=Math.clamp(tag.getInt("charges"),kind()==ArcaneLampBlock.Kind.GROWTH?-1:-3,kind()==ArcaneLampBlock.Kind.GROWTH?20:10);
        reserve=tag.getBoolean("reserve"); animalTicks=Math.max(0,tag.getInt("AnimalTicks")); columns.clear(); pendingPlant=null; pendingState=null;
        if(kind()==ArcaneLampBlock.Kind.GROWTH && tag.contains("PendingPlant") && tag.contains("PendingState")) {
            BlockPos candidate=BlockPos.of(tag.getLong("PendingPlant"));
            if(worldPosition.distSqr(candidate)<36) { pendingPlant=candidate; pendingState=NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK),tag.getCompound("PendingState")); }
        }
    }
}
