import java.util.zip.ZipFile;
import com.mojang.blaze3d.platform.NativeImage;

/** Uses the real 1.21.1 image loader; no window, server, or OpenGL context. */
public final class GuiTextureSmokeTest {
    public static void main(String[] args) throws Exception {
        int rejected=0,decoded=0;
        try(var jar=new ZipFile(args[0])){
            for(int i=1;i<=7;i++){
                String stem="assets/thaumcraft/textures/gui/gui_research_back_"+i;
                try(var image=NativeImage.read(jar.getInputStream(jar.getEntry(stem+".jpg")))){
                    throw new AssertionError("Expected the original JPG to be rejected by the 1.21.1 PNG header check");
                }catch(java.io.IOException expected){rejected++;}
                try(var image=NativeImage.read(jar.getInputStream(jar.getEntry(stem+".png")))){
                    if(image.getWidth()!=1024||image.getHeight()!=1024)throw new AssertionError(stem);
                }
            }
            for(var entry:java.util.Collections.list(jar.entries())){
                if(!entry.getName().startsWith("assets/thaumcraft/textures/gui/")||!entry.getName().endsWith(".png"))continue;
                try(var image=NativeImage.read(jar.getInputStream(entry))){
                    if(image.getWidth()<1||image.getHeight()<1)throw new AssertionError(entry.getName());decoded++;
                }
            }
        }
        System.out.println("PASS: reproduced rejection of "+rejected+" legacy JPG backgrounds; all 7 replacement PNG backgrounds decode at 1024x1024");
        System.out.println("PASS: "+decoded+" GUI textures decoded by Minecraft NativeImage; GPU compositing not tested");
    }
}
