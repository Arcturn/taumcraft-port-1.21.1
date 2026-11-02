package thaumcraft.port.auromancy.client.media;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;

/** The position math of CoreGLE's five-sided, capped ANGLE join (style 1042).
 * The first and last path points are guides, not rendered end rings. */
final class FocusBoltGeometry {
    record Node(Vec3 position,double radius) { }
    record Segment(Vec3[] front,Vec3[] back) { }

    static List<Segment> tube(List<Node> points,double scale) {
        var result=new ArrayList<Segment>();
        if(points.size()<4)return result;
        int guide=next(points,0);
        Vec3 direction=points.get(guide).position.subtract(points.get(0).position);
        Vec3 up=direction.x==0&&direction.z==0?new Vec3(1,1,1):new Vec3(0,1,0);
        Vec3 unit=unit(direction);
        up=up.subtract(unit.scale(up.dot(unit)));
        if(up.lengthSqr()==0)up=unit;
        int start=1,end=next(points,start);
        Vec3 frontNormal=bisector(points.get(0).position,points.get(start).position,points.get(end).position);
        up=reflect(up,frontNormal);
        while(end<points.size()-1) {
            int after=next(points,end);
            var a=points.get(start);var b=points.get(end);
            Vec3 backNormal=bisector(a.position,b.position,points.get(after).position);
            Vec3 axis=unit(b.position.subtract(a.position));
            Vec3 y=unit(up.subtract(axis.scale(up.dot(axis))));
            Vec3 x=axis.cross(y);
            var front=new Vec3[5];var back=new Vec3[5];
            double cosine=1,sine=0,stepCos=Math.cos(Math.PI*2/5),stepSin=Math.sin(Math.PI*2/5);
            for(int side=0;side<5;side++) {
                Vec3 radial=x.scale(cosine).add(y.scale(sine));
                front[side]=miter(a.position,radial.scale(a.radius*scale),axis,frontNormal);
                back[side]=miter(b.position,radial.scale(b.radius*scale),axis,backNormal);
                double nextCos=cosine*stepCos-sine*stepSin;
                sine=sine*stepCos+cosine*stepSin;cosine=nextCos;
            }
            result.add(new Segment(front,back));
            start=end;end=after;frontNormal=backNormal;up=reflect(up,frontNormal);
        }
        return result;
    }

    private static int next(List<Node> points,int index) {
        // FIND_NON_DEGENERATE_POINT increments before testing the tolerance.
        // Its decompiled do/while moves that increment into the condition;
        // the BETA26 bytecode is authoritative for this detail.
        int next=index;
        double length,tolerance;
        do {
            var a=points.get(next).position;var b=points.get(next+1).position;
            length=b.subtract(a).length();tolerance=b.add(a).length()*2e-6;
            next++;
        }while(length<=tolerance&&next<points.size()-1);
        return next;
    }
    private static Vec3 unit(Vec3 value) {double length=value.length();return length==0?Vec3.ZERO:value.scale(1/length);}
    private static Vec3 reflect(Vec3 value,Vec3 normal) {return value.subtract(normal.scale(2*value.dot(normal)));}
    private static Vec3 bisector(Vec3 previous,Vec3 current,Vec3 next) {
        Vec3 before=current.subtract(previous),after=next.subtract(current);
        double beforeLength=before.length(),afterLength=after.length();
        if(beforeLength<=2e-6*afterLength)return unit(after);
        if(afterLength<=2e-6*beforeLength)return unit(before);
        before=unit(before);after=unit(after);double dot=before.dot(after);
        return dot>=.999998||dot<=-.999998?before:unit(before.add(after).scale(dot-1));
    }
    private static Vec3 miter(Vec3 center,Vec3 radial,Vec3 axis,Vec3 normal) {
        double denominator=axis.dot(normal);
        return denominator==0?center:center.add(radial).subtract(axis.scale(radial.dot(normal)/denominator));
    }
    private FocusBoltGeometry() { }
}
