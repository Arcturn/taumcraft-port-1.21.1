package thaumcraft.port.essentia;

import net.minecraft.core.Direction;
import thaumcraft.port.aspect.Aspect;

/** Sided TC6 suction transport. Simulation must not mutate; calls run on the server thread. */
public interface EssentiaTransport {
    boolean connects(Direction side);
    boolean input(Direction side);
    boolean output(Direction side);
    Aspect aspect(Direction side);
    int amount(Direction side);
    Aspect suctionType(Direction side);
    int suction(Direction side);
    int minimumSuction();
    int insert(Aspect aspect,int amount,Direction side,boolean simulate);
    int extract(Aspect aspect,int amount,Direction side,boolean simulate);

    static int pull(EssentiaTransport sink,Direction face,EssentiaTransport source){
        Direction other=face.getOpposite();
        if(source==null||!sink.connects(face)||!source.connects(other)||!sink.input(face)||!source.output(other)
                ||sink.suction(face)<=source.suction(other)||sink.suction(face)<source.minimumSuction())return 0;
        Aspect a=sink.suctionType(face)!=null?sink.suctionType(face):source.aspect(other);
        if(a==null||(sink.suctionType(face)!=null&&sink.suctionType(face)!=a)
                ||sink.insert(a,1,face,true)!=1||source.extract(a,1,other,true)!=1)return 0;
        // Both stores have been checked, on the same thread, before either changes.
        int taken=source.extract(a,1,other,false);
        if(taken!=1)return 0;
        int accepted=sink.insert(a,1,face,false);
        if(accepted!=1)throw new IllegalStateException("Essentia capability changed after simulation");
        return accepted;
    }
}
