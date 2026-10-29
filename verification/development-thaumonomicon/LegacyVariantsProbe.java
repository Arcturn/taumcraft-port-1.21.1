import java.lang.reflect.*;import java.util.*;
class LegacyVariantsProbe {
 public static void main(String[] args)throws Exception {
  Method seed=Class.forName("rk").getMethod("c",int.class,int.class,int.class);
  Class<?> model=Class.forName("cfy"), builder=Class.forName("cgg$a");
  Object b=builder.getConstructor().newInstance();
  Method add=builder.getMethod("a",model,int.class);
  for(int i=0;i<16;i++){final int index=i;Object m=Proxy.newProxyInstance(model.getClassLoader(),new Class<?>[]{model},(proxy,method,a)->method.getName().equals("toString")?"variant"+index:method.getReturnType()==List.class?List.of(index):null);add.invoke(b,m,1);}
  Object weighted=builder.getMethod("a").invoke(b);Method choose=weighted.getClass().getDeclaredMethod("a",long.class);choose.setAccessible(true);
  int[][] fixtures={{0,0,0},{0,0,2},{0,1,0},{0,1,2},{2,0,0},{2,0,2},{2,1,0},{2,1,2},{1,2,1},{-1,0,-1},{-2147483648,7,2147483647},{1000000,-64,-1000000}};
  for(int[] p:fixtures){long s=(long)seed.invoke(null,p[0],p[1],p[2]);System.out.println(Arrays.toString(p)+" seed="+s+" selected="+choose.invoke(weighted,s));}
 }
}
