package thaumcraft.port.client;

import net.minecraft.util.RandomSource;

/** BETA26 drawNitorCore/drawNitorFlames and FXGeneric's tick-indexed scale keys. */
final class NitorVisualRules {
    static final int GRID=64, CORE_FRAME=457, FLAME_FRAME=264, FLAME_FRAMES=8;
    static final double SLOWDOWN=(double).98f;
    static final float CORE_MOVEMENT=.0002f, FLAME_MOVEMENT=.0025f;

    record Preset(int lifetime,float alpha,boolean additive,float[] scaleFrames) {}
    static Preset preset(RandomSource random,boolean core){
        if(core)return new Preset(10,1,false,scaleFrames(10,1,1+(float)random.nextGaussian()*.1f,1));
        int lifetime=10+random.nextInt(5);
        return new Preset(lifetime,.66f,true,scaleFrames(lifetime,3+random.nextFloat(),.05f));
    }
    static float[] scaleFrames(int lifetime,float...keys){
        float[] frames=new float[lifetime+1];
        float increment=(keys.length-1)/(float)lifetime,position=0;
        for(int age=0;age<=lifetime;age++){
            int index=Math.min(keys.length-1,(int)Math.floor(position));
            float difference=index<keys.length-1?keys[index+1]-keys[index]:0;
            // FXGeneric precomputes unscaled keys, then multiplies the selected key by .1 on render.
            frames[age]=.1f*(keys[index]+difference*(position-index));
            position+=increment;
        }
        return frames;
    }
    // Core is inserted after the legacy START tick; delayed flames enter on the next START.
    // Both remain invisible until FXGeneric calculates its frame arrays in the first tick.
    static float size(float[] frames,int age){return age<=0?0:frames[Math.min(age,frames.length-1)];}
    static int frame(boolean core,int age){return core?CORE_FRAME:FLAME_FRAME+Math.floorMod(age,FLAME_FRAMES);}
    static float u0(int frame){return (frame%GRID)/(float)GRID;}
    static float u1(int frame){return (frame%GRID+1)/(float)GRID;}
    static float v0(int frame){return (frame/GRID)/(float)GRID;}
    static float v1(int frame){return (frame/GRID+1)/(float)GRID;}
    private NitorVisualRules(){}
}
