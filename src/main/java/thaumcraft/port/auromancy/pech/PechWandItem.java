package thaumcraft.port.auromancy.pech;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.research.*;

/** Unusable Pech wand: a research curio, with its original gate and smaller knowledge grants. */
public final class PechWandItem extends Item {
    public PechWandItem(Properties properties){super(properties);}
    @Override public String getDescriptionId(){return "item.pech_wand.name";}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){lines.add(Component.translatable("item.curio.text"));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!(player instanceof ServerPlayer server))return InteractionResultHolder.pass(stack);
        var storage=ResearchStorage.get(server.getServer());var state=storage.player(server.getUUID());
        if(!state.isResearchKnown("BASEAUROMANCY")){server.sendSystemMessage(Component.translatable("not.pechwand").withStyle(ChatFormatting.RED));return InteractionResultHolder.pass(stack);}
        if(!player.hasInfiniteMaterials())stack.shrink(1);
        var random=player.getRandom();level.playSound(null,player.getX(),player.getY(),player.getZ(),PortSounds.get("learn"),SoundSource.NEUTRAL,.5f,.4f/(random.nextFloat()*.4f+.8f));
        if(!state.isResearchKnown("FOCUSPECH")){ResearchProgress.completeResearch(server,"FOCUSPECH");server.sendSystemMessage(Component.translatable("got.pechwand").withStyle(ChatFormatting.DARK_PURPLE));}
        var categories=ResearchCatalog.CATEGORIES.toArray(String[]::new);int observation=ResearchState.progression("OBSERVATION"),theory=ResearchState.progression("THEORY");
        state.addKnowledge("OBSERVATION",categories[random.nextInt(categories.length)],observation/3+random.nextInt(observation/2-observation/3+1));
        state.addKnowledge("THEORY",categories[random.nextInt(categories.length)],theory/5+random.nextInt(theory/4-theory/5+1));
        storage.setDirty();ResearchProgress.sync(server);player.awardStat(Stats.ITEM_USED.get(this));return InteractionResultHolder.pass(stack);
    }
}
