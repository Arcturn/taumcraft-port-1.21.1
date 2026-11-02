package thaumcraft.port.golem.part;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import thaumcraft.port.golem.*;
public final class GolemHead {
 private static final List<GolemHead> VALUES=new ArrayList<>();
 public byte id; public final String key; public final String[] research; public final ResourceLocation icon;
 public final Object[] components; public final EnumGolemTrait[] traits; public final PartModel model;
 public GolemHead(String key,String[] research,ResourceLocation icon,PartModel model,Object[] components,EnumGolemTrait[] traits){this.key=key;this.research=research;this.icon=icon;this.model=model;this.components=components;this.traits=traits;}
 public static void register(GolemHead p){p.id=(byte)VALUES.size();VALUES.add(p);}
 public static GolemHead[] getHeads(){GolemProperties.ensureInitialized();return VALUES.toArray(GolemHead[]::new);}
 public String getLocalizedName(){return Component.translatable("golem.head."+key.toLowerCase(Locale.ROOT)).getString();}
 public String getLocalizedDescription(){return Component.translatable("golem.head.text."+key.toLowerCase(Locale.ROOT)).getString();}
}
