package thaumcraft.port.theory;

import java.util.*;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.research.ResearchState;

/** Original registered BETA26 card effects, separated from inventory/network transactions. */
public final class TheoryCards {
    public static final List<String> KINDS=List.of("Study","Analyze","Balance","Notation","Ponder","Rethink","Reject","Experimentation","Curio","Inspired","Enchantment","Beacon","Celestial","Concentrate","Reactions","Synthesis","Calibrate","MindOverMatter","Tinker","Measure","Channel","Infuse","Focus","Awareness","Spellbinding","Sculpting","Scripting","Synergy","DarkWhispers","Glyphs","Portal","Revelation","Realization");
    public static final List<String> CURIOS=List.of("arcane","preserved","ancient","eldritch","knowledge","twisted","rites");
    public static final List<String> CURIO_CATEGORIES=List.of("AUROMANCY","ALCHEMY","GOLEMANCY","ELDRITCH","INFUSION","ARTIFICE","ELDRITCH");
    private static final List<Aspect> COMPOUNDS=Arrays.stream(Aspect.values()).filter(a->!a.isPrimal()).toList();
    public static final List<String> TINKER=List.of("thaumcraft:vis_resonator","thaumcraft:thaumometer","minecraft:anvil","minecraft:activator_rail","minecraft:dispenser","minecraft:dropper","minecraft:enchanting_table","minecraft:ender_chest","minecraft:jukebox","minecraft:daylight_detector","minecraft:piston","minecraft:hopper","minecraft:sticky_piston","minecraft:map","minecraft:compass","minecraft:tnt_minecart","minecraft:comparator","minecraft:clock");
    public static final List<String> INFUSE=List.of("thaumcraft:alumentum","thaumcraft:nitor_yellow","thaumcraft:amber","thaumcraft:brain","thaumcraft:fabric","thaumcraft:salis_mundus","thaumcraft:ingot_thaumium","thaumcraft:ingot_brass","thaumcraft:quicksilver","minecraft:gold_ingot","minecraft:iron_ingot","minecraft:diamond","minecraft:emerald","minecraft:blaze_rod","minecraft:leather","minecraft:white_wool","minecraft:brick","minecraft:arrow","minecraft:egg","minecraft:feather","minecraft:glowstone_dust","minecraft:redstone","minecraft:ghast_tear","minecraft:gunpowder","minecraft:bow","minecraft:golden_sword","minecraft:iron_sword","minecraft:iron_pickaxe","minecraft:golden_pickaxe","minecraft:quartz","minecraft:apple");
    public interface Context {
        ResearchState research();int levels();List<String> curios();int aspectTotal(String item);
    }
    public record Requirement(String item,String aspect,int kind,boolean consumed){
        public static Requirement item(String id,boolean consume){return new Requirement(id,"",-1,consume);}
        public static Requirement crystal(Aspect aspect,boolean consume){return new Requirement("thaumcraft:crystal_essence",aspect.id(),-1,consume);}
        public static Requirement phial(Aspect aspect,boolean consume){return new Requirement("thaumcraft:phial",aspect.id(),-1,consume);}
        public static Requirement note(int kind){return new Requirement("thaumcraft:celestial_notes","",kind,true);}
    }
    public record Card(String kind,long seed,boolean fromAid,String category,String other,int value,String aspect,List<Requirement> required){}
    public record Result(TheorySession next,int levels,int observation,String observationCategory,int normalWarp,int temporaryWarp,List<Requirement> outputs){}
    public static String researchCategory(Card c){return c.kind().equals("Notation")||c.kind().equals("Reject")?"":c.category();}
    public static int cost(String kind){return switch(kind){case "Analyze","Ponder","Experimentation","Inspired"->2;case "Rethink","Portal"->-1;case "Beacon"->-2;case "Reject"->0;default->1;};}
    public static boolean aidOnly(String kind){return Set.of("Study","Notation","Enchantment","Beacon","DarkWhispers","Glyphs","Portal").contains(kind);}
    public static String category(String kind){return switch(kind){case "Concentrate","Reactions","Synthesis"->"ALCHEMY";case "Calibrate","Tinker","MindOverMatter"->"ARTIFICE";case "Measure","Channel","Infuse"->"INFUSION";case "Focus","Awareness","Spellbinding"->"AUROMANCY";case "Sculpting","Scripting","Synergy"->"GOLEMANCY";case "DarkWhispers","Glyphs","Portal","Revelation","Realization"->"ELDRITCH";default->"";};}
    public static Card initialize(String kind,long seed,boolean aid,TheorySession s,Context ctx){
        if(!KINDS.contains(kind))return null;
        Random r=new Random(seed);String cat=category(kind),other="",aspect="";int value=0;List<Requirement> items=new ArrayList<>();
        switch(kind){
            case "Study"->{var available=s.availableCategories(ctx.research());if(available.isEmpty())return null;cat=pick(available,r);}
            // BETA26 queries observation for null, not the loop category. Its untyped pool is never awarded.
            case "Analyze"->{return null;}
            case "Balance"->{int n=(int)s.totals.keySet().stream().filter(c->!s.blocked.contains(c)).count();if(s.blocked.size()>=s.totals.size()-1||s.totals.entrySet().stream().filter(e->!s.blocked.contains(e.getKey())).mapToInt(Map.Entry::getValue).sum()<n)return null;}
            case "Notation"->{if(s.totals.size()<2)return null;cat=s.totals.entrySet().stream().min(Map.Entry.comparingByValue()).orElseThrow().getKey();other=s.highest();if(cat.equals(other))return null;}
            case "Ponder"->{if(s.blocked.size()>=s.totals.size())return null;}
            case "Rethink"->{if(s.totals.values().stream().mapToInt(Integer::intValue).sum()<10)return null;}
            case "Reject"->{var cats=s.totals.keySet().stream().filter(c->!s.blocked.contains(c)).toList();if(cats.isEmpty())return null;cat=pick(cats,r);}
            case "Curio"->{if(ctx.curios().isEmpty())return null;String id=pick(ctx.curios(),r);items.add(Requirement.item(id,true));}
            case "Inspired"->{cat=s.highest();if(cat.isEmpty())return null;value=10+s.totals.get(cat)/2;}
            case "Celestial"->{cat=s.highest();if(cat.isEmpty()||!ctx.research().knows("CELESTIALSCANNING"))return null;int first=r.nextInt(13),second;do{second=r.nextInt(13);}while(first==second);items.add(Requirement.note(first));items.add(Requirement.note(second));}
            case "Concentrate","Reactions","Synthesis","Channel","Infuse"->{
                Aspect a=pick(COMPOUNDS,r);aspect=a.id();
                if(kind.equals("Synthesis")){items.add(Requirement.crystal(a.components().get(0),true));items.add(Requirement.crystal(a.components().get(1),true));}
                else if(kind.equals("Infuse")){String id=pick(INFUSE,r);items.add(Requirement.item(id,true));items.add(Requirement.phial(a,true));value=(int)(10+Math.sqrt(ctx.aspectTotal(id))*1.5);}
                else if(kind.equals("Channel"))items.add(Requirement.phial(a,false));
                else {items.add(Requirement.crystal(a,false));if(kind.equals("Reactions")){Aspect b;do{b=pick(COMPOUNDS,r);}while(a==b);items.add(Requirement.crystal(b,false));}}
            }
            case "Tinker","MindOverMatter"->{String id=pick(TINKER,r);items.add(Requirement.item(id,kind.equals("MindOverMatter")));value=(int)Math.sqrt(ctx.aspectTotal(id));}
            case "Spellbinding"->{if(ctx.levels()<1)return null;}
            case "Sculpting"->items.add(Requirement.item("minecraft:clay_ball",true));
            case "Synergy"->{if(s.totals.getOrDefault("ARTIFICE",0)+s.totals.getOrDefault("ALCHEMY",0)+s.totals.getOrDefault("INFUSION",0)<15)return null;}
        }
        return new Card(kind,seed,aid,cat,other,value,aspect,List.copyOf(items));
    }
    public static void validate(Card c){
        if(c==null||!KINDS.contains(c.kind())||c.category()==null||!c.category().isEmpty()&&!TheorySession.CATEGORIES.contains(c.category())||c.other()==null||c.aspect()==null||c.required()==null||c.value()<0||c.value()>1000000)throw new IllegalArgumentException("card");
        int required=switch(c.kind()){case "Concentrate","Channel","Tinker","MindOverMatter","Curio","Sculpting"->1;case "Reactions","Synthesis","Infuse","Celestial"->2;default->0;};
        if(c.required().size()!=required||!c.aspect().isEmpty()&&Aspect.byId(c.aspect()).isEmpty())throw new IllegalArgumentException("card parameters");
        for(var q:c.required()){
            if(q==null||q.item()==null||q.item().length()>128||!q.item().matches("[a-z0-9_.-]+:[a-z0-9_./-]+")||q.aspect()==null||!q.aspect().isEmpty()&&Aspect.byId(q.aspect()).isEmpty()||q.kind()< -1||q.kind()>12)throw new IllegalArgumentException("card requirement");
            if(q.item().equals("thaumcraft:celestial_notes")!=(q.kind()>=0))throw new IllegalArgumentException("celestial kind");
            if(Set.of("thaumcraft:crystal_essence","thaumcraft:phial").contains(q.item())==q.aspect().isEmpty())throw new IllegalArgumentException("aspect component");
        }
    }
    public static Card refreshValue(Card c,Context context){
        if(c==null||!Set.of("Tinker","MindOverMatter","Infuse").contains(c.kind()))return c;
        double root=Math.sqrt(context.aspectTotal(c.required().getFirst().item()));int value=c.kind().equals("Infuse")?(int)(10+root*1.5):(int)root;
        return c.value()==value?c:new Card(c.kind(),c.seed(),c.fromAid(),c.category(),c.other(),value,c.aspect(),c.required());
    }
    /** Does not mutate the source or the player. The host first reserves all inventory requirements. */
    public static Result activate(TheorySession source,int index,Context ctx,Random r){
        if(source.complete()||source.selected>=0||index<0||index>=source.choices.size())return null;
        Card c=refreshValue(source.choices.get(index),ctx);if(cost(c.kind())>source.inspiration)return null;
        if(c.kind().equals("Enchantment")&&ctx.levels()<5||c.kind().equals("Spellbinding")&&ctx.levels()<1||c.kind().equals("Analyze")&&ctx.research().knowledge("OBSERVATION",c.category())<16)return null;
        TheorySession s=source.copy();s.choices.set(index,c);int levels=0,obs=0,warp=0,temp=0;List<Requirement> outputs=new ArrayList<>();String cat=c.category();
        switch(c.kind()){
            case "Study"->s.add(cat,between(r,15,25));
            case "Analyze"->{s.add("BASICS",5);s.add(cat,between(r,25,50));obs=16;}
            case "Balance"->{var cats=s.totals.keySet().stream().filter(k->!s.blocked.contains(k)).toList();if(cats.isEmpty())return null;int average=cats.stream().mapToInt(s.totals::get).sum()/cats.size();cats.forEach(k->{s.totals.remove(k);s.add(k,average);});s.add("BASICS",5);s.penaltyStart++;}
            case "Notation"->{int amount=s.totals.getOrDefault(cat,0);s.totals.remove(cat);s.add(c.other(),amount/2+between(r,0,amount/2));}
            case "Ponder"->{var cats=s.totals.keySet().stream().filter(k->!s.blocked.contains(k)).toList();if(cats.isEmpty())return null;for(int i=0;i<25;i++)s.add(cats.get(i%cats.size()),1);s.add("BASICS",5);s.bonusDraws++;}
            case "Rethink"->{rethink(s);s.bonusDraws++;s.add("BASICS",between(r,1,10));}
            case "Reject"->{s.add("BASICS",5);s.blocked.add(cat);}
            case "Experimentation"->{s.add(randomCategory(r),between(r,15,30));s.add("BASICS",between(r,1,10));}
            case "Curio"->{s.add("BASICS",5);s.add(randomCategory(r),5);String id=c.required().getFirst().item();int type=CURIOS.indexOf(id.substring(id.lastIndexOf('_')+1));if(type==6){s.add("ELDRITCH",between(r,15,20));s.add("AUROMANCY",between(r,10,15));}else s.add(type>=0?CURIO_CATEGORIES.get(type):"BASICS",between(r,25,35));if(r.nextBoolean())s.bonusDraws++;if(r.nextBoolean())s.bonusDraws++;}
            case "Inspired"->s.add(cat,c.value());
            case "Enchantment"->{levels=5;s.add("INFUSION",between(r,15,20));s.add("AUROMANCY",between(r,15,20));}
            case "Beacon"->{s.bonusDraws++;s.penaltyStart++;}
            case "Celestial"->{s.add(cat,between(r,25,50));if(c.required().stream().anyMatch(q->q.kind()>0&&q.kind()<5)){int n=between(r,0,5);s.add("ELDRITCH",n*2);temp+=n;}if(c.required().stream().anyMatch(q->q.kind()==0))s.penaltyStart++;if(c.required().stream().anyMatch(q->q.kind()>4))s.bonusDraws++;}
            case "Concentrate"->{s.add(cat,15);s.bonusDraws++;if(r.nextFloat()<.33f)s.inspire(1);}
            case "Reactions"->{s.add(cat,25);if(r.nextFloat()<.33f)s.inspire(1);}
            case "Synthesis"->{s.add(cat,40);if(r.nextFloat()<.33f)s.inspire(1);outputs.add(Requirement.crystal(Aspect.byId(c.aspect()).orElseThrow(),false));}
            case "Calibrate","Measure","Focus"->{s.add(cat,15);s.bonusDraws++;}
            case "Tinker"->s.add(cat,between(r,c.value()*2,c.value()*2+10));
            case "MindOverMatter"->s.add(cat,10+c.value());
            case "Channel","Scripting"->s.add(cat,25);
            case "Infuse"->s.add(cat,c.value());
            case "Awareness"->{s.add(cat,20);if(r.nextFloat()<.33f){s.add("ELDRITCH",between(r,1,5));warp++;}}
            case "Spellbinding"->{levels=Math.min(5,ctx.levels());s.add(cat,levels*5);}
            case "Sculpting"->{s.add(cat,20);s.bonusDraws++;}
            case "Synergy"->{drain(s,List.of("ARTIFICE","ALCHEMY","INFUSION"),15);s.add(cat,30);s.penaltyStart++;}
            case "DarkWhispers"->{int l=ctx.levels();levels=10+l;if(l>0)for(String k:TheorySession.CATEGORIES)if(!r.nextBoolean())s.add(k,between(r,0,Math.max(1,(int)Math.sqrt(l))));s.add(cat,between(r,Math.max(1,l/5),Math.max(5,l/2)));warp+=Math.max(1,(int)Math.sqrt(l));if(r.nextBoolean())s.bonusDraws++;}
            case "Glyphs"->{s.add(randomCategory(r),between(r,10,20));s.add(cat,between(r,10,20));temp+=5;}
            case "Portal"->{s.add(randomCategory(r),between(r,5,10));s.add(randomCategory(r),between(r,5,10));s.add(cat,between(r,5,10));s.bonusDraws+=2;temp+=5;warp++;}
            case "Revelation"->{s.add(randomCategory(r),between(r,5,10));s.add(cat,30);temp+=5;warp++;s.penaltyStart++;}
            case "Realization"->{s.add(randomCategory(r),between(r,5,10));s.add(randomCategory(r),between(r,5,10));s.add(cat,15);temp+=5;if(r.nextBoolean())warp++;}
            default->throw new IllegalArgumentException(c.kind());
        }
        s.inspire(-cost(c.kind()));s.selected=index;
        return new Result(s,levels,obs,cat,warp,temp,List.copyOf(outputs));
    }
    private static void rethink(TheorySession s){
        int left=10;
        while(left>0)for(String category:new ArrayList<>(s.totals.keySet())){s.add(category,-1);if(--left<=0||!s.totals.containsKey(category))break;}
    }
    private static void drain(TheorySession s,List<String> categories,int amount){if(categories.stream().mapToInt(k->s.totals.getOrDefault(k,0)).sum()<amount)throw new IllegalArgumentException("insufficient theory");for(int i=0;amount>0;i++){String cat=categories.get(i%categories.size());if(s.totals.getOrDefault(cat,0)>0){s.add(cat,-1);amount--;}}}
    public static int between(Random r,int min,int max){return min>=max?min:min+r.nextInt(max-min+1);}
    private static <T>T pick(List<T> list,Random r){return list.get(r.nextInt(list.size()));}
    private static String randomCategory(Random r){return pick(TheorySession.CATEGORIES,r);}
    private TheoryCards(){}
}
