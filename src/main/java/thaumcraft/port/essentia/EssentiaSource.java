package thaumcraft.port.essentia;

import thaumcraft.port.aspect.Aspect;

/** Remote source contract for infusion/devices. Separate from pressure-driven pipes. */
public interface EssentiaSource {
    boolean blocked();
    Aspect storedAspect();
    int storedAmount();
    int takeRemote(Aspect aspect,int amount,boolean simulate);
    int addRemote(Aspect aspect,int amount,boolean simulate);
}
