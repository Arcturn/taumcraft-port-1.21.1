package thaumcraft.port.auromancy.focus;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** The original integer/list spinner contract, shared by the editor and server validator. */
public final class NodeSetting {
    private int value;
    public final String key;
    private final String description;
    private final INodeSettingType type;
    private final String research;

    public NodeSetting(String key, String description, INodeSettingType type, String research) {
        this.key = key; this.description = description; this.type = type; this.research = research;
        value = type.getDefault();
    }
    public NodeSetting(String key, String description, INodeSettingType type) { this(key, description, type, null); }
    public int getValue() { return type.getValue(value); }
    public String getResearch() { return research; }
    public String getValueText() { return Component.translatable(type.getValueText(value)).getString(); }
    public String getLocalizedName() { return Component.translatable(description).getString(); }
    public String getDescriptionKey() { return description; }
    public INodeSettingType getType() { return type; }
    public void increment() { if (value < Integer.MAX_VALUE) value = type.clamp(value + 1); }
    public void decrement() { if (value > Integer.MIN_VALUE) value = type.clamp(value - 1); }
    public void setValue(int actualValue) {
        value = type.clamp(Integer.MIN_VALUE);
        int last = Integer.MIN_VALUE;
        while (getValue() != actualValue && last != value) { last = value; increment(); }
    }
    /** Packet values must be members of the original setting, not silently clamped. */
    public boolean setValueChecked(int actualValue) {
        if (!isValidValue(actualValue)) return false;
        setValue(actualValue); return true;
    }
    public boolean isValidValue(int actualValue) { return validValues().contains(actualValue); }
    public List<Integer> validValues() {
        int minimum = type.clamp(Integer.MIN_VALUE), maximum = type.clamp(Integer.MAX_VALUE);
        if ((long) maximum - minimum > 4096) throw new IllegalStateException("Unbounded focus setting " + key);
        var values = new ArrayList<Integer>();
        for (long i = minimum; i <= maximum; i++) values.add(type.getValue((int) i));
        return List.copyOf(values);
    }
    public static final class NodeSettingIntRange implements INodeSettingType {
        public final int min, max;
        public NodeSettingIntRange(int min, int max) {
            if (min > max || (long) max - min > 4096) throw new IllegalArgumentException("Invalid focus setting range");
            this.min = min; this.max = max;
        }
        public int getDefault() { return min; }
        public int clamp(int value) { return Mth.clamp(value, min, max); }
        public int getValue(int value) { return clamp(value); }
        public String getValueText(int value) { return Integer.toString(getValue(value)); }
    }
    public static final class NodeSettingIntList implements INodeSettingType {
        private final int[] values;
        private final String[] descriptions;
        public NodeSettingIntList(int[] values, String[] descriptions) {
            if (values.length == 0 || values.length != descriptions.length || values.length > 4096)
                throw new IllegalArgumentException("Invalid focus setting choices");
            this.values = values.clone(); this.descriptions = descriptions.clone();
        }
        public int getDefault() { return 0; }
        public int clamp(int value) { return Mth.clamp(value, 0, values.length - 1); }
        public int getValue(int value) { return values[clamp(value)]; }
        public String getValueText(int value) { return descriptions[clamp(value)]; }
    }
    public interface INodeSettingType {
        int getDefault();
        int clamp(int value);
        int getValue(int value);
        String getValueText(int value);
    }
}
