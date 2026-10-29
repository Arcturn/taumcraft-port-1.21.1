package thaumcraft.port.world;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
public final class MagicTreeFeature extends Feature<NoneFeatureConfiguration>{
    private final boolean silver;
    public MagicTreeFeature(boolean silver){super(NoneFeatureConfiguration.CODEC);this.silver=silver;}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c){return silver?MagicTrees.silverwood(c.level(),c.random(),c.origin(),false,false):MagicTrees.greatwood(c.level(),c.random(),c.origin(),false);}
}
