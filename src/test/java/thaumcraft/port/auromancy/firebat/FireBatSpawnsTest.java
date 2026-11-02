package thaumcraft.port.auromancy.firebat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FireBatSpawnsTest {
    @Test void netherWeightIsTenAndOnlyOctoberThirtyFirstAddsOverworldSpawns(){
        for(var date:new LocalDate[]{LocalDate.of(2026,10,30),LocalDate.of(2026,10,31),LocalDate.of(2026,11,1)}){
            assertEquals(10,FireBatSpawns.weight(true,false,date));
            assertEquals(0,FireBatSpawns.weight(false,false,date));
            assertEquals(date.getDayOfMonth()==31?5:0,FireBatSpawns.weight(false,true,date));
        }
    }
}
