package thaumcraft.port.auromancy.focal;

import java.util.HashSet;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import thaumcraft.port.auromancy.focus.FocusEngine;
import thaumcraft.port.auromancy.focus.FocusNode;
import thaumcraft.port.auromancy.focus.FocusPackage;

/** The original editable node NBT, separate from the compiled focus package. */
public final class FocusElementNode {
    public int x,y,id,parent=-1;
    public boolean target,trajectory;
    public int[] children=new int[0];
    public float complexityMultiplier=1;
    public FocusNode node;

    public float getPower(Map<Integer,FocusElementNode> data) {
        if(node==null)return 1;
        float power=1;var visited=new HashSet<Integer>();FocusElementNode current=this;
        while(current!=null&&current.node!=null&&visited.add(current.id)&&visited.size()<=FocusPackage.MAX_NODES) {
            power*=current.node.getPowerMultiplier();current=data.get(current.parent);
        }
        return power;
    }
    public void deserialize(CompoundTag tag) {
        x=tag.getInt("x");y=tag.getInt("y");id=tag.getInt("id");parent=tag.getInt("parent");
        target=tag.getBoolean("target");trajectory=tag.getBoolean("trajectory");children=tag.getIntArray("children").clone();
        complexityMultiplier=tag.contains("complexity",Tag.TAG_ANY_NUMERIC)?tag.getFloat("complexity"):1;
        node=null;String key=tag.getString("key");
        if(!key.isEmpty()) {
            if(!(FocusEngine.getElement(key) instanceof FocusNode decoded))throw new IllegalArgumentException("Unknown focus element "+key);
            node=decoded;
            for(String setting:node.getSettingList())if(!tag.contains("setting."+setting,Tag.TAG_INT)||!node.getSetting(setting).setValueChecked(tag.getInt("setting."+setting)))
                throw new IllegalArgumentException("Invalid focus setting "+key+"/"+setting);
        }
    }
    public CompoundTag serialize() {
        var tag=new CompoundTag();tag.putInt("x",x);tag.putInt("y",y);tag.putInt("id",id);tag.putInt("parent",parent);
        tag.putBoolean("target",target);tag.putBoolean("trajectory",trajectory);tag.putIntArray("children",children.clone());tag.putFloat("complexity",complexityMultiplier);
        if(node!=null){tag.putString("key",node.getKey());for(String setting:node.getSettingList())tag.putInt("setting."+setting,node.getSettingValue(setting));}
        return tag;
    }
}
