package thaumcraft.port.research;
import java.util.*;
/** Presentation policy from TC6; does not grant server research. */
public final class ResearchView {
    public static String clean(String key){return key.replaceFirst("^~", "").split("@",2)[0];}
    public static int addenda(ResearchState s,ResearchCatalog.Entry e){return s.knows(e.key())?(int)e.addenda().stream().filter(a->ResearchCatalog.strings(a,"required_research").stream().allMatch(s::knows)).count():0;}
    public static boolean newResearch(ResearchState s,String key){return s.hasResearchFlag(key,ResearchState.ResearchFlag.RESEARCH);}
    public static boolean newPage(ResearchState s,String key){return s.hasResearchFlag(key,ResearchState.ResearchFlag.PAGE);}
    public static boolean unread(ResearchState s,String key){return newResearch(s,key)||newPage(s,key);}
    public static boolean categoryFlag(ResearchState s,String category,ResearchState.ResearchFlag flag){return ResearchCatalog.all().stream().anyMatch(e->e.category().equals(category)&&s.isResearchKnown(e.key())&&s.hasResearchFlag(e.key(),flag));}
    public static List<String> searchRecipes(ResearchState s,ResearchCatalog.Entry e){return e.stages().isEmpty()?List.of():ResearchCatalog.strings(e.stages().get(Math.min(e.stages().size()-1,s.stage(e.key())+1)),"recipes");}
    public static boolean category(ResearchState state,String key){return key.equals("BASICS")||state.knows("UNLOCK"+key);}
    public static boolean visible(ResearchState state,ResearchCatalog.Entry entry){return visible(state,entry,new HashSet<>());}
    private static boolean visible(ResearchState state,ResearchCatalog.Entry e,Set<String> visited){
        if(e==null||state.stage(e.key())>0||state.knows(e.key())||!visited.add(e.key()))return true;
        if(e.meta().contains("HIDDEN")&&(!state.available(e)||e.parents().isEmpty()))return false;
        return e.parents().stream().map(ResearchView::clean).map(ResearchCatalog::get).allMatch(p->visible(state,p,visited));
    }
    public static int completion(ResearchState state,String category){
        var es=ResearchCatalog.all().stream().filter(e->e.category().equals(category)&&!e.meta().contains("AUTOUNLOCK")).toList();
        return es.isEmpty()?0:(int)(es.stream().filter(e->state.isResearchKnown(e.key())).count()*100/es.size());
    }
    private ResearchView(){}
}
