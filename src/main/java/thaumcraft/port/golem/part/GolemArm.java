package thaumcraft.port.golem.part;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import thaumcraft.port.golem.*;
public final class GolemArm {
 private static final List<GolemArm> VALUES=new ArrayList<>();
 public byte id; public final String key; public final String[] research; public final ResourceLocation icon;
 public final Object[] components; public final EnumGolemTrait[] traits; public final PartModel model;
 public GolemArm(String key,String[] research,ResourceLocation icon,PartModel model,Object[] components,EnumGolemTrait[] traits){this.key=key;this.research=research;this.icon=icon;this.model=model;this.components=components;this.traits=traits;}
 public static void register(GolemArm p){p.id=(byte)VALUES.size();VALUES.add(p);}
 public static GolemArm[] getArms(){GolemProperties.ensureInitialized();return VALUES.toArray(GolemArm[]::new);}
 public String getLocalizedName(){return Component.translatable("golem.arm."+key.toLowerCase(Locale.ROOT)).getString();}
 public String getLocalizedDescription(){return Component.translatable("golem.arm.text."+key.toLowerCase(Locale.ROOT)).getString();}
}
