package thaumcraft.port.golem.part;
import net.minecraft.resources.ResourceLocation;
/** Resource-only metadata: safe on dedicated servers, original OBJ groups are rendered client-side. */
public record PartModel(ResourceLocation objModel,ResourceLocation texture,EnumAttachPoint attachPoint){
 public ResourceLocation getObjModel(){return objModel;} public ResourceLocation getTexture(){return texture;} public EnumAttachPoint getAttachPoint(){return attachPoint;}
 public boolean useMaterialTextureForObjectPart(String part){return part.startsWith("bm");}
 public enum EnumAttachPoint{ARMS,LEGS,BODY,HEAD} public enum EnumLimbSide{LEFT,RIGHT,MIDDLE}
}
