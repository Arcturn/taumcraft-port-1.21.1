package thaumcraft.port.research;

import com.google.gson.JsonObject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static org.junit.jupiter.api.Assertions.*;

/** Differential layout tests against independently transcribed TC6/1.12 control flow.
 * Both sides receive the same glyph widths; the active 1.21 font is intentionally
 * a presentation choice, not a claim of legacy font pixel identity.
 */
class BookDocumentParityTest {
    private static final ToIntFunction<String> WIDTH=BookDocumentParityTest::nativeWidth;
    private static final int LINE=9;

    @TestFactory Stream<DynamicTest> all271StagesAnd16AddendaMatchOriginalLayoutWithSharedMetrics() {
        var tests=new ArrayList<DynamicTest>();
        var english=ResearchCatalog.read("/assets/thaumcraft/lang/en_us.json");
        int stages=0,addenda=0;
        for(var entry:ResearchCatalog.all()) {stages+=entry.stages().size();addenda+=entry.addenda().size();}
        assertEquals(271,stages);assertEquals(16,addenda);
        for(String language:List.of("en_us","ru_ru")) {
            var localized=ResearchCatalog.read("/assets/thaumcraft/lang/"+language+".json");
            for(var entry:ResearchCatalog.all()) {
                var sections=new ArrayList<JsonObject>(entry.stages());sections.addAll(entry.addenda());
                for(var section:sections) {
                    String key=section.get("text").getAsString();
                    String raw=(localized.has(key)?localized:english).get(key).getAsString();
                    tests.add(DynamicTest.dynamicTest(language+":"+entry.key()+":"+key,()->{
                        for(int height:new int[]{182,128,95,47})
                            assertEquals(original(raw,WIDTH,height),actual(raw,WIDTH,height),"first column "+height);
                    }));
                }
                if(!entry.addenda().isEmpty()) {
                    String stage=entry.stages().getLast().get("text").getAsString();
                    var combined=new StringBuilder((localized.has(stage)?localized:english).get(stage).getAsString());
                    int number=0;
                    for(var addition:entry.addenda()) {
                        String key=addition.get("text").getAsString();
                        String heading=String.format(Locale.ROOT,localized.get("tc.addendumtext").getAsString(),++number);
                        combined.append("<PAGE>").append(heading).append("<BR>")
                                .append((localized.has(key)?localized:english).get(key).getAsString());
                    }
                    tests.add(DynamicTest.dynamicTest(language+":"+entry.key()+":complete-addenda",()->
                            assertEquals(original(combined.toString(),WIDTH,182),actual(combined.toString(),WIDTH,182))));
                }
            }
        }
        return tests.stream();
    }

    @Test void leadingAndRepeatedSpacesAffectWrappingBeforeOriginalTrim() {
        var leading=BookDocument.parse(" "+"A".repeat(12)+" B",s->s.length()*10,LINE);
        assertEquals(List.of("A".repeat(12),"B"),leading.stream().map(BookDocument.Element::text).toList());
        var repeated=BookDocument.parse("A".repeat(10)+"   BBB",s->s.length()*10,LINE);
        assertEquals(List.of("A".repeat(10),"BBB"),repeated.stream().map(BookDocument.Element::text).toList());
        assertEquals(original("first\n   \nlast",WIDTH,182),actual("first\n   \nlast",WIDTH,182));
        assertEquals(3,BookDocument.parse("first\n   \nlast",WIDTH,LINE).size());
    }

    @Test void formattingCarryKeepsResetsDuplicatesCaseAndFormattedBlankLines() {
        for(String raw:List.of("§A§n§n"+"hello ".repeat(40),"§a"+"A".repeat(14)+"§r"+"B".repeat(14),
                "§nHeading<BR>paragraph", "§Lbold §Rreset\nnext", "§k§Ltext §cnew ".repeat(20)))
            assertEquals(original(raw,WIDTH,47),actual(raw,WIDTH,47),raw);
        var rows=BookDocument.parse("§nHeading<BR>paragraph",WIDTH,LINE);
        assertEquals("§n",rows.get(1).text(),"Vanilla carries the underline through the otherwise empty BR row");
    }

    @Test void nativeFractionalBoldAdvancesAreMeasuredCumulatively() {
        // Native Unihex bold offset is .5, not the old bitmap offset of 1.
        // Eighteen 7.5px glyphs fit; per-glyph ceil would incorrectly allow 17.
        var rows=BookDocument.parse("§l"+"Ж".repeat(20),WIDTH,LINE);
        assertEquals("§l"+"Ж".repeat(18),rows.getFirst().text());
        assertEquals("§lЖЖ",rows.get(1).text());
        assertTrue(rows.stream().allMatch(row->WIDTH.applyAsInt(row.text())<=140));
    }

    @Test void sourceTrailingImageDropIsRetainedForExactInfusionStageThreeParity() {
        // GuiResearchPage splits ~P/~D/~L/~I with default Java split. The real
        // BETA26 INFUSION.stage.3 ends in IMG, whose trailing marker is omitted.
        for(String language:List.of("en_us","ru_ru")) {
            String raw=ResearchCatalog.read("/assets/thaumcraft/lang/"+language+".json")
                    .get("research.INFUSION.stage.3").getAsString();
            assertTrue(raw.endsWith("</IMG>"));
            assertEquals(original(raw,WIDTH,182),actual(raw,WIDTH,182));
            long before=BookDocument.parse(raw,WIDTH,LINE).stream().filter(e->e.image()!=null).count();
            long withTerminator=BookDocument.parse(raw+"\n",WIDTH,LINE).stream().filter(e->e.image()!=null).count();
            assertEquals(before+1,withTerminator);
        }
    }

    @Test void deferredPicturesAndFractionalParagraphBudgetMatchOriginal() {
        String img="<IMG>thaumcraft:textures/gui/gui_researchbook.png:0:0:20:35:1</IMG>";
        for(String raw:List.of("one"+img+"\nnext<BR>last", "one"+img+"\n<PAGE>after",
                "one<BR>two<BR>three", "start<LINE>middle<DIV>end", "<PAGE>first<PAGE>last"))
            for(int remaining:new int[]{9,10,14,15,20,27,38,47})
                assertEquals(original(raw,WIDTH,remaining),actual(raw,WIDTH,remaining),raw+" / "+remaining);
    }

    @Test void oversizedIllustrationsAndGlyphsTerminateWithoutLosingVisibleContent() {
        assertTimeoutPreemptively(Duration.ofSeconds(2),()->{
            var pages=actual("<IMG>thaumcraft:large.png:0:0:30:300:1</IMG>\nend",WIDTH,182);
            assertEquals(2,pages.size());assertEquals("end",pages.getFirst().getFirst().element().text());
            assertEquals(300,pages.getLast().getFirst().element().image().displayHeight());
            var wide=BookDocument.parse("§nW",s->1000,LINE);
            assertEquals(1,wide.size());assertEquals("§nW",wide.getFirst().text());
        });
    }

    private static List<List<BookDocument.Placed>> actual(String raw,ToIntFunction<String> width,int height) {
        return BookDocument.paginate(BookDocument.parse(raw,width,LINE),height,LINE);
    }

    /** Direct source-order transcription of GuiResearchPage.parsePages/drawPage.
     * Does not call production parsing, wrapping, pagination, formatting or sizing.
     */
    private static List<List<BookDocument.Placed>> original(String raw,ToIntFunction<String> width,int height) {
        raw=raw.replace("<BR>","~B\n\n").replace("<BR/>","~B\n\n")
                .replace("<LINE>","~L").replace("<LINE/>","~L")
                .replace("<DIV>","~D").replace("<DIV/>","~D")
                .replace("<PAGE>","~P").replace("<PAGE/>","~P");
        var images=new ArrayList<BookDocument.Image>();
        for(String chunk:raw.split("<IMG>")) {
            int end=chunk.indexOf("</IMG>");if(end<0)continue;
            String clean=chunk.substring(0,end);images.add(sourceImage(clean));
            raw=raw.replaceFirst(Pattern.quote(clean),"~I");
        }
        raw=raw.replace("<IMG>","").replace("</IMG>","");
        var firstPass=new ArrayList<String>();
        String[] p=raw.split("~P");
        for(int a=0;a<p.length;a++) {
            String[] d=p[a].split("~D");
            for(int x=0;x<d.length;x++) {
                String[] l=d[x].split("~L");
                for(int b=0;b<l.length;b++) {
                    String[] i=l[b].split("~I");
                    for(int c=0;c<i.length;c++) {firstPass.add(i[c]);if(c<i.length-1)firstPass.add("~I");}
                    if(b<l.length-1)firstPass.add("~L");
                }
                if(x<d.length-1)firstPass.add("~D");
            }
            if(a<p.length-1)firstPass.add("~P");
        }
        var text=new ArrayList<String>();
        for(String segment:firstPass)text.addAll(Arrays.asList(fontWrap(segment,width).split("\n")));
        var pages=new ArrayList<List<Object>>();var page=new ArrayList<Object>();var pending=new ArrayList<BookDocument.Image>();
        for(String originalLine:text) {
            String line=originalLine;
            if(line.contains("~I")) {if(!images.isEmpty())pending.add(images.removeFirst());line="";}
            if(line.contains("~L")) {pending.add(sourceImage("thaumcraft:textures/gui/gui_researchbook.png:24:184:95:6:1"));line="";}
            if(line.contains("~D")) {pending.add(sourceImage("thaumcraft:textures/gui/gui_researchbook.png:28:192:140:6:1"));line="";}
            if(line.contains("~P")) {height=210;pages.add(List.copyOf(page));page.clear();line="";}
            if(!line.isEmpty()) {
                line=line.trim();page.add(line);height-=LINE;
                if(line.endsWith("~B"))height=(int)(height-LINE*.66);
            }
            while(!pending.isEmpty()&&height>=pending.getFirst().displayHeight()+2) {
                var image=pending.removeFirst();height-=image.displayHeight()+2;page.add(image);
            }
            if(height<LINE&&!page.isEmpty()) {height=210;pages.add(List.copyOf(page));page.clear();}
        }
        if(!page.isEmpty())pages.add(List.copyOf(page));page.clear();height=210;
        while(!pending.isEmpty()) {
            var image=pending.getFirst();
            assertTrue(image.displayHeight()+2<=210,"The original loops forever for oversized images; that bug is excluded from differential inputs");
            if(height<image.displayHeight()+2) {height=210;pages.add(List.copyOf(page));page.clear();continue;}
            height-=image.displayHeight()+2;page.add(pending.removeFirst());
        }
        if(!page.isEmpty())pages.add(List.copyOf(page));
        var result=new ArrayList<List<BookDocument.Placed>>();
        for(var contents:pages) {
            var placed=new ArrayList<BookDocument.Placed>();int y=0;
            for(Object item:contents) {
                if(item instanceof String line) {
                    int next=y+LINE;if(line.endsWith("~B"))next=(int)(next+LINE*.66);
                    placed.add(new BookDocument.Placed(new BookDocument.Element(line.replace("~B",""),null,next-y,false),y));y=next;
                } else {
                    var image=(BookDocument.Image)item;int size=(int)(image.height()*image.scale())+2;
                    placed.add(new BookDocument.Placed(new BookDocument.Element(null,image,size,false),y));y+=size;
                }
            }
            result.add(List.copyOf(placed));
        }
        return List.copyOf(result);
    }

    private static BookDocument.Image sourceImage(String text) {
        String[] p=text.split(":");
        return new BookDocument.Image(p[0]+":"+p[1],Integer.parseInt(p[2]),Integer.parseInt(p[3]),
                Integer.parseInt(p[4]),Integer.parseInt(p[5]),Float.parseFloat(p[6]));
    }

    /** Original FontRenderer recursive wrapping, separate from production's iteration. */
    private static String fontWrap(String text,ToIntFunction<String> width) {
        int length=text.length(),used=0,index=0,space=-1;
        for(;index<length;index++) {
            char c=text.charAt(index);
            switch(c) {
                case '\n':index--;break;
                case ' ':space=index;
                // The only intentional font-era adaptation: measure through
                // the caller's current cumulative font oracle, including style.
                default:used=width.applyAsInt(text.substring(0,index+1));break;
                case '§':
                    if(index<length-1)index++;
            }
            if(c=='\n') {space=++index;break;}
            if(used>140)break;
        }
        int cut=index!=length&&space!=-1&&space<index?space:index;
        if(length<=cut)return text;
        String head=text.substring(0,cut);char c=text.charAt(cut);
        return head+"\n"+fontWrap(sourceCarry(head)+text.substring(cut+(c==' '||c=='\n'?1:0)),width);
    }
    private static boolean sourceColor(char c) {return c>='0'&&c<='9'||c>='a'&&c<='f'||c>='A'&&c<='F';}
    private static int nativeWidth(String text) {
        double width=0;boolean bold=false;
        for(int i=0;i<text.length();i++) {
            char c=text.charAt(i);
            if(c=='§'&&i+1<text.length()) {
                char style=Character.toLowerCase(text.charAt(++i));
                if(style=='l')bold=true;else if(style=='r'||sourceColor(style))bold=false;
                continue;
            }
            width+=c==' '?4:"il.,:;'!|".indexOf(c)>=0?2:c=='W'||c=='M'?7:c>=0x400?7:6;
            if(bold)width+=c>=0x400?.5:1;
        }
        return (int)Math.ceil(width);
    }
    private static String sourceCarry(String text) {
        String format="";int at=-1;
        while((at=text.indexOf('§',at+1))!=-1) if(at<text.length()-1) {
            char c=text.charAt(at+1);
            if(sourceColor(c))format="§"+c;
            else if(c>='k'&&c<='o'||c>='K'&&c<='O'||c=='r'||c=='R')format+="§"+c;
        }
        return format;
    }
}
