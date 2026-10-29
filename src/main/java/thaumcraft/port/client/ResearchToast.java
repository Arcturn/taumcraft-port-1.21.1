package thaumcraft.port.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import thaumcraft.port.research.ResearchCatalog;

/** TC6 frame/icon/timing, with native-size wrapped text as requested for the modern book. */
public final class ResearchToast implements Toast {
    private final ResearchCatalog.Entry entry;
    private final List<FormattedCharSequence> heading;
    private final List<FormattedCharSequence> title;
    private final Layout layout;
    private final List<FrameStrip> frame;
    public ResearchToast(ResearchCatalog.Entry entry){
        this.entry=entry;
        var font=Minecraft.getInstance().font;
        heading=List.copyOf(font.split(Component.translatable("research.complete"),124));
        title=List.copyOf(BookTypography.titleLines(font,Component.translatable(entry.title()),124));
        // ToastComponent reserves slots before render. Keep these dimensions stable while queued.
        layout=new Layout(heading.size(),title.size(),font.lineHeight);
        frame=frame(layout.height());
    }
    @Override public int height(){return layout.height();}
    @Override public int slotCount(){return layout.slots();}
    @Override public Visibility render(GuiGraphics graphics,ToastComponent component,long elapsed){
        for(var strip:frame)BookAssets.sprite(graphics,BookAssets.id("textures/gui/hud.png"),0,strip.y(),0,strip.sourceY(),160,strip.height());
        BookAssets.researchBrowserIcon(graphics,entry,6,8,false,1);
        var font=component.getMinecraft().font;
        for(int line=0;line<heading.size();line++)graphics.drawString(font,heading.get(line),30,layout.headingY(line),10631665,false);
        for(int line=0;line<title.size();line++)graphics.drawString(font,title.get(line),30,layout.titleY(line),16755465,false);
        return elapsed<5000?Visibility.SHOW:Visibility.HIDE;
    }

    record Layout(int headingLines,int titleLines,int lineHeight){
        Layout{headingLines=Math.max(1,headingLines);titleLines=Math.max(1,titleLines);lineHeight=Math.max(1,lineHeight);}
        int headingY(int line){return 7+line*lineHeight;}
        int titleY(int line){return 7+headingLines*lineHeight+2+line*lineHeight;}
        int height(){return Math.max(32,titleY(titleLines)+5);}
        int slots(){return Math.ceilDiv(height(),Toast.SLOT_HEIGHT);}
    }
    record FrameStrip(int y,int sourceY,int height){}
    /** Preserve both original 16-pixel caps; only an interior one-pixel strip is repeated. */
    static List<FrameStrip> frame(int height){
        int bottom=Math.max(32,height)-16;var strips=new ArrayList<FrameStrip>();
        strips.add(new FrameStrip(0,224,16));
        for(int y=16;y<bottom;y++)strips.add(new FrameStrip(y,240,1));
        strips.add(new FrameStrip(bottom,240,16));return List.copyOf(strips);
    }
}
