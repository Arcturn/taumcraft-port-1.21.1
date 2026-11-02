package thaumcraft.port.golem.seal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
public interface ISealEntity {
    void tickSealEntity(Level level);
    ISeal getSeal();
    SealPos getSealPos();
    byte getPriority();
    void setPriority(byte value);
    void readNBT(CompoundTag tag, HolderLookup.Provider registries);
    CompoundTag writeNBT(HolderLookup.Provider registries);
    void syncToClient(Level level);
    BlockPos getArea();
    void setArea(BlockPos area);
    boolean isLocked();
    void setLocked(boolean value);
    boolean isRedstoneSensitive();
    void setRedstoneSensitive(boolean value);
    String getOwner();
    void setOwner(String owner);
    byte getColor();
    void setColor(byte value);
    boolean isStoppedByRedstone(Level level);
}
