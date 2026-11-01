package thaumcraft.port.client;

import java.util.*;
import thaumcraft.port.theory.TheoryCards;
import thaumcraft.port.theory.TheorySession;

/** Client-only copies retained while TC6's server acknowledgment clears the card choices. */
public final class TheoryPresentation {
    private final Map<String,Integer> totals=new TreeMap<>();
    private List<TheoryCards.Card> cards=List.of(),finishedCards=List.of();
    private TheoryCards.Card lastDraw;
    private int selected=-1,nextCategoryCheck;
    private boolean initialized;

    public record Changes(boolean drawn,boolean selected){}

    public Changes sync(TheorySession session,int tick){
        if(session==null){
            totals.clear();cards=List.of();finishedCards=List.of();lastDraw=null;selected=-1;initialized=false;
            return new Changes(false,false);
        }
        if(!initialized){
            totals.putAll(session.totals);lastDraw=session.lastDraw;nextCategoryCheck=tick+1;initialized=true;
        }
        boolean drawn=false,selection=false;
        if(selected<0){
            // Another viewer may acknowledge or draw before this client observes the selected snapshot.
            if(session.choices.isEmpty())cards=List.of();
            else if(!sameDraw(session.choices,finishedCards)){
                drawn=!sameDraw(cards,session.choices);cards=List.copyOf(session.choices);
            }
        }
        if(!cards.isEmpty()&&selected<0&&session.selected>=0&&session.selected<session.choices.size()){
            TheoryCards.Card accepted=session.choices.get(session.selected);
            for(int i=0;i<cards.size();i++)if(cards.get(i).seed()==accepted.seed()){
                var refreshed=new ArrayList<>(cards);refreshed.set(i,accepted);cards=List.copyOf(refreshed);
                selected=i;selection=true;break;
            }
        }
        if(cards.isEmpty()&&session.selected<0){lastDraw=session.lastDraw;finishedCards=List.of();}
        return new Changes(drawn,selection);
    }

    public Set<String> stepTotals(TheorySession session,int tick){
        if(!initialized||tick<=nextCategoryCheck)return Set.of();
        nextCategoryCheck=tick+1;
        Set<String> rising=new HashSet<>();
        for(String category:TheorySession.CATEGORIES){
            int current=totals.getOrDefault(category,0),target=session.totals.getOrDefault(category,0);
            if(current<target)rising.add(category);
            current+=Integer.compare(target,current);
            if(current==0)totals.remove(category);else totals.put(category,current);
        }
        return rising;
    }

    public void finishSelection(){
        if(selected<0)return;
        lastDraw=cards.get(selected);finishedCards=cards;cards=List.of();selected=-1;
    }

    /** A server snapshot can change between the last rendered frame and a mouse event. */
    public boolean canSelect(TheorySession session,int index){
        return selected<0&&session.selected<0&&index>=0&&index<cards.size()&&index<session.choices.size()
                &&cards.get(index).seed()==session.choices.get(index).seed()
                &&cards.get(index).kind().equals(session.choices.get(index).kind());
    }

    private static boolean sameDraw(List<TheoryCards.Card> a,List<TheoryCards.Card> b){
        if(a.size()!=b.size())return false;
        for(int i=0;i<a.size();i++)if(a.get(i).seed()!=b.get(i).seed()||!a.get(i).kind().equals(b.get(i).kind()))return false;
        return true;
    }

    public Map<String,Integer> totals(){return Collections.unmodifiableMap(totals);}
    public List<TheoryCards.Card> cards(){return cards;}
    public TheoryCards.Card lastDraw(){return lastDraw;}
    public int selected(){return selected;}
}
