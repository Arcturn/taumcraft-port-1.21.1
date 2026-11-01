package thaumcraft.port.essentia;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;

/** BETA26 ItemResonator: diagnostic use takes priority over machine GUI interactions. */
public final class ResonatorItem extends Item {
    public ResonatorItem(Properties properties){super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));}
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context){return diagnose(context);}
    @Override public InteractionResult useOn(UseOnContext context){return diagnose(context);}
    private InteractionResult diagnose(UseOnContext context){
        var player=context.getPlayer();var level=context.getLevel();var pos=context.getClickedPos();
        var transport=EssentiaEntity.at(level,pos,context.getClickedFace());
        if(player==null||transport==null)return InteractionResult.PASS;
        Direction side=side(context);
        if(!level.isClientSide){
            for(Component line:readout(transport,side))player.displayClientMessage(line,false);
            if(transport instanceof CondenserReadout condenser){
                player.displayClientMessage(Component.translatable("tc.condenser1",condenser.essentiaCost()),false);
                player.displayClientMessage(Component.translatable("tc.condenser2",condenser.cycleTicks(),condenser.cycleTicks()/20),false);
            }
            level.playSound(null,pos,SoundEvents.SHIELD_BLOCK,SoundSource.BLOCKS,.5f,1.9f+level.random.nextFloat()*.1f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    private static Direction side(UseOnContext context){
        // The hit face on the side of a thin arm is not that arm's transport direction.
        if(!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof TubeEntity))return context.getClickedFace();
        var hit=context.getClickLocation().subtract(context.getClickedPos().getX()+.5,context.getClickedPos().getY()+.5,context.getClickedPos().getZ()+.5);
        double ax=Math.abs(hit.x),ay=Math.abs(hit.y),az=Math.abs(hit.z);
        if(Math.max(ax,Math.max(ay,az))<=.25)return context.getClickedFace();
        if(ay>=ax&&ay>=az)return hit.y<0?Direction.DOWN:Direction.UP;
        return ax>=az?(hit.x<0?Direction.WEST:Direction.EAST):(hit.z<0?Direction.NORTH:Direction.SOUTH);
    }
    public static List<Component> readout(EssentiaTransport transport,Direction side){
        List<Component> lines=new ArrayList<>();
        if(transport instanceof BufferEntity buffer)buffer.contents.values().entrySet().stream().sorted(Comparator.comparing(entry->entry.getKey().displayName())).forEach(entry->lines.add(Component.translatable("tc.resonator1",entry.getValue(),entry.getKey().displayName())));
        else if(transport.aspect(side)!=null)lines.add(Component.translatable("tc.resonator1",transport.amount(side),transport.aspect(side).displayName()));
        Object type=transport.suctionType(side)==null?Component.translatable("tc.resonator3"):transport.suctionType(side).displayName();
        lines.add(Component.translatable("tc.resonator2",transport.suction(side),type));
        return List.copyOf(lines);
    }
    /** Implemented by the flux condenser without coupling this item to its registry. */
    public interface CondenserReadout {int essentiaCost();int cycleTicks();}
}
