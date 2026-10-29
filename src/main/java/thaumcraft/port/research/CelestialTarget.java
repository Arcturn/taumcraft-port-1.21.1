package thaumcraft.port.research;
/** Original ScanSky angular tolerances and note metadata, without client dependencies. */
public record CelestialTarget(String name,int variant) {
    public static CelestialTarget find(float yaw,float pitch,float angle,int moon,int facing){
        if(pitch>0)return null;
        int y=(int)(yaw+90)%360,p=(int)Math.abs(pitch),ca=(int)((angle+.25)*360)%360;
        boolean night=ca>180;if(night)ca-=180;
        boolean aim=ca>90?Math.abs(Math.abs(y)-180)<10&&Math.abs(180-ca-p)<7:Math.abs(y)<10&&Math.abs(ca-p)<7;
        if(aim)return new CelestialTarget(night?"Moon"+moon:"Sun",night?5+moon:0);
        return night&&facing>=0&&facing<4?new CelestialTarget("Star"+facing,1+facing):null;
    }
    public String key(long gameTime){return "CEL_"+(gameTime/24000)+"_"+name;}
}
