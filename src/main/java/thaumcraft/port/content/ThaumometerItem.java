package thaumcraft.port.content;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import thaumcraft.port.research.Scanner;
/** Immediate local feedback and an authoritative server scan, as in ItemThaumometer. */
public final class ThaumometerItem extends Item {
    public ThaumometerItem(Properties properties){super(properties.stacksTo(1));}
    @Override public boolean doesSneakBypassUse(ItemStack stack,net.minecraft.world.level.LevelReader level,net.minecraft.core.BlockPos pos,Player player){
        // Preserve the port's temporary wrench gestures until the caster gauntlet is available.
        var block=level.getBlockState(pos).getBlock();return block instanceof thaumcraft.port.essentia.TubeBlock||block instanceof thaumcraft.port.machine.CrucibleBlock;
    }
    public static void scan(Player p){
        if(p.level().isClientSide)thaumcraft.port.client.ClientScanner.activate();
        else if(p instanceof ServerPlayer s)Scanner.use(s);
    }
    // Vanilla first lets blocks/entities consume the interaction (containers, mounts,
    // item frames), then falls back to this ordinary item use, exactly as TC6 did.
    @Override public InteractionResultHolder<ItemStack> use(Level l,Player p,InteractionHand h){
        scan(p);
        // 1.21 SUCCESS also swings the arm; TC6's 1.12 SUCCESS did not swing on
        // ordinary item use. CONSUME retains that original upright scanner pose.
        return InteractionResultHolder.consume(p.getItemInHand(h));
    }
}
