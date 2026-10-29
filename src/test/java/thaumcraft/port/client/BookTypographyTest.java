package thaumcraft.port.client;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookTypographyTest {
    @Test void decorativeNumbersKeepWholePhysicalPixelsAtCommonGuiScales() {
        assertEquals(1, BookTypography.smallScale(1));
        assertEquals(.5f, BookTypography.smallScale(2));
        assertEquals(2f / 3, BookTypography.smallScale(3));
        assertEquals(.5f, BookTypography.smallScale(4));
        assertEquals(2, 3 * BookTypography.smallScale(3), .000001,
                "GUI scale 3 must use two-pixel strokes instead of alternating one and two");
        assertEquals(2, 2.5 * BookTypography.smallScale(2.5), .000001);
    }

    @Test void invalidWindowScalesFallBackToNativeSize() {
        for (double scale : new double[]{0, -1, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY})
            assertEquals(1, BookTypography.smallScale(scale));
    }

    @Test void fabricoComponentsStayReadableAtGuiOneWithoutMovingOtherGuiScales() {
        // Actual 1.21 bitmap advances: Permutatio=52, Instrumentum=66.
        int gap=BookTypography.componentSpacing(52,66,BookTypography.smallScale(1));
        assertEquals(63,gap);
        int shift=BookTypography.componentShift(66,gap,1);
        assertEquals(7,shift);
        assertTrue(gap-52/2-66/2>=4,"The two native-size names need a visible gap");
        assertTrue(4-shift+gap+66/2<=93,"Fit the opaque paper, not its wider transparent texture quad");
        assertTrue((4-shift-52/2)-(-52+38/2)>=4,"Fabrico must also stay separate from its first component");
        for(double guiScale:new double[]{2,3,4}){
            assertEquals(42,BookTypography.componentSpacing(52,66,BookTypography.smallScale(guiScale)));
            assertEquals(0,BookTypography.componentShift(66,42,BookTypography.smallScale(guiScale)));
        }
        assertEquals(42,BookTypography.componentSpacing(37,45,1),"Uniform names already fit at native size");
        assertEquals(0,BookTypography.componentShift(45,42,1));
    }

    @Test void russianImprovedSmeltingKeepsItsPartSuffixAtTheWidestFittingWidth() {
        // Actual 1.21 default glyph measurements for research.IMPROVEDSMELTING.title:
        // width131 leaves "1)" alone; width130 gives 106px / 93px / 48px lines.
        var orphan=List.of("Улучшение процесса","очистки эссенций (Часть","1)");
        var grouped=List.of("Улучшение процесса","очистки эссенций","(Часть 1)");
        var tried=new ArrayList<Integer>();
        var result=BookTypography.wrapTitle("Улучшение процесса очистки эссенций (Часть 1)",140,s->48,
                width->{tried.add(width);return width>=131?orphan:grouped;},s->s);
        assertEquals(grouped,result);
        assertEquals(130,tried.getLast().intValue());
        assertEquals(orphan.size(),result.size(),"Keep the original title's reserved height");
    }

    @Test void suffixPolishNeverAddsRowsOrRewrapsAnAlreadyCompleteSuffix() {
        var normal=List.of("A long heading (part","2)");
        assertEquals(normal,BookTypography.wrapTitle("A long heading (part 2)",140,s->30,
                width->width==140?normal:List.of("A long","heading","(part 2)"),s->s));
        var complete=List.of("Improved Essentia","Distillation (part 1)");
        int[] calls={0};
        assertEquals(complete,BookTypography.wrapTitle("Improved Essentia Distillation (part 1)",140,s->30,
                width->{calls[0]++;return complete;},s->s));
        assertEquals(1,calls[0]);
    }
}
