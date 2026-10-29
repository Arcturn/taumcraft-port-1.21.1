package thaumcraft.port.world;
import com.mojang.brigadier.arguments.FloatArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
public final class WorldCommands {
    public static void register(RegisterCommandsEvent e){
        var aura=Commands.literal("aura").executes(c->{
            var source=c.getSource();var a=AuraSystem.get(source.getLevel(),BlockPos.containing(source.getPosition()));
            if(a==null)return 0;source.sendSuccess(()->Component.literal("base="+a.base()+" vis="+a.vis()+" flux="+a.flux()),false);return 1;
        });
        for(boolean flux:new boolean[]{false,true})aura.then(Commands.literal(flux?"flux":"vis").requires(s->s.hasPermission(2)).then(Commands.argument("amount",FloatArgumentType.floatArg(-32766,32766)).executes(c->{
            var source=c.getSource();var p=BlockPos.containing(source.getPosition());float n=FloatArgumentType.getFloat(c,"amount");
            if(n<0)AuraSystem.drain(source.getLevel(),p,-n,flux,false);else AuraSystem.add(source.getLevel(),p,n,flux);
            source.sendSuccess(()->Component.literal("Aura adjusted: "+(flux?"flux":"vis")+" "+n),true);return 1;
        })));
        e.getDispatcher().register(Commands.literal("thaumcraft").then(aura));
    }
}
