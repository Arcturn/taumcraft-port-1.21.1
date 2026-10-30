package thaumcraft.port.research;

import com.mojang.brigadier.arguments.*;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ResearchCommands {
    public static void register(RegisterCommandsEvent event){
        var root=Commands.literal("research").executes(c->{
            var p=c.getSource().getPlayerOrException();var s=ResearchStorage.get(p.getServer()).player(p.getUUID());
            c.getSource().sendSuccess(()->Component.literal("Stages: "+s.stages()+"\nKnowledge: "+s.knowledge()+"\nWarp: "+s.permanentWarp()+"/"+s.normalWarp()+"/"+s.temporaryWarp()),false);return 1;
        });
        root.then(Commands.literal("grant").requires(s->s.hasPermission(2))
            .then(Commands.literal("all").executes(c->{
                var p=c.getSource().getPlayerOrException();var storage=ResearchStorage.get(p.getServer());
                int count=storage.player(p.getUUID()).completeCatalog();if(count>0)storage.setDirty();ResearchProgress.sync(p);
                c.getSource().sendSuccess(()->Component.literal("Research completed: "+count+"; catalog total: "+ResearchCatalog.all().size()),false);return count;
            }))
            .then(Commands.argument("key",StringArgumentType.word())
            .suggests((c,b)->SharedSuggestionProvider.suggest(ResearchCatalog.all().stream().map(ResearchCatalog.Entry::key),b)).executes(c->{
                var entry=ResearchCatalog.get(StringArgumentType.getString(c,"key"));if(entry==null){c.getSource().sendFailure(Component.literal("Unknown research"));return 0;}
                var p=c.getSource().getPlayerOrException();var storage=ResearchStorage.get(p.getServer());storage.player(p.getUUID()).stage(entry.key(),entry.stages().size()+1);storage.setDirty();
                c.getSource().sendSuccess(()->Component.literal("Granted: "+entry.key()),true);return 1;
            })));
        var knowledge=Commands.literal("knowledge").requires(s->s.hasPermission(2));
        for(String type:java.util.List.of("OBSERVATION","THEORY"))knowledge.then(Commands.literal(type.toLowerCase(java.util.Locale.ROOT)).then(Commands.argument("category",StringArgumentType.word())
            .suggests((c,b)->SharedSuggestionProvider.suggest(ResearchCatalog.CATEGORIES,b)).then(Commands.argument("raw",IntegerArgumentType.integer(1,1000000)).executes(c->{
                var p=c.getSource().getPlayerOrException();var storage=ResearchStorage.get(p.getServer());
                boolean ok=storage.player(p.getUUID()).addKnowledge(type,StringArgumentType.getString(c,"category").toUpperCase(java.util.Locale.ROOT),IntegerArgumentType.getInteger(c,"raw"));
                if(ok){storage.setDirty();ResearchProgress.sync(p);c.getSource().sendSuccess(()->Component.literal("Knowledge added"),true);return 1;}
                c.getSource().sendFailure(Component.literal("Invalid category or knowledge limit exceeded"));return 0;
            }))));
        root.then(knowledge);event.getDispatcher().register(Commands.literal("thaumcraft").then(root));
    }
    private ResearchCommands(){}
}
