package thaumcraft.port;
import java.util.*;
import org.junit.jupiter.api.Test;
import thaumcraft.port.research.*;
import static org.junit.jupiter.api.Assertions.*;
class BookAndScannerTest {
    @Test void administrativeCompletionIncludesHiddenRecordsAndSurvivesReloadWithoutFakeScans(){
        var state=new ResearchState();state.stage("FIRSTSTEPS",1);state.flag("UNLOCKALCHEMY");state.addKnowledge("THEORY","BASICS",7);state.warp(3,2,1);
        assertEquals(148,state.completeCatalog());assertEquals(0,state.completeCatalog());
        var copy=ResearchCodec.read(ResearchCodec.write(state));
        for(var entry:ResearchCatalog.all()){assertTrue(copy.knows(entry.key()),entry.key());assertTrue(copy.stage(entry.key())>entry.stages().size(),entry.key());}
        assertTrue(copy.scanned().isEmpty());assertTrue(copy.discovered().isEmpty());assertEquals(7,copy.knowledge("THEORY","BASICS"));assertEquals(3,copy.permanentWarp());
    }
    @Test void markupProducesImagesRatherThanResourcePathText(){
        var elements=BookDocument.parse("Before<BR/><IMG>thaumcraft:textures/gui/gui_researchbook.png:24:184:95:6:1</IMG>\n<PAGE>After",String::length,9);
        assertTrue(elements.stream().anyMatch(e->e.image()!=null&&e.image().u()==24&&e.image().height()==6));
        assertTrue(elements.stream().noneMatch(e->e.text()!=null&&e.text().contains("textures/")));
        var pages=BookDocument.paginate(elements,182);assertEquals(2,pages.size());assertEquals("After",pages.get(1).getFirst().element().text());
    }
    @Test void pageBreaksAndReservedRequirementAreaDoNotLoseContent(){
        var source="one two three four five ".repeat(30);var elements=BookDocument.parse(source,s->s.length()*6,9);var pages=BookDocument.paginate(elements,65);
        assertTrue(pages.size()>1);assertTrue(pages.getFirst().stream().allMatch(p->p.y()+p.element().height()<=65));
        assertEquals(elements.size(),pages.stream().mapToInt(List::size).sum());
    }
    @Test void everyOriginalLocalizedPageParsesIncludingInlineIllustrations(){
        for(String lang:List.of("en_us","ru_ru")){var strings=ResearchCatalog.read("/assets/thaumcraft/lang/"+lang+".json");
            for(var e:ResearchCatalog.all())for(var stage:e.stages()){String key=stage.get("text").getAsString();assertTrue(strings.has(key),key);assertDoesNotThrow(()->BookDocument.paginate(BookDocument.parse(strings.get(key).getAsString(),s->s.length()*6,9),110),key);}
        }
    }
    @Test void knowledgeTableAndRequirementsReserveTheOriginalFirstColumnHeight(){
        assertEquals(182,BookDocument.firstPageHeight(false,2,0));
        assertEquals(168,BookDocument.firstPageHeight(true,0,0));
        assertEquals(128,BookDocument.firstPageHeight(true,2,0));
        assertEquals(107,BookDocument.firstPageHeight(true,2,1));
        var elements=BookDocument.parse("knowledge ".repeat(100),s->s.length()*6,9);
        var pages=BookDocument.paginate(elements,BookDocument.firstPageHeight(true,2,0));
        assertTrue(pages.getFirst().stream().allMatch(p->p.y()+p.element().height()<=128));
        assertEquals(elements.size(),pages.stream().mapToInt(List::size).sum());
    }
    @Test void wrappedOriginalFormattingKeepsUnderlineAndHonorsReset(){
        java.util.function.ToIntFunction<String> width=s->s.replaceAll("§.","").length()*10;
        var lines=BookDocument.parse("§nВспомогательные средства при создании теорий§r",width,9);
        assertTrue(lines.size()>2);assertTrue(lines.stream().allMatch(e->e.text().startsWith("§n")));
        assertTrue(lines.stream().allMatch(e->width.applyAsInt(e.text())<=140));
        var reset=BookDocument.parse("§a"+"A".repeat(14)+"§r"+"B".repeat(14),width,9);
        assertEquals(2,reset.size());assertEquals("§a§r"+"B".repeat(14),reset.get(1).text());
    }
    @Test void fractionalIllustrationDimensionsMatchOriginalIntegerTruncation(){
        var image=BookDocument.Image.parse("thaumcraft:textures/gui/gui_researchbook.png:0:0:95:7:0.5");
        assertEquals(47,image.displayWidth());assertEquals(3,image.displayHeight());
    }
    @Test void multiblockTabsKeepCompletedTargetsAndOriginalDisplayStackException(){
        var recipes=ResearchCatalog.read("/thaumcraft-port/book-recipes.json").getAsJsonObject("recipes");
        var blueprints=recipes.entrySet().stream().map(e->e.getValue().getAsJsonObject()).filter(r->r.get("type").getAsString().equals("thaumcraft:blueprint")).toList();
        assertEquals(6,blueprints.size());assertEquals(1,blueprints.stream().filter(r->r.get("display_stack").getAsBoolean()).count());
        for(var r:blueprints){assertEquals(r.getAsJsonArray("layers").size(),r.getAsJsonArray("target_layers").size());}
        var altar=recipes.getAsJsonObject("thaumcraft:infusionaltar");
        assertEquals("thaumcraft:stone_arcane",blueprintCell(altar,"layers",1,0,0));
        assertEquals("minecraft:air",blueprintCell(altar,"target_layers",1,0,0));
        assertEquals("thaumcraft:pillar_arcane;1;3",blueprintCell(altar,"target_layers",2,0,0));
        assertEquals("thaumcraft:pillar_arcane;1;2",blueprintCell(altar,"target_layers",2,0,2));
        assertEquals("thaumcraft:pillar_arcane;1;0",blueprintCell(altar,"target_layers",2,2,0));
        assertEquals("thaumcraft:pillar_arcane;1;1",blueprintCell(altar,"target_layers",2,2,2));
        var furnace=recipes.getAsJsonObject("thaumcraft:infernalfurnace");
        assertEquals("minecraft:nether_bricks;12;0",furnace.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("legacy").getAsString());
        assertEquals("minecraft:lava",blueprintCell(furnace,"layers",1,1,1));
        assertEquals("thaumcraft:infernal_furnace",blueprintCell(furnace,"target_layers",1,1,1));
        assertEquals("minecraft:air",blueprintCell(furnace,"target_layers",1,2,1));
        var thaumatorium=recipes.getAsJsonObject("thaumcraft:Thaumatorium");
        assertEquals("thaumcraft:thaumatorium_top",blueprintCell(thaumatorium,"target_layers",0,0,0));
        assertEquals("thaumcraft:thaumatorium",blueprintCell(thaumatorium,"target_layers",1,0,0));
        var press=recipes.getAsJsonObject("thaumcraft:GolemPress");
        assertTrue(press.get("display_stack").getAsBoolean());assertEquals("minecraft:piston;1;1",blueprintCell(press,"layers",1,1,0));
    }
    private static String blueprintCell(com.google.gson.JsonObject recipe,String layers,int y,int x,int z){return recipe.getAsJsonArray(layers).get(y).getAsJsonArray().get(x).getAsJsonArray().get(z).getAsString();}
    @Test void oversizedForCurrentColumnIllustrationsWaitWhileLaterTextFillsIt(){
        var large=new BookDocument.Element(null,new BookDocument.Image("thaumcraft:large.png",0,0,20,20,1),22,false);
        var small=new BookDocument.Element(null,new BookDocument.Image("thaumcraft:small.png",0,0,6,6,1),8,false);
        var first=new BookDocument.Element("first",null,9,false);var second=new BookDocument.Element("second",null,9,false);var third=new BookDocument.Element("third",null,9,false);
        var pages=BookDocument.paginate(List.of(first,large,small,second,third),27);
        assertEquals(2,pages.size());assertEquals(List.of(first,second,third),pages.get(0).stream().map(BookDocument.Placed::element).toList());
        assertEquals(List.of(large,small),pages.get(1).stream().map(BookDocument.Placed::element).toList());assertEquals(22,pages.get(1).get(1).y());
    }
    @Test void explicitPageBreakPlacesWaitingIllustrationBeforeFollowingText(){
        var elements=BookDocument.parse("before<IMG>thaumcraft:textures/test.png:0:0:20:20:1</IMG>\n<PAGE>after",String::length,9);
        var pages=BookDocument.paginate(elements,27);
        assertEquals(2,pages.size());assertEquals(1,pages.get(0).size());assertNotNull(pages.get(1).get(0).element().image());
        assertEquals("after",pages.get(1).get(1).element().text());assertEquals(22,pages.get(1).get(1).y());
    }
    @Test void paragraphSpacingStaysAttachedToTheLineAcrossPageBoundaries(){
        var elements=BookDocument.parse("first<BR>second",String::length,9);assertEquals(2,elements.size());assertEquals(14,elements.getFirst().height());
        var pages=BookDocument.paginate(elements,20);assertEquals(2,pages.size());assertEquals("second",pages.get(1).getFirst().element().text());assertEquals(0,pages.get(1).getFirst().y());
    }
    @Test void hiddenParagraphMarkerParticipatesInWrappingBeforeItIsRemoved(){
        // The visible paragraph fits 140 px, but TC6 wraps with the additional ~B glyphs.
        var elements=BookDocument.parse("alpha beta gamma delta<BR>next",s->s.length()*6,9);
        assertEquals("alpha beta gamma",elements.get(0).text());assertEquals("delta",elements.get(1).text());assertEquals(14,elements.get(1).height());
        assertTrue(elements.stream().noneMatch(e->e.text()!=null&&e.text().contains("~B")));
        var pages=BookDocument.paginate(elements,27);assertEquals(2,pages.size());assertEquals("next",pages.get(1).getFirst().element().text());
    }
    @Test void recipeGatesAllowPartialStagesAndRequireBothAdvancedSealResearches(){
        var state=new ResearchState();assertTrue(BookRecipeCatalog.recipes("thaumcraft:brassingot",state).isEmpty());
        state.stage("METALLURGY",1);assertFalse(state.knows("METALLURGY"));assertEquals(1,BookRecipeCatalog.recipes("thaumcraft:brassingot",state).size());
        state.flag("SEALCOLLECT");assertTrue(BookRecipeCatalog.recipes("thaumcraft:SealCollectAdv",state).isEmpty());
        state.flag("MINDBIOTHAUMIC");assertEquals(1,BookRecipeCatalog.recipes("thaumcraft:SealCollectAdv",state).size());
        assertTrue(BookRecipeCatalog.recipes("thaumcraft:viscrystalgroup",state).isEmpty());state.flag("BASEALCHEMY");assertEquals(37,BookRecipeCatalog.recipes("thaumcraft:viscrystalgroup",state).size());
    }
    @Test void ingredientDrilldownRetainsGroupVariantWithoutRequiringOwningEntryCompletion(){
        var state=new ResearchState();String output="thaumcraft:nitor_light_blue;1;0";
        var reference=BookRecipeCatalog.reference(output,state,BookRecipeCatalog::itemIdentity);
        assertNotNull(reference);assertEquals("thaumcraft:nitorgroup",reference.recipe());assertEquals(3,reference.variant());
        assertEquals(output,BookRecipeCatalog.output(BookRecipeCatalog.recipes(reference.recipe(),state).get(reference.variant())));
        assertFalse(state.knows("BASEALCHEMY"));
        String crystal="thaumcraft:crystal_essence;1;0;{Aspects:[{key:\"aqua\",amount:1}]}";
        assertNull(BookRecipeCatalog.reference(crystal,state,BookRecipeCatalog::itemIdentity));
        // Original ignores aspect NBT and selects the first crystal with the same item and metadata.
        state.flag("BASEALCHEMY");assertEquals(0,BookRecipeCatalog.reference(crystal,state,BookRecipeCatalog::itemIdentity).variant());
        assertEquals(3,BookRecipeCatalog.variant("thaumcraft:nitorgroup","thaumcraft:nitor_light_blue;64;0;{display:{Name:\"renamed\"}}",state,BookRecipeCatalog::itemIdentity));
        assertEquals(-1,BookRecipeCatalog.variant("thaumcraft:nitorgroup","thaumcraft:nitor_light_blue;1;2",state,BookRecipeCatalog::itemIdentity));
    }
    @Test void lockedFirstDirectRecipeStopsBeforeAnUnlockedAlternative(){
        var state=new ResearchState();String yellow="thaumcraft:nitor_yellow;1;0";
        assertEquals(4,BookRecipeCatalog.variant("thaumcraft:nitorgroup",yellow,state,BookRecipeCatalog::itemIdentity));
        var locked=BookRecipeCatalog.reference(yellow,state,BookRecipeCatalog::itemIdentity);
        assertNotNull(locked);assertTrue(locked.locked());assertEquals("thaumcraft:nitor",locked.recipe());
        state.stage("UNLOCKALCHEMY",3);var unlocked=BookRecipeCatalog.reference(yellow,state,BookRecipeCatalog::itemIdentity);
        assertNotNull(unlocked);assertFalse(unlocked.locked());assertEquals("thaumcraft:nitor",unlocked.recipe());
    }
    @Test void referenceSearchUsesStageRecipesWithoutAddingAddenda(){
        var state=new ResearchState();state.completeCatalog();var advanced=BookRecipeCatalog.recipes("thaumcraft:SealCollectAdv",state);
        assertEquals(1,advanced.size());
        // Preserve NBT in this lookup-only fixture to distinguish the addendum's seal
        // from stage seals, independently of the production item+metadata comparator.
        assertNull(BookRecipeCatalog.reference(BookRecipeCatalog.output(advanced.getFirst()),state,java.util.function.UnaryOperator.identity()));
    }
    @Test void wildcardWoolIngredientsRetainAllColorsAndFixedBlackBannerStaysBlack(){
        var recipes=ResearchCatalog.read("/thaumcraft-port/book-recipes.json").getAsJsonObject("recipes");
        var fabric=recipes.getAsJsonObject("thaumcraft:EnchantedFabric");var wool=fabric.getAsJsonObject("key").getAsJsonArray("C");
        assertEquals(16,wool.size());assertEquals(16,wool.asList().stream().map(e->e.getAsJsonObject().get("legacy").getAsString()).distinct().count());
        assertEquals("minecraft:white_wool;1",wool.get(0).getAsJsonObject().get("legacy").getAsString());assertEquals("minecraft:black_wool;1",wool.get(15).getAsJsonObject().get("legacy").getAsString());
        assertEquals("thaumcraft:fabric;1;0",fabric.getAsJsonObject("result").get("legacy").getAsString());
        var butcher=recipes.getAsJsonObject("thaumcraft:SealButcher").getAsJsonArray("ingredients");assertTrue(butcher.asList().stream().anyMatch(e->e.isJsonArray()&&e.getAsJsonArray().equals(wool)));
        assertTrue(recipes.getAsJsonObject("thaumcraft:Bannerblack").toString().contains("minecraft:black_wool;1"));
        assertFalse(recipes.getAsJsonObject("thaumcraft:Bannerblack").toString().contains("minecraft:white_wool;1"));
    }
    @Test void hiddenResearchWithSatisfiedParentsBecomesReachable(){
        var state=new ResearchState();assertFalse(ResearchView.visible(state,ResearchCatalog.get("BASEALCHEMY")));
        state.flag("UNLOCKALCHEMY");assertTrue(state.available(ResearchCatalog.get("BASEALCHEMY")));assertTrue(ResearchView.visible(state,ResearchCatalog.get("BASEALCHEMY")));
        state.stage("FLUX",1);assertTrue(ResearchView.visible(state,ResearchCatalog.get("FLUX")));
        assertFalse(ResearchView.visible(state,ResearchCatalog.get("WARP")));state.flag("WARP");assertTrue(ResearchView.visible(state,ResearchCatalog.get("WARP")));
    }
    @Test void originalCategoryGatesRemainInPlace(){var s=new ResearchState();assertTrue(ResearchView.category(s,"BASICS"));assertFalse(ResearchView.category(s,"ELDRITCH"));s.flag("UNLOCKELDRITCH");assertTrue(ResearchView.category(s,"ELDRITCH"));}
    @Test void heavenlyTargetUsesOriginalTolerancesAndFourStarDirections(){
        assertEquals("Sun",CelestialTarget.find(-90,-90,0,0,0).name());
        assertNull(CelestialTarget.find(0,10,0,0,0));assertNull(CelestialTarget.find(0,-45,0,0,0));
        assertEquals(7,CelestialTarget.find(-90,-90,.5f,2,0).variant());
        for(int direction=0;direction<4;direction++)assertEquals(direction+1,CelestialTarget.find(0,-45,.5f,0,direction).variant());
        assertNull(CelestialTarget.find(-79,-90,0,0,0));assertNull(CelestialTarget.find(-90,-82,0,0,0));
    }
    @Test void dailySkyFlagsSurviveSavingAndOldDaysArePruned(){
        var target=new CelestialTarget("Sun",0);var state=new ResearchState();state.flag(target.key(24000));state.flag("FIRSTSTEPS");state.flag("CEL_0_Star0");
        state.pruneCelestial(1);var copy=ResearchCodec.read(ResearchCodec.write(state));assertTrue(copy.knows(target.key(47000)));assertFalse(copy.knows(target.key(48000)));assertFalse(copy.knows("CEL_0_Star0"));assertTrue(copy.knows("FIRSTSTEPS"));
    }
    @Test void legacyWispSubtypesDoNotAwardFreshGenericScans(){var s=new ResearchState(List.of("entity:thaumcraft:wisp:aer","entity:thaumcraft:wisp:terra"),List.of());assertEquals(Set.of("entity:thaumcraft:wisp"),s.scanned());}
    @Test void presentationFlagsCannotCompleteResearch(){var s=new ResearchState();s.stage("FIRSTSTEPS",1);s.setResearchFlag("FIRSTSTEPS",ResearchState.ResearchFlag.PAGE);s.clearReadFlags("FIRSTSTEPS");assertFalse(s.knows("FIRSTSTEPS"));assertEquals(1,s.stage("FIRSTSTEPS"));assertFalse(s.flag("ui_seen:FIRSTSTEPS:4:0"));assertFalse(s.knows("FIRSTSTEPS"));}
}
