package thaumcraft.port.client;

import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Native font sizing for decorative book numbers on the physical pixel grid. */
public final class BookTypography {
    public static float smallScale() {
        var minecraft=Minecraft.getInstance();
        return minecraft.options.forceUnicodeFont().get()?1:smallScale(minecraft.getWindow().getGuiScale());
    }

    /** Rounds half-size glyph pixels up to whole screen pixels; invalid scales use native size. */
    public static float smallScale(double guiScale) {
        if (!Double.isFinite(guiScale) || guiScale <= 0) return 1;
        return (float) (Math.ceil(guiScale * .5) / guiScale);
    }

    /** Keep TC6's component columns unless readable native-size labels actually overlap. */
    static int componentSpacing(int firstWidth, int secondWidth, float textScale) {
        double halfWidths = (firstWidth + secondWidth) * (double) textScale / 2;
        return halfWidths <= 42 ? 42 : (int) Math.ceil(halfWidths) + 4;
    }

    /** The paper is opaque through center+93, although its transparent texture quad is wider. */
    static int componentShift(int secondWidth, int spacing, float textScale) {
        // smallLabel uses -width/2 (integer division), so odd widths leave the extra advance on the right.
        double rightEdge=4+spacing+(secondWidth-secondWidth/2)*(double)textScale;
        return Math.max(0,(int)Math.ceil(rightEdge-93));
    }

    /** Native wrapping with a short trailing parenthesis kept together, without adding title rows. */
    public static List<FormattedCharSequence> titleLines(Font font, Component title, int maximumWidth) {
        return wrapTitle(title.getString(),maximumWidth,font::width,width->font.split(title,width),BookTypography::plainText);
    }

    static <T> List<T> wrapTitle(String source, int maximumWidth, ToIntFunction<String> width,
                                 IntFunction<List<T>> split, Function<T,String> text) {
        int maximum=Math.max(1,maximumWidth);
        var normal=split.apply(maximum);
        int start=source.lastIndexOf('(');
        if(normal.isEmpty()||start<0||!source.endsWith(")"))return normal;
        String suffix=source.substring(start);
        if(suffix.indexOf(')')!=suffix.length()-1||suffix.indexOf('\n')>=0)return normal;
        int minimum=Math.max(1,width.applyAsInt(suffix));
        if(minimum>maximum||text.apply(normal.getLast()).stripTrailing().endsWith(suffix))return normal;
        // The first successful width is the widest; native providers and all component styles
        // remain authoritative. No nonbreaking-space substitution or fractional font scaling.
        for(int candidate=maximum-1;candidate>=minimum;candidate--){
            var lines=split.apply(candidate);
            if(!lines.isEmpty()&&lines.size()<=normal.size()&&text.apply(lines.getLast()).stripTrailing().endsWith(suffix))return lines;
        }
        return normal;
    }

    private static String plainText(FormattedCharSequence line) {
        var text=new StringBuilder();
        line.accept((index,style,codePoint)->{text.appendCodePoint(codePoint);return true;});
        return text.toString();
    }

    private BookTypography() {}
}
