package thaumcraft.port.research;

import java.util.*;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

/** TC6's 140-pixel columns, explicit page breaks, dividers and cropped illustrations. */
public final class BookDocument {
    public record Image(String texture,int u,int v,int width,int height,float scale) {
        public static Image parse(String source) {
            String[] p=source.trim().split(":");
            if(p.length!=7)throw new IllegalArgumentException("Invalid research image: "+source);
            return new Image(p[0]+":"+p[1],Integer.parseInt(p[2]),Integer.parseInt(p[3]),Integer.parseInt(p[4]),Integer.parseInt(p[5]),Float.parseFloat(p[6]));
        }
        public int displayWidth(){return (int)(width*scale);}
        public int displayHeight(){return (int)(height*scale);}
    }
    public record Element(String text,Image image,int height,boolean pageBreak) {}
    public record Placed(Element element,int y) {}
    private static final Pattern IMAGES=Pattern.compile("<IMG>(.*?)</IMG>",Pattern.DOTALL);
    private static final String[] MARKERS={"~P","~D","~L","~I"};
    private static final Image LINE=new Image("thaumcraft:textures/gui/gui_researchbook.png",24,184,95,6,1);
    private static final Image DIV=new Image("thaumcraft:textures/gui/gui_researchbook.png",28,192,140,6,1);
    /** Original parsePages reserves the inline knowledge table before wrapping the first column. */
    public static int firstPageHeight(boolean knowledgePage,int knowledgeRows,int requirementRows){
        int height=182,divider=0;
        if(knowledgePage){height-=2+20*knowledgeRows;divider=12;}
        if(requirementRows>0){height-=18*requirementRows;divider=15;}
        return height-divider;
    }
    public static List<Element> parse(String raw,ToIntFunction<String> width,int lineHeight) {
        // The original parser substitutes BR before FontRenderer sees the text:
        // ~B consumes width, and the two newlines carry formatting to the next paragraph.
        raw=raw.replace("<BR>","~B\n\n").replace("<BR/>","~B\n\n")
                .replace("<LINE>","~L").replace("<LINE/>","~L")
                .replace("<DIV>","~D").replace("<DIV/>","~D")
                .replace("<PAGE>","~P").replace("<PAGE/>","~P");
        Deque<Image> images=new ArrayDeque<>();var matcher=IMAGES.matcher(raw);var replaced=new StringBuilder();
        while(matcher.find()) {
            String token;
            try {images.addLast(Image.parse(matcher.group(1)));token="~I";}
            catch(IllegalArgumentException exception) {token="\n";}
            matcher.appendReplacement(replaced,token);
        }
        matcher.appendTail(replaced);
        List<String> tokens=new ArrayList<>();splitMarkers(replaced.toString(),0,tokens);
        List<Element> out=new ArrayList<>();boolean hasImages=false;
        for(String token:tokens) for(String line:wrap(token,width)) {
            int before=out.size();
            if(line.contains("~I")) {if(!images.isEmpty())image(images.removeFirst(),out);line="";}
            if(line.contains("~L")) {image(LINE,out);line="";}
            if(line.contains("~D")) {image(DIV,out);line="";}
            if(line.contains("~P")) {out.add(new Element(null,null,0,true));line="";}
            // Test emptiness BEFORE trim, as GuiResearchPage does. A line of
            // spaces consumes a text row; repeated spaces also affect wrapping.
            if(!line.isEmpty()) {
                line=line.trim();boolean paragraph=line.endsWith("~B");
                out.add(new Element(line.replace("~B",""),null,
                        lineHeight+(paragraph?(int)(lineHeight*.66):0),false));
            } else if(out.size()==before&&hasImages) {
                // Empty wrapped rows still run the original image queue. If
                // the preceding row started a new page, a waiting image must
                // be inserted before the next text row, not after it.
                out.add(new Element(null,null,0,false));
            }
            if(out.size()>before&&out.getLast().image()!=null)hasImages=true;
        }
        return List.copyOf(out);
    }
    private static void image(Image image,List<Element> out) {
        out.add(new Element(null,image,image.displayHeight()+2,false));
    }
    private static void splitMarkers(String text,int depth,List<String> out) {
        if(depth==MARKERS.length) {out.add(text);return;}
        // Java split's trailing-empty removal is part of the original parser.
        // In particular, a final IMG/LINE/PAGE marker is not emitted.
        String[] parts=text.split(Pattern.quote(MARKERS[depth]));
        for(int i=0;i<parts.length;i++) {
            splitMarkers(parts[i],depth+1,out);
            if(i+1<parts.length)out.add(MARKERS[depth]);
        }
    }
    /** FontRenderer.wrapFormattedStringToWidth, iterated to avoid deep recursion. */
    private static List<String> wrap(String text,ToIntFunction<String> width) {
        List<String> lines=new ArrayList<>();
        while(true) {
            int cut=wrapIndex(text,width);
            if(text.length()<=cut) {lines.add(text);break;}
            boolean separator=text.charAt(cut)==' '||text.charAt(cut)=='\n';
            // Vanilla recurses forever when a single glyph is wider than the
            // column. Consume that glyph safely without changing normal wraps.
            if(cut==0&&!separator)cut=Character.charCount(text.codePointAt(0));
            String line=text.substring(0,cut);
            int next=cut+(separator?1:0);
            String remainder=activeFormat(line)+text.substring(next);
            if(remainder.equals(text)) {
                while(cut+1<text.length()&&text.charAt(cut)=='\u00a7')cut+=2;
                if(cut<text.length())cut+=Character.charCount(text.codePointAt(cut));
                line=text.substring(0,cut);
                String tail=text.substring(cut);remainder=tail.isEmpty()?"":activeFormat(line)+tail;
            }
            lines.add(line);text=remainder;
        }
        // Preserve Java's distinction between an empty input (one empty row)
        // and an input containing only newlines (no rows after trailing trim).
        return Arrays.asList(String.join("\n",lines).split("\n"));
    }
    private static int wrapIndex(String text,ToIntFunction<String> width) {
        int used=0,index=0,lastSpace=-1;
        for(;index<text.length();index++) {
            char c=text.charAt(index);
            if(c=='\n')break;
            if(c=='\u00a7') {
                if(index+1<text.length())index++;
            } else {
                if(c==' ')lastSpace=index;
                // Keep TC6's break positions/format carry rules, but use the
                // selected 1.21 font's actual cumulative styled width. Unihex
                // bold adds .5 rather than bitmap's 1; per-glyph rounding or
                // a fixed legacy bold increment would overflow native text.
                used=width.applyAsInt(text.substring(0,index+1));
            }
            if(used>140)break;
        }
        // An explicit newline wins over an earlier space in this line.
        if(index<text.length()&&text.charAt(index)=='\n')return index;
        return index!=text.length()&&lastSpace>=0&&lastSpace<index?lastSpace:index;
    }
    private static boolean isColor(char code) {
        return code>='0'&&code<='9'||code>='a'&&code<='f'||code>='A'&&code<='F';
    }
    /** Preserve FontRenderer's exact carry, including redundant styles and resets. */
    private static String activeFormat(String text) {
        String result="";
        for(int i=0;(i=text.indexOf('\u00a7',i))>=0;i++) if(i+1<text.length()) {
            char code=text.charAt(i+1);
            if(isColor(code))result="\u00a7"+code;
            else if(code>='k'&&code<='o'||code>='K'&&code<='O'||code=='r'||code=='R')result+="\u00a7"+code;
        }
        return result;
    }
    public static List<List<Placed>> paginate(List<Element> elements,int firstHeight){return paginate(elements,firstHeight,9);}
    public static List<List<Placed>> paginate(List<Element> elements,int firstHeight,int lineHeight){
        List<List<Placed>> pages=new ArrayList<>();List<Placed> page=new ArrayList<>();Deque<Element> images=new ArrayDeque<>();int y=0,remaining=firstHeight;
        for(var e:elements){
            if(e.image()!=null)images.addLast(e);
            else if(e.pageBreak()){pages.add(List.copyOf(page));page.clear();y=0;remaining=210;}
            else if(e.text()!=null) {
                page.add(new Placed(e,y));y+=e.height();remaining-=lineHeight;
                // Java truncates toward zero. Near a full page, subtracting
                // the fractional BR spacing is not equivalent to always ceil.
                if(e.height()>lineHeight)remaining=(int)(remaining-lineHeight*.66);
            }
            // An illustration that does not fit waits; later text continues filling this column.
            while(!images.isEmpty()&&images.peekFirst().height()<=remaining){var image=images.removeFirst();page.add(new Placed(image,y));y+=image.height();remaining-=image.height();}
            if(remaining<lineHeight&&!page.isEmpty()){pages.add(List.copyOf(page));page.clear();y=0;remaining=210;}
        }
        if(!page.isEmpty()){pages.add(List.copyOf(page));page.clear();}
        y=0;remaining=210;
        while(!images.isEmpty()){
            var image=images.peekFirst();
            if(image.height()>remaining&&!page.isEmpty()){pages.add(List.copyOf(page));page.clear();y=0;remaining=210;continue;}
            images.removeFirst();page.add(new Placed(image,y));y+=image.height();remaining-=image.height();
        }
        if(!page.isEmpty())pages.add(List.copyOf(page));return List.copyOf(pages);
    }
    private BookDocument(){}
}
