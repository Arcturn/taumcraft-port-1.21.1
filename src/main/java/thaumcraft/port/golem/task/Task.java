package thaumcraft.port.golem.task;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import thaumcraft.port.golem.IGolemAPI;
import thaumcraft.port.golem.ProvisionRequest;
import thaumcraft.port.golem.seal.SealHandler;
import thaumcraft.port.golem.seal.SealPos;

/** Transient BETA26 task ticket; seals regenerate tickets after a world reload. */
public final class Task {
    private static final AtomicInteger NEXT_ID = new AtomicInteger();
    private UUID golemUUID;
    private final int id = NEXT_ID.incrementAndGet();
    private final byte type;
    private final SealPos sealPos;
    private final BlockPos pos;
    private final Entity entity;
    private boolean reserved, suspended, completed;
    private int data;
    private ProvisionRequest linkedProvision;
    private short lifespan = 300;
    private byte priority;

    public Task(SealPos sealPos, BlockPos pos) {
        this.sealPos=sealPos;this.pos=pos.immutable();entity=null;type=0;
    }
    public Task(SealPos sealPos, Entity entity) {
        this.sealPos=sealPos;this.entity=java.util.Objects.requireNonNull(entity);pos=null;type=1;
    }
    public byte getPriority(){return priority;}
    public void setPriority(byte priority){this.priority=priority;}
    public boolean isCompleted(){return completed;}
    public void setCompletion(boolean value){completed=value;lifespan=(short)Math.min(Short.MAX_VALUE,lifespan+1);}
    public UUID getGolemUUID(){return golemUUID;}
    public void setGolemUUID(UUID uuid){golemUUID=uuid;}
    public BlockPos getPos(){return type==1?entity.blockPosition():pos;}
    public byte getType(){return type;}
    public Entity getEntity(){return entity;}
    public int getId(){return id;}
    public boolean isReserved(){return reserved;}
    public void setReserved(boolean value){reserved=value;lifespan=(short)Math.min(Short.MAX_VALUE,lifespan+120);}
    public boolean isSuspended(){return suspended;}
    public void setSuspended(boolean value){linkedProvision=null;suspended=value;}
    public SealPos getSealPos(){return sealPos;}
    public long getLifespan(){return lifespan;}
    public void setLifespan(short lifespan){this.lifespan=lifespan;}
    public int getData(){return data;}
    public void setData(int data){this.data=data;}
    public ProvisionRequest getLinkedProvision(){return linkedProvision;}
    public void setLinkedProvision(ProvisionRequest request){linkedProvision=request;}
    public boolean canGolemPerformTask(IGolemAPI golem){
        var seal=sealPos==null?null:SealHandler.getSealEntity(golem.getGolemWorld(),sealPos);
        if(seal==null)return true;
        if(golem.getGolemColor()>0&&seal.getColor()>0&&golem.getGolemColor()!=seal.getColor())return false;
        return seal.getSeal().canGolemPerformTask(golem,this);
    }
    @Override public boolean equals(Object other){return other instanceof Task task&&task.id==id;}
    @Override public int hashCode(){return id;}
}
