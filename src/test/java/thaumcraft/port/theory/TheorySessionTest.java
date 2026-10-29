package thaumcraft.port.theory;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import thaumcraft.port.research.ResearchState;

final class TheorySessionTest {
    private static final UUID OWNER=UUID.fromString("bda346a0-9802-44c7-a902-99d136b73fc5");
    private static final class Context implements TheoryCards.Context {
        final ResearchState state=new ResearchState();int xp=30;
        Context(boolean unlocked){if(unlocked){state.completeCatalog();for(String c:TheorySession.CATEGORIES)state.addKnowledge("OBSERVATION",c,32);}}
        public ResearchState research(){return state;}public int levels(){return xp;}public List<String> curios(){return List.of("thaumcraft:curio_arcane","thaumcraft:curio_rites");}public int aspectTotal(String id){return 49;}
    }
    private TheorySession session(){return new TheorySession(OWNER,10,List.of());}
    private TheorySession prepared(){var s=session();s.add("ALCHEMY",40);s.add("ARTIFICE",30);s.add("INFUSION",20);return s;}
    @Test void exactRegistryAndWeightedAids(){assertEquals(33,TheoryCards.KINDS.size());assertEquals(33,new HashSet<>(TheoryCards.KINDS).size());assertEquals(14,TheoryAids.DEFINITIONS.size());assertFalse(TheoryCards.KINDS.contains("Truth"));assertFalse(TheoryCards.KINDS.contains("DragonEgg"));assertEquals(3,Collections.frequency(TheoryAids.DEFINITIONS.get("Bookshelf").cards(),"Study"));assertEquals(2,Collections.frequency(TheoryAids.DEFINITIONS.get("Bookshelf").cards(),"Notation"));}
    @Test void inspirationAndAidCosts(){var ctx=new Context(false);assertEquals(5,TheorySession.availableInspiration(ctx.state));assertEquals(14,TheorySession.availableInspiration(new Context(true).state));var s=new TheorySession(OWNER,5,List.of("Bookshelf","BasicAlchemy"));assertEquals(3,s.inspiration);assertEquals(9,s.aidCards.size());assertThrows(IllegalArgumentException.class,()->new TheorySession(OWNER,1,List.of("Bookshelf")));assertThrows(IllegalArgumentException.class,()->new TheorySession(OWNER,5,List.of("NotAnAid")));}
    @Test void allRegisteredCardsHaveTheirOriginalConditionsAndEffects(){
        var ctx=new Context(true);
        for(String kind:TheoryCards.KINDS){var s=prepared();var card=TheoryCards.initialize(kind,91,true,s,ctx);
            if(kind.equals("Analyze")){assertNull(card,"BETA26 null-category observation bug");continue;}
            assertNotNull(card,kind);s.choices.add(card);var snapshot=s.encode();var result=TheoryCards.activate(s,0,ctx,new Random(1234));assertNotNull(result,kind);assertEquals(snapshot,s.encode(),kind+" source must be untouched before transaction commit");assertEquals(0,result.next().selected,kind);assertNull(TheoryCards.activate(result.next(),0,ctx,new Random(1234)),kind+" selected card cannot pay twice");assertEquals(result.next().encode(),TheorySession.decode(result.next().encode()).encode(),kind+" durable card parameters");
        }
    }
    @Test void initializationGuards(){var ctx=new Context(false);var s=session();for(String kind:List.of("Balance","Notation","Ponder","Rethink","Reject","Inspired","Celestial","Synergy"))assertNull(TheoryCards.initialize(kind,1,false,s,ctx),kind);ctx.xp=0;assertNull(TheoryCards.initialize("Spellbinding",1,false,s,ctx));assertNull(TheoryCards.initialize("Truth",1,true,s,ctx));}
    @Test void drawingLocksCategoriesAndNeverReturnsDuplicateCardKinds(){
        var ctx=new Context(false);for(int seed=0;seed<100;seed++){var s=session();assertTrue(s.draw(ctx,new Random(seed)));assertTrue(s.choices.size()>=1&&s.choices.size()<=2);assertEquals(s.choices.size(),s.choices.stream().map(TheoryCards.Card::kind).distinct().count());for(var c:s.choices){assertFalse(TheoryCards.aidOnly(c.kind()));assertTrue(TheoryCards.researchCategory(c).isEmpty()||TheoryCards.researchCategory(c).equals("BASICS"));}assertFalse(s.draw(ctx,new Random(seed)),"Cannot replace an unresolved draw");}
    }
    @Test void aidCardsCanTeachLockedCategoriesButRespectRejection(){var ctx=new Context(false);boolean seen=false;for(int seed=0;seed<100;seed++){var s=new TheorySession(OWNER,5,List.of("BasicAlchemy"));s.draw(ctx,new Random(seed));if(s.choices.stream().anyMatch(c->c.fromAid()&&c.category().equals("ALCHEMY")))seen=true;var b=new TheorySession(OWNER,5,List.of("BasicAlchemy"));b.blocked.add("ALCHEMY");b.draw(ctx,new Random(seed));assertTrue(b.choices.stream().noneMatch(c->TheoryCards.researchCategory(c).equals("ALCHEMY")));}assertTrue(seen);}
    @Test void bonusDrawIsConsumedOnceAndCannotReroll(){var s=prepared();s.bonusDraws=2;assertTrue(s.draw(new Context(true),new Random(3)));assertEquals(3,s.choices.size());assertEquals(1,s.bonusDraws);assertFalse(s.draw(new Context(true),new Random(4)));assertEquals(1,s.bonusDraws);}
    @Test void seededRequirementsSurviveRestart(){var ctx=new Context(true);for(String kind:List.of("Concentrate","Reactions","Synthesis","Channel","Infuse","Tinker","MindOverMatter","Celestial","Curio")){var s=prepared();var c=TheoryCards.initialize(kind,898,true,s,ctx);s.choices.add(c);var loaded=TheorySession.decode(s.encode());assertEquals(c,loaded.choices.getFirst());assertEquals(c,TheoryCards.initialize(kind,898,true,s,ctx));}}
    @Test void celestialAlwaysRequiresTwoDifferentNotes(){var ctx=new Context(true);for(int i=0;i<100;i++){var c=TheoryCards.initialize("Celestial",i,false,prepared(),ctx);assertNotEquals(c.required().get(0).kind(),c.required().get(1).kind());assertTrue(c.required().stream().allMatch(TheoryCards.Requirement::consumed));}}
    @Test void requirementsDistinguishPresenceAndConsumption(){var ctx=new Context(true);for(String k:List.of("Concentrate","Reactions","Channel","Tinker"))assertTrue(TheoryCards.initialize(k,3,false,prepared(),ctx).required().stream().noneMatch(TheoryCards.Requirement::consumed));for(String k:List.of("Synthesis","Infuse","MindOverMatter","Curio","Celestial","Sculpting"))assertTrue(TheoryCards.initialize(k,3,false,prepared(),ctx).required().stream().allMatch(TheoryCards.Requirement::consumed));}
    @Test void synergyDrainsExactlyFifteenIncludingExhaustedCategories(){var ctx=new Context(true);var s=session();s.add("ARTIFICE",1);s.add("ALCHEMY",2);s.add("INFUSION",12);s.choices.add(TheoryCards.initialize("Synergy",1,false,s,ctx));var n=TheoryCards.activate(s,0,ctx,new Random(1)).next();assertEquals(Map.of("GOLEMANCY",30),n.totals);assertEquals(1,n.penaltyStart);}
    @Test void zeroAndNegativeInspirationCostsAreReal(){var ctx=new Context(true);for(String k:List.of("Beacon","Portal","Rethink","Reject")){var s=prepared();s.inspiration=4;s.choices.add(TheoryCards.initialize(k,4,true,s,ctx));var n=TheoryCards.activate(s,0,ctx,new Random(5)).next();assertEquals(4-TheoryCards.cost(k),n.inspiration);}}
    @Test void experienceIsValidatedBeforeAnyTheoryMutation(){var ctx=new Context(true);var s=prepared();s.choices.add(TheoryCards.initialize("Enchantment",1,true,s,ctx));ctx.xp=4;String before=s.encode();assertNull(TheoryCards.activate(s,0,ctx,new Random(1)));assertEquals(before,s.encode());ctx.xp=5;assertEquals(5,TheoryCards.activate(s,0,ctx,new Random(1)).levels());}
    @Test void rawKnowledgeRoundingPenaltyAndTieOrder(){var s=session();s.inspiration=0;s.add("BASICS",100);s.add("ALCHEMY",100);s.add("ARTIFICE",50);assertEquals(List.of("ALCHEMY","BASICS","ARTIFICE"),new ArrayList<>(s.rewards().keySet()));assertEquals(Map.of("ALCHEMY",32,"BASICS",21,"ARTIFICE",10),s.rewards());s.penaltyStart=1;assertEquals(32,s.rewards().get("BASICS"));s.add("ELDRITCH",1);assertEquals(1,s.rewards().get("ELDRITCH"));}
    @Test void lastCardAndHistoryArePreservedWithoutRepeatedApplication(){var s=prepared();var ctx=new Context(true);s.choices.add(TheoryCards.initialize("Calibrate",5,false,s,ctx));var n=TheoryCards.activate(s,0,ctx,new Random(4)).next();n.acknowledge();assertTrue(n.choices.isEmpty());assertEquals("Calibrate",n.lastDraw.kind());n.choices.add(TheoryCards.initialize("Measure",6,false,n,ctx));n=TheoryCards.activate(n,0,ctx,new Random(8)).next();n.acknowledge();assertEquals(List.of(5L),n.savedCards);assertEquals("Measure",n.lastDraw.kind());assertEquals(n.encode(),TheorySession.decode(n.encode()).encode());}
    @Test void aspectValuationRefreshesAfterRecipeReloadWithoutRerollingTheItem(){
        var context=new Context(true);var s=prepared();var card=TheoryCards.initialize("Tinker",17,false,s,context);s.choices.add(card);
        var changed=new TheoryCards.Context(){public ResearchState research(){return context.state;}public int levels(){return 20;}public List<String> curios(){return List.of();}public int aspectTotal(String item){return 100;}};
        var fresh=TheoryCards.refreshValue(card,changed);assertEquals(10,fresh.value());assertEquals(card.required(),fresh.required());assertEquals(card.seed(),fresh.seed());
        var result=TheoryCards.activate(s,0,changed,new Random(){public int nextInt(int bound){return 0;}});assertEquals(50,result.next().totals.get("ARTIFICE"));assertEquals(7,card.value());
    }
    @Test void malformedSaveIsRejected(){assertThrows(RuntimeException.class,()->TheorySession.decode("{}"));var s=prepared();assertThrows(RuntimeException.class,()->TheorySession.decode(s.encode().replace("\"inspirationStart\":10","\"inspirationStart\":1000")));assertThrows(RuntimeException.class,()->TheorySession.decode(" ".repeat(60001)));}
    @Test void originalCardArithmeticGoldenCases(){
        // Hand-calculated from BETA26 activate() methods: initial ALCHEMY=40, ARTIFICE=30,
        // INFUSION=20; every inclusive random range returns its minimum, chance/coin=false.
        String[][] cases={
            {"Analyze","ALCHEMY=65,ARTIFICE=30,INFUSION=20,BASICS=5","2,0,0,0,0,16"},
            {"Balance","ALCHEMY=30,ARTIFICE=30,INFUSION=30,BASICS=5","1,0,1,0,0,0"},
            {"Notation","ALCHEMY=50,ARTIFICE=30","1,0,0,0,0,0"},
            {"Ponder","ALCHEMY=49,ARTIFICE=38,INFUSION=28,BASICS=5","2,1,0,0,0,0"},
            {"Rethink","ALCHEMY=36,ARTIFICE=27,INFUSION=17,BASICS=1","-1,1,0,0,0,0"},
            {"Experimentation","ALCHEMY=40,ARTIFICE=30,INFUSION=20,BASICS=16","2,0,0,0,0,0"},
            {"Curio","ALCHEMY=40,ARTIFICE=30,INFUSION=20,BASICS=10,AUROMANCY=25","1,0,0,0,0,0"},
            {"Inspired","ALCHEMY=70,ARTIFICE=30,INFUSION=20","2,0,0,0,0,0"},
            {"Enchantment","ALCHEMY=40,ARTIFICE=30,INFUSION=35,AUROMANCY=15","1,0,0,0,0,0"},
            {"Beacon","ALCHEMY=40,ARTIFICE=30,INFUSION=20","-2,1,1,0,0,0"},
            {"Celestial","ALCHEMY=65,ARTIFICE=30,INFUSION=20","1,1,1,0,0,0"},
            {"Concentrate","ALCHEMY=55,ARTIFICE=30,INFUSION=20","1,1,0,0,0,0"},
            {"Reactions","ALCHEMY=65,ARTIFICE=30,INFUSION=20","1,0,0,0,0,0"},
            {"Synthesis","ALCHEMY=80,ARTIFICE=30,INFUSION=20","1,0,0,0,0,0"},
            {"Calibrate","ALCHEMY=40,ARTIFICE=45,INFUSION=20","1,1,0,0,0,0"},
            {"MindOverMatter","ALCHEMY=40,ARTIFICE=47,INFUSION=20","1,0,0,0,0,0"},
            {"Tinker","ALCHEMY=40,ARTIFICE=44,INFUSION=20","1,0,0,0,0,0"},
            {"Measure","ALCHEMY=40,ARTIFICE=30,INFUSION=35","1,1,0,0,0,0"},
            {"Channel","ALCHEMY=40,ARTIFICE=30,INFUSION=45","1,0,0,0,0,0"},
            {"Infuse","ALCHEMY=40,ARTIFICE=30,INFUSION=40","1,0,0,0,0,0"},
            {"Focus","ALCHEMY=40,ARTIFICE=30,INFUSION=20,AUROMANCY=15","1,1,0,0,0,0"},
            {"Awareness","ALCHEMY=40,ARTIFICE=30,INFUSION=20,AUROMANCY=20","1,0,0,0,0,0"},
            {"Spellbinding","ALCHEMY=40,ARTIFICE=30,INFUSION=20,AUROMANCY=25","1,0,0,0,0,0"},
            {"Sculpting","ALCHEMY=40,ARTIFICE=30,INFUSION=20,GOLEMANCY=20","1,1,0,0,0,0"},
            {"Scripting","ALCHEMY=40,ARTIFICE=30,INFUSION=20,GOLEMANCY=25","1,0,0,0,0,0"},
            {"Synergy","ALCHEMY=35,ARTIFICE=25,INFUSION=15,GOLEMANCY=30","1,0,1,0,0,0"},
            {"DarkWhispers","ALCHEMY=40,ARTIFICE=30,INFUSION=20,ELDRITCH=6","1,0,0,5,0,0"},
            {"Glyphs","ALCHEMY=40,ARTIFICE=30,INFUSION=20,ELDRITCH=10,BASICS=10","1,0,0,0,5,0"},
            {"Portal","ALCHEMY=40,ARTIFICE=30,INFUSION=20,ELDRITCH=5,BASICS=10","-1,2,0,1,5,0"},
            {"Revelation","ALCHEMY=40,ARTIFICE=30,INFUSION=20,ELDRITCH=30,BASICS=5","1,0,1,1,5,0"},
            {"Realization","ALCHEMY=40,ARTIFICE=30,INFUSION=20,ELDRITCH=15,BASICS=10","1,0,0,0,5,0"}
        };
        var random=new Random(){public int nextInt(int bound){return 0;}public float nextFloat(){return .9f;}public boolean nextBoolean(){return false;}};
        var ctx=new Context(true);
        for(var row:cases){String kind=row[0];var s=prepared();s.inspiration=7;TheoryCards.Card c=TheoryCards.initialize(kind,91,true,s,ctx);
            if(kind.equals("Analyze"))c=new TheoryCards.Card(kind,91,false,"ALCHEMY","",0,"",List.of());
            if(kind.equals("Curio"))c=new TheoryCards.Card(kind,91,false,"","",0,"",List.of(TheoryCards.Requirement.item("thaumcraft:curio_arcane",true)));
            if(kind.equals("Celestial"))c=new TheoryCards.Card(kind,91,false,"ALCHEMY","",0,"",List.of(TheoryCards.Requirement.note(0),TheoryCards.Requirement.note(5)));
            s.choices.add(c);var result=TheoryCards.activate(s,0,ctx,random);assertNotNull(result,kind);Map<String,Integer> expected=new TreeMap<>();for(String pair:row[1].split(",")){var parts=pair.split("=");expected.put(parts[0],Integer.parseInt(parts[1]));}assertEquals(expected,result.next().totals,kind);
            int[] v=Arrays.stream(row[2].split(",")).mapToInt(Integer::parseInt).toArray();assertEquals(7-v[0],result.next().inspiration,kind);assertEquals(v[1],result.next().bonusDraws,kind);assertEquals(v[2],result.next().penaltyStart,kind);assertEquals(v[3],result.normalWarp(),kind);assertEquals(v[4],result.temporaryWarp(),kind);assertEquals(v[5],result.observation(),kind);
            if(kind.equals("Enchantment")||kind.equals("Spellbinding"))assertEquals(5,result.levels());if(kind.equals("DarkWhispers"))assertEquals(40,result.levels());if(kind.equals("Synthesis"))assertEquals(c.aspect(),result.outputs().getFirst().aspect());
        }
    }
}
