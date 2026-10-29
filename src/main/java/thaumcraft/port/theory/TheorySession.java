package thaumcraft.port.theory;

import com.google.gson.Gson;
import java.util.*;
import thaumcraft.port.research.ResearchState;
import thaumcraft.port.research.ResearchCatalog;

/** TC6 BETA26 theory state. All random decisions and mutations belong to the server. */
public final class TheorySession {
    public static final List<String> CATEGORIES=List.of("BASICS","AUROMANCY","ALCHEMY","ARTIFICE","INFUSION","GOLEMANCY","ELDRITCH");
    private static final Gson JSON=new Gson();
    public UUID owner;
    public int inspirationStart,inspiration,bonusDraws,penaltyStart;
    public TreeMap<String,Integer> totals=new TreeMap<>();
    public LinkedHashSet<String> blocked=new LinkedHashSet<>(),aids=new LinkedHashSet<>();
    public ArrayList<String> aidCards=new ArrayList<>();
    public ArrayList<TheoryCards.Card> choices=new ArrayList<>();
    public ArrayList<Long> savedCards=new ArrayList<>();
    public TheoryCards.Card lastDraw;
    public int selected=-1;

    public TheorySession(){}
    public TheorySession(UUID owner,int inspiration,Collection<String> aids){
        this.owner=Objects.requireNonNull(owner);this.inspirationStart=Math.clamp(inspiration,1,15);
        this.aids.addAll(aids);if(aids.size()>=inspirationStart||!TheoryAids.DEFINITIONS.keySet().containsAll(aids))throw new IllegalArgumentException("aids");
        this.inspiration=inspirationStart-aids.size();
        aids.forEach(a->aidCards.addAll(TheoryAids.DEFINITIONS.get(a).cards()));
    }
    public static int availableInspiration(ResearchState state){
        float amount=5;for(var e:ResearchCatalog.all())if(state.knows(e.key())){if(e.meta().contains("SPIKY"))amount+=.5f;if(e.meta().contains("HIDDEN"))amount+=.1f;}
        return Math.min(15,Math.round(amount));
    }
    public boolean complete(){return inspiration<=0;}
    public void add(String category,int amount){if(!CATEGORIES.contains(category))throw new IllegalArgumentException(category);int n=Math.clamp((long)totals.getOrDefault(category,0)+amount,0,1000000);if(n==0)totals.remove(category);else totals.put(category,n);}
    public void inspire(int n){inspiration=Math.min(inspirationStart,inspiration+n);}
    public String highest(){return totals.entrySet().stream().max(Comparator.<Map.Entry<String,Integer>>comparingInt(Map.Entry::getValue).thenComparing(Map.Entry::getKey,Comparator.reverseOrder())).map(Map.Entry::getKey).orElse("");}
    public List<String> availableCategories(ResearchState state){return CATEGORIES.stream().filter(c->!blocked.contains(c)&&(c.equals("BASICS")||state.knows("UNLOCK"+c))).toList();}
    public boolean draw(TheoryCards.Context context,Random random){
        if(complete()||!choices.isEmpty())return false;
        int count=bonusDraws>0?3:2;if(count==3)bonusDraws--;
        int attempts=0;
        for(int slot=0;slot<count;slot++)while(attempts++<10000){
            boolean fromAid=!aidCards.isEmpty()&&random.nextFloat()<=.25f;
            int aidIndex=fromAid?random.nextInt(aidCards.size()):-1;
            String kind=fromAid?aidCards.get(aidIndex):TheoryCards.KINDS.get(random.nextInt(TheoryCards.KINDS.size()));
            var card=TheoryCards.initialize(kind,Math.abs(random.nextLong()),fromAid,this,context);
            if(card==null||TheoryCards.cost(kind)>inspiration||blocked.contains(TheoryCards.researchCategory(card))||choices.stream().anyMatch(c->c.kind().equals(kind)))continue;
            if(!fromAid&&(TheoryCards.aidOnly(kind)||!TheoryCards.researchCategory(card).isEmpty()&&!availableCategories(context.research()).contains(TheoryCards.researchCategory(card))))continue;
            choices.add(card);if(fromAid)aidCards.remove(aidIndex);break;
        }
        return !choices.isEmpty();
    }
    public void acknowledge(){
        if(selected<0||selected>=choices.size())return;
        if(lastDraw!=null){savedCards.add(lastDraw.seed());if(savedCards.size()>256)savedCards.removeFirst();}
        lastDraw=choices.get(selected);choices.clear();selected=-1;
    }
    /** Raw knowledge, not whole theory units. Stable ties retain TC6's TreeMap order. */
    public Map<String,Integer> rewards(){
        Map<String,Integer> result=new LinkedHashMap<>();if(!complete())return result;
        var sorted=new ArrayList<>(totals.entrySet());sorted.sort(Map.Entry.<String,Integer>comparingByValue().reversed());
        int rank=0;for(var e:sorted){int amount=Math.round(e.getValue()/100f*32);if(rank++>penaltyStart)amount=(int)Math.max(1,amount*.666666667);result.put(e.getKey(),amount);}return result;
    }
    public String encode(){return JSON.toJson(this);}
    public TheorySession copy(){return decode(encode());}
    public static TheorySession decode(String json){
        if(json==null||json.length()>60000)throw new IllegalArgumentException("theory size");
        var s=JSON.fromJson(json,TheorySession.class);
        if(s==null||s.owner==null||s.inspirationStart<1||s.inspirationStart>15||s.inspiration> s.inspirationStart||s.inspiration< -2||s.bonusDraws<0||s.bonusDraws>1000000||s.penaltyStart<0||s.penaltyStart>1000000||s.selected< -1||s.selected>=s.choices.size()||s.choices.size()>3||s.savedCards.size()>256||s.aidCards.size()>64||s.aids.size()>14||!TheoryAids.DEFINITIONS.keySet().containsAll(s.aids)||!CATEGORIES.containsAll(s.blocked)||!CATEGORIES.containsAll(s.totals.keySet())||s.totals.values().stream().anyMatch(n->n==null||n<1||n>1000000))throw new IllegalArgumentException("theory data");
        for(var c:s.choices)TheoryCards.validate(c);if(s.lastDraw!=null)TheoryCards.validate(s.lastDraw);
        for(String card:s.aidCards)if(!TheoryCards.KINDS.contains(card)&&!card.equals("Truth"))throw new IllegalArgumentException("aid card");
        return s;
    }
}
