package thaumcraft.port.research;

/** Advancement conditions read completed research; they never grant research or spend knowledge. */
public final class ResearchMilestones {
    public static boolean matches(ResearchState state,String research,String category){
        if(research.isEmpty()==category.isEmpty())return false;
        if(!category.isEmpty())return ResearchCatalog.CATEGORIES.contains(category)&&ResearchCatalog.all().stream()
                .anyMatch(entry->entry.category().equals(category)&&state.knows(entry.key()));
        if(research.equals("!gotthaumonomicon"))return state.flags().contains(research);
        return ResearchCatalog.get(research)!=null&&state.knows(research);
    }
    private ResearchMilestones(){}
}
