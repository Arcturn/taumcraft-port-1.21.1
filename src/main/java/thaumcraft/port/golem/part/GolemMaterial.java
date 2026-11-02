package thaumcraft.port.golem.part;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import thaumcraft.port.golem.*;
public final class GolemMaterial {
 private static final List<GolemMaterial> VALUES=new ArrayList<>();
 public byte id; public final String key; public final String[] research; public final ResourceLocation texture;
 public final int itemColor,healthMod,armor,damage;public final ItemStack componentBase,componentMechanism;public final EnumGolemTrait[] traits;
 public GolemMaterial(String key,String[] research,ResourceLocation texture,int color,int health,int armor,int damage,ItemStack base,ItemStack mechanism,EnumGolemTrait[] traits){this.key=key;this.research=research;this.texture=texture;itemColor=color;healthMod=health;this.armor=armor;this.damage=damage;componentBase=base;componentMechanism=mechanism;this.traits=traits;}
 public static void register(GolemMaterial p){p.id=(byte)VALUES.size();VALUES.add(p);}
 public static GolemMaterial[] getMaterials(){GolemProperties.ensureInitialized();return VALUES.toArray(GolemMaterial[]::new);}
 public String getLocalizedName(){return Component.translatable("golem.material."+key.toLowerCase(Locale.ROOT)).getString();}
 public String getLocalizedDescription(){return Component.translatable("golem.material.text."+key.toLowerCase(Locale.ROOT)).getString();}
}
