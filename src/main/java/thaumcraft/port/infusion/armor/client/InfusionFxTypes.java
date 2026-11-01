package thaumcraft.port.infusion.armor.client;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
final class InfusionFxTypes extends RenderType {
    static final RenderType SOUNDING=make("sounding",ResourceLocation.parse("thaumcraft:textures/misc/particles.png"),true,false);
    static final RenderType SOUNDING_DARK=create("thaumcraft_sounding_dark",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,1536,false,true,CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER).setTextureState(new TextureStateShard(ResourceLocation.parse("thaumcraft:textures/misc/particles.png"),true,false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(NO_DEPTH_TEST).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    static final RenderType SLASH=make("slash",ResourceLocation.parse("thaumcraft:textures/misc/particles.png"),false,false);
    static final RenderType[] SHIELD=new RenderType[15];
    static{for(int i=0;i<15;i++)SHIELD[i]=make("shield_"+i,ResourceLocation.parse("thaumcraft:textures/models/hemis"+(i+1)+".png"),false,true);}
    private static RenderType make(String id,ResourceLocation texture,boolean through,boolean triangles){return create("thaumcraft_"+id,DefaultVertexFormat.NEW_ENTITY,triangles?VertexFormat.Mode.TRIANGLES:VertexFormat.Mode.QUADS,1536,false,true,CompositeState.builder().setShaderState(RENDERTYPE_EYES_SHADER).setTextureState(new TextureStateShard(texture,true,false)).setTransparencyState(LIGHTNING_TRANSPARENCY).setDepthTestState(through?NO_DEPTH_TEST:LEQUAL_DEPTH_TEST).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE).createCompositeState(false));}
    private InfusionFxTypes(){super("unused",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,0,false,false,()->{},()->{});}
}
