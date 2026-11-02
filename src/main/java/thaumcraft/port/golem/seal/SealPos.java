package thaumcraft.port.golem.seal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import java.util.Objects;
public final class SealPos {
    public final BlockPos pos;
    public final Direction face;
    public SealPos(BlockPos pos, Direction face) { this.pos=Objects.requireNonNull(pos).immutable(); this.face=Objects.requireNonNull(face); }
    @Override public boolean equals(Object other) { return other instanceof SealPos p && pos.equals(p.pos) && face==p.face; }
    @Override public int hashCode() { return 31*pos.hashCode()+face.ordinal(); }
    @Override public String toString() { return pos.toShortString()+"/"+face.getName(); }
}
