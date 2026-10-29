package thaumcraft.port.theory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import thaumcraft.port.research.*;
import thaumcraft.port.content.PortSounds;

/** Curio research consumables; unrelated to the Curios equipment API. */
public final class ResearchCurioItem extends Item {
    public final String kind;
    public ResearchCurioItem(Properties properties,String kind){super(properties);this.kind=kind;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(player instanceof ServerPlayer sp){
            var storage=ResearchStorage.get(sp.getServer());var state=storage.player(sp.getUUID());int type=TheoryCards.CURIOS.indexOf(kind);var r=sp.getRandom();
            level.playSound(null,player.blockPosition(),PortSounds.get("learn"),net.minecraft.sounds.SoundSource.PLAYERS,.5f,.4f/(r.nextFloat()*.4f+.8f));
            if(type==6&&state.normalWarp()+state.permanentWarp()<=20){sp.displayClientMessage(Component.translatable("fail.crimsonrites").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE),false);return InteractionResultHolder.fail(stack);}
            String category=TheoryCards.CURIO_CATEGORIES.get(type);
            state.addKnowledge("OBSERVATION",category,8+r.nextInt(9));state.addKnowledge("THEORY",category,10+r.nextInt(7));
            if(type==3||type==6)state.warp(state.permanentWarp()+(type==6&&r.nextBoolean()?1:0),state.normalWarp()+1,state.temporaryWarp()+5);
            if(type==6){var e=ResearchCatalog.get("CrimsonRites");if(e!=null)state.stage(e.key(),e.stages().size()+1);}
            state.addKnowledge("OBSERVATION",TheorySession.CATEGORIES.get(r.nextInt(7)),8+r.nextInt(9));state.addKnowledge("THEORY",TheorySession.CATEGORIES.get(r.nextInt(7)),10+r.nextInt(7));
            if(!player.hasInfiniteMaterials())stack.shrink(1);storage.setDirty();ResearchProgress.sync(sp);
            sp.displayClientMessage(Component.translatable("tc.knowledge.gained").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE),false);
        }
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,java.util.List<Component> lines,TooltipFlag flag){lines.add(Component.translatable("item.curio.text"));}
}
