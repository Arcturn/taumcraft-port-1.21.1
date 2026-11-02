package thaumcraft.port.golem.seal;
public interface ISealConfigToggles {
    SealToggle[] getToggles();
    default void setToggle(int index, boolean value) { if(index>=0&&index<getToggles().length)getToggles()[index].setValue(value); }
    final class SealToggle {
        public boolean value;
        public final String key, name;
        public SealToggle(boolean value,String key,String name){this.value=value;this.key=key;this.name=name;}
        public boolean getValue(){return value;}
        public void setValue(boolean value){this.value=value;}
        public String getKey(){return key;}
        public String getName(){return name;}
    }
}
