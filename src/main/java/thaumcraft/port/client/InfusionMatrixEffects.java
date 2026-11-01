package thaumcraft.port.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import java.util.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import thaumcraft.port.content.PortSounds;
import thaumcraft.port.infusion.matrix.InfusionMatrixEntity;
import thaumcraft.port.infusion.matrix.MatrixEffectPayload;
import thaumcraft.port.infusion.structure.PedestalEntity;

/** Client-only original ritual sounds, runes, FXBore attraction particles and FXArc ribbons. */
public final class InfusionMatrixEffects {
    private static final Map<InfusionMatrixEntity,Map<Source,Integer>> SOURCES=new WeakHashMap<>();
    private record Source(BlockPos pos,int entity){}
    public static void receive(MatrixEffectPayload payload){
        var mc=Minecraft.getInstance();var level=mc.level;
        if(level==null||mc.player==null||!payload.valid()||mc.player.distanceToSqr(Vec3.atCenterOf(payload.matrix()))>64*64)return;
        if(payload.kind()==MatrixEffectPayload.ARC){
            mc.particleEngine.add(new Arc(level,payload.source(),Vec3.atCenterOf(payload.matrix())));return;
        }
        if(!(level.getBlockEntity(payload.matrix()) instanceof InfusionMatrixEntity matrix))return;
        if(payload.kind()==MatrixEffectPayload.SOURCE){BlockPos source=BlockPos.containing(payload.source());SOURCES.computeIfAbsent(matrix,key->new HashMap<>()).put(new Source(source,payload.entity()),level.getBlockEntity(source) instanceof PedestalEntity?60:15);}
    }
    public static void tick(InfusionMatrixEntity matrix){
        var mc=Minecraft.getInstance();if(!(matrix.getLevel() instanceof ClientLevel level))return;
        BlockPos pos=matrix.getBlockPos();var random=level.random;
        if(matrix.crafting){
            if(matrix.craftCount==0||matrix.craftCount%65==0)level.playLocalSound(pos.getX(),pos.getY(),pos.getZ(),PortSounds.get(matrix.craftCount==0?"infuserstart":"infuser"),SoundSource.BLOCKS,.5f,1,false);
            matrix.craftCount++;mc.particleEngine.add(new Rune(level,pos.getX()+.5,pos.getY()-1.5,pos.getZ()+.5));
        }else if(matrix.craftCount>0)matrix.craftCount=Math.min(50,Math.max(0,matrix.craftCount-2));
        if(matrix.active&&matrix.startUp<1){matrix.startUp+=Math.max(matrix.startUp/10,.001f);if(matrix.startUp>.999)matrix.startUp=1;}
        if(!matrix.active&&matrix.startUp>0){matrix.startUp-=matrix.startUp/10;if(matrix.startUp<.001)matrix.startUp=0;}
        var sources=SOURCES.get(matrix);
        if(sources!=null)for(var iterator=sources.entrySet().iterator();iterator.hasNext();){
            var entry=iterator.next();var source=entry.getKey();if(entry.getValue()<=0){iterator.remove();continue;}
            Vec3 target=new Vec3(pos.getX()+.5,pos.getY()-.5,pos.getZ()+.5);
            if(source.pos.equals(pos)){
                var entity=level.getEntity(source.entity);
                if(entity!=null)for(int i=0;i<4;i++)mc.particleEngine.add(new Bore(level,new Vec3(entity.getX()+(random.nextFloat()-random.nextFloat())*entity.getBbWidth(),entity.getY()+random.nextFloat()*entity.getBbHeight(),entity.getZ()+(random.nextFloat()-random.nextFloat())*entity.getBbWidth()),target,ItemStack.EMPTY,true));
            }else if(level.getBlockEntity(source.pos) instanceof PedestalEntity pedestal){
                ItemStack stack=pedestal.getItem(0);
                if(!stack.isEmpty()){
                    if(random.nextInt(3)==0)mc.particleEngine.add(new Bore(level,new Vec3(source.pos.getX()+random.nextFloat(),source.pos.getY()+1+random.nextFloat(),source.pos.getZ()+random.nextFloat()),target,ItemStack.EMPTY,false));
                    else for(int i=0;i<4;i++){
                        boolean block=stack.getItem() instanceof BlockItem;
                        Vec3 start=block?new Vec3(source.pos.getX()+random.nextFloat(),source.pos.getY()+1+random.nextFloat(),source.pos.getZ()+random.nextFloat()):new Vec3(source.pos.getX()+.4+random.nextFloat()*.2,source.pos.getY()+1.23+random.nextFloat()*.2,source.pos.getZ()+.4+random.nextFloat()*.2);
                        mc.particleEngine.add(new Bore(level,start,target,stack,false));
                    }
                }
            }else{iterator.remove();continue;}
            entry.setValue(entry.getValue()-1);
        }
        if(matrix.crafting&&matrix.stability<0&&random.nextInt(250)<=Math.abs(matrix.stability))mc.particleEngine.add(new Spark(level,Vec3.atLowerCornerOf(pos).add(random.nextFloat(),random.nextFloat(),random.nextFloat()),3+random.nextFloat()*2));
        if(matrix.active&&!matrix.problemBlocks.isEmpty()&&random.nextInt(25)==0){var problem=matrix.problemBlocks.get(random.nextInt(matrix.problemBlocks.size()));mc.particleEngine.add(new Spark(level,Vec3.atLowerCornerOf(problem).add(random.nextFloat(),random.nextFloat(),random.nextFloat()),2+random.nextFloat()));}
    }
    private static final class Rune extends TextureSheetParticle {
        private final int glyph;private final Quaternionf rotation;
        Rune(ClientLevel level,double x,double y,double z){
            super(level,x,y,z);rotation=new Quaternionf().rotateY(random.nextInt(4)*(float)Math.PI/2).rotateZ((float)Math.PI/2);glyph=random.nextInt(16);
            var offset=new Vector3f(random.nextFloat()*.2f,-.3f+random.nextFloat()*.6f,-.51f).rotate(rotation);setPos(x+offset.x,y+offset.y,z+offset.z);xo=this.x;yo=this.y;zo=this.z;
            xd=yd=zd=0;hasPhysics=false;lifetime=75;quadSize=.15f*(1+(float)random.nextGaussian()*.1f);alpha=0;
            rCol=.5f+random.nextFloat()*.2f;gCol=.1f;bCol=.7f+random.nextFloat()*.3f;setSize(.01f,.01f);
        }
        @Override public void tick(){xo=x;yo=y;zo=z;alpha=ScannerVisualRules.runeOpacity(age,lifetime);if(age++>=lifetime)remove();yd+=.04*.03;setPos(x,y+yd,z);}
        @Override public void render(VertexConsumer buffer,Camera camera,float partial){var origin=camera.getPosition();ScanRuneParticle.emitQuad(buffer,rotation,(float)(Mth.lerp(partial,xo,x)-origin.x),(float)(Mth.lerp(partial,yo,y)-origin.y),(float)(Mth.lerp(partial,zo,z)-origin.z),quadSize,glyph/64f,(glyph+1)/64f,6/64f,7/64f,rCol,gCol,bCol,alpha);}
        @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.scannerGlowLayer();}
    }
    private static final class Bore extends TextureSheetParticle {
        private final Vec3 target;private final boolean sparkle;private final float partU,partV;
        Bore(ClientLevel level,Vec3 start,Vec3 target,ItemStack item,boolean experience){
            super(level,start.x,start.y,start.z);this.target=target;sparkle=item.isEmpty();hasPhysics=true;
            int base=Math.max(1,(int)(start.distanceTo(target)*10));lifetime=base/2+random.nextInt(base);xd=random.nextGaussian()*.01;yd=random.nextGaussian()*.01;zd=random.nextGaussian()*.01;
            partU=random.nextFloat()*3;partV=random.nextFloat()*3;
            if(sparkle){quadSize=.1f*(random.nextFloat()*.5f+.5f);rCol=experience?.2f:.4f+random.nextFloat()*.2f;gCol=experience?.6f+random.nextFloat()*.3f:.2f;bCol=experience?.3f:.6f+random.nextFloat()*.3f;alpha=1;}
            else{
                // The source's setAlphaF(.3) is ignored by FXBoreParticles' hardcoded alpha=1;
                // it does not scale the fragments, whose original half-size is .04..07.
                quadSize=.1f*(random.nextFloat()*.3f+.4f);
                setSprite(Minecraft.getInstance().getItemRenderer().getModel(item,level,null,0).getParticleIcon());int colour=item.getItem() instanceof BlockItem?-1:Minecraft.getInstance().getItemColors().getColor(item,0);
                rCol=.6f*((colour>>16)&255)/255f;gCol=.6f*((colour>>8)&255)/255f;bCol=.6f*(colour&255)/255f;
                if(!(item.getItem() instanceof BlockItem)){xd+=random.nextGaussian()*.03;yd+=random.nextGaussian()*.03;zd+=random.nextGaussian()*.03;}
            }
            setSize(.01f,.01f);
        }
        @Override public void tick(){
            xo=x;yo=y;zo=z;
            if(age++>=lifetime||BlockPos.containing(x,y,z).equals(BlockPos.containing(target))){remove();return;}
            move(xd,yd,zd);xd*=.985;yd*=.95;zd*=.985;
            Vec3 difference=target.subtract(x,y,z);double distance=difference.length();if(distance<1e-6){remove();return;}
            double clamp=Math.min(.25,distance/15),noise=sparkle?.01:.005;if(distance<2)quadSize*=.9f;
            xd=Mth.clamp((float)(xd+difference.x/distance*clamp),-clamp,clamp)+random.nextGaussian()*noise;
            yd=Mth.clamp((float)(yd+difference.y/distance*clamp),-clamp,clamp)+random.nextGaussian()*noise;
            zd=Mth.clamp((float)(zd+difference.z/distance*clamp),-clamp,clamp)+random.nextGaussian()*noise;
        }
        @Override public float getQuadSize(float partial){return sparkle?quadSize*(Mth.sin(age/3f)*.5f+1):quadSize;}
        @Override protected float getU0(){return sparkle?(age%4)/64f:sprite.getU(partU/4);}
        @Override protected float getU1(){return sparkle?(age%4+1)/64f:sprite.getU((partU+1)/4);}
        @Override protected float getV0(){return sparkle?4/64f:sprite.getV(partV/4);}
        @Override protected float getV1(){return sparkle?5/64f:sprite.getV((partV+1)/4);}
        @Override protected int getLightColor(float partial){return sparkle?0x00F00000:super.getLightColor(partial);}
        @Override public ParticleRenderType getRenderType(){return sparkle?SalisMundusParticle.scannerGlowLayer():ParticleRenderType.TERRAIN_SHEET;}
    }
    private static final class Spark extends TextureSheetParticle {
        private final int first;private final boolean flipped;
        Spark(ClientLevel level,Vec3 position,float size){super(level,position.x,position.y,position.z);lifetime=5+random.nextInt(5);first=8+random.nextInt(3)*16;flipped=random.nextBoolean();quadSize=size*.1f;rCol=.7f+random.nextFloat()*.1f;gCol=.1f;bCol=.65f+random.nextFloat()*.1f;alpha=.8f;hasPhysics=false;xd=yd=zd=0;}
        private int frame(){return first+Math.min(7,age*8/lifetime);}
        @Override protected float getU0(){return ((frame()%16)+(flipped?1:0))/16f;}@Override protected float getU1(){return ((frame()%16)+(flipped?0:1))/16f;}
        @Override protected float getV0(){return (frame()/16)/16f;}@Override protected float getV1(){return (frame()/16+1)/16f;}
        @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.scannerGlowLayer();}
    }
    private static final class ArcEmber extends TextureSheetParticle {
        private final int first;private final float[] opacity;private final float red,green,blue,endRed,endGreen,endBlue;private int delay;
        ArcEmber(ClientLevel level,Vec3 position,float red,float green,float blue){
            super(level,position.x,position.y,position.z);setSize(.1f,.1f);xd=yd=zd=0;lifetime=30+random.nextInt(20);delay=2+random.nextInt(3);
            this.red=Math.min(1,red*3);this.green=Math.min(1,green*3);this.blue=Math.min(1,blue*3);endRed=random.nextFloat();endGreen=random.nextFloat();endBlue=random.nextFloat();
            opacity=new float[6+random.nextInt(lifetime/3)];for(int i=1;i<opacity.length-1;i++)opacity[i]=random.nextFloat();opacity[0]=1;
            boolean spark=random.nextFloat()<.2f;first=spark?320:512;gravity=spark?0:.125f;quadSize=.05f;alpha=0;hasPhysics=true;
        }
        @Override public void tick(){
            xo=x;yo=y;zo=z;if(delay>0){delay--;return;}if(age++>=lifetime){remove();return;}
            yd-=.04*gravity;move(xd,yd,zd);xd=xd*.995+random.nextGaussian()*.0025;yd=yd*.995+random.nextGaussian()*.001;zd=zd*.995+random.nextGaussian()*.0025;
            if(onGround){xd*=.7;zd*=.7;}
            float fraction=Math.min(1,age/(float)lifetime),key=fraction*(opacity.length-1);int low=Math.min(opacity.length-1,(int)key);
            alpha=Mth.lerp(key-low,opacity[low],opacity[Math.min(opacity.length-1,low+1)]);quadSize=Mth.lerp(fraction,.05f,.0125f);rCol=Mth.lerp(fraction,red,endRed);gCol=Mth.lerp(fraction,green,endGreen);bCol=Mth.lerp(fraction,blue,endBlue);
        }
        private int frame(){return first+age%16;}
        @Override protected float getU0(){return (frame()%64)/64f;}@Override protected float getU1(){return (frame()%64+1)/64f;}
        @Override protected float getV0(){return (frame()/64)/64f;}@Override protected float getV1(){return (frame()/64+1)/64f;}
        @Override public ParticleRenderType getRenderType(){return SalisMundusParticle.scannerGlowLayer();}
    }
    private static final ResourceLocation BEAM=ResourceLocation.parse("thaumcraft:textures/misc/beamh.png");
    private static final ParticleRenderType ARC_LAYER=new ParticleRenderType(){
        @Override public BufferBuilder begin(Tesselator tessellator,TextureManager textures){RenderSystem.depthMask(false);RenderSystem.enableBlend();RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,GlStateManager.DestFactor.ONE);RenderSystem.disableCull();RenderSystem.setShader(GameRenderer::getParticleShader);RenderSystem.setShaderTexture(0,BEAM);return tessellator.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE);}
        @Override public String toString(){return "THAUMCRAFT_MATRIX_ARC";}
    };
    private static final class Arc extends Particle {
        private final List<Vec3> points=new ArrayList<>();private final float length;
        Arc(ClientLevel level,Vec3 start,Vec3 end){
            super(level,start.x,start.y,start.z);lifetime=3;hasPhysics=false;rCol=.3f-random.nextFloat()*.1f;gCol=0;bCol=.3f-random.nextFloat()*.1f;
            Vec3 target=end.subtract(start);length=(float)Math.max(.001,target.length());Vec3 velocity=velocity(target),current=Vec3.ZERO;double step=velocity.lengthSqr();points.add(start);
            for(int n=0;current.distanceToSqr(target)>step&&n<50;n++){
                current=current.add(velocity);Vec3 point=start.add(current).add((random.nextDouble()-random.nextDouble())*.25,(random.nextDouble()-random.nextDouble())*.25,(random.nextDouble()-random.nextDouble())*.25);points.add(point);
                Minecraft.getInstance().particleEngine.add(new ArcEmber(level,point,rCol,gCol,bCol));
                velocity=velocity.subtract(0,.115/1.9,0);
            }
            points.add(end);setBoundingBox(new AABB(start,end).inflate(2));
        }
        private static Vec3 velocity(Vec3 target){
            double horizontal=Math.sqrt(target.x*target.x+target.z*target.z);if(horizontal<.00001)return target.normalize().scale(.25);
            double gain=Math.max(.5,target.y+.5),a=-horizontal*horizontal/(4*gain),slope=-horizontal/(2*a)-Math.sqrt(horizontal*horizontal+4*a*target.y)/(2*a),vy=Math.sqrt(gain*.115),vh=vy/slope;
            return new Vec3(vh*target.x/horizontal,vy,vh*target.z/horizontal);
        }
        @Override public void tick(){if(age++>=lifetime)remove();}
        @Override public void render(VertexConsumer buffer,Camera camera,float partial){
            float alpha=Math.max(0,1-(age+partial)/lifetime);Vec3 eye=camera.getPosition();
            for(int ribbon=0;ribbon<2;ribbon++)for(int n=0;n<points.size()-1;n++){
                Vec3 a=points.get(n).subtract(eye),b=points.get(n+1).subtract(eye),offset=ribbon==0?new Vec3(0,.125,0):new Vec3(.125,0,.125);
                vertex(buffer,a.subtract(offset),n/length,1,alpha);vertex(buffer,a.add(offset),n/length,0,alpha);vertex(buffer,b.add(offset),(n+1)/length,0,alpha);vertex(buffer,b.subtract(offset),(n+1)/length,1,alpha);
            }
        }
        private void vertex(VertexConsumer buffer,Vec3 point,float u,float v,float alpha){buffer.addVertex((float)point.x,(float)point.y,(float)point.z).setUv(u,v).setColor(rCol,gCol,bCol,alpha).setLight(0x00DC0000);}
        @Override public ParticleRenderType getRenderType(){return ARC_LAYER;}
    }
    private InfusionMatrixEffects(){}
}
