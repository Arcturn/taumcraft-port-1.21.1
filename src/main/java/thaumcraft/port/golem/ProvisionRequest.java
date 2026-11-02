package thaumcraft.port.golem;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import thaumcraft.port.golem.seal.ISealEntity;
import thaumcraft.port.golem.task.Task;

/** Original ten-second request / two-minute assigned-task deadlines. */
public final class ProvisionRequest {
    private final ISealEntity seal;
    private final Entity entity;
    private BlockPos pos;
    private Direction side;
    private final ItemStack stack;
    private int id,ui;
    private Task linkedTask;
    private boolean invalid;
    private long timeout=System.currentTimeMillis()+10000;
    public ProvisionRequest(ISealEntity seal,ItemStack stack){this.seal=seal;entity=null;this.stack=stack.copy();id=Objects.hash(seal.getSealPos(),ItemStack.hashItemAndComponents(stack),stack.getCount());}
    public ProvisionRequest(BlockPos pos,Direction side,ItemStack stack){seal=null;entity=null;this.pos=pos.immutable();this.side=side;this.stack=stack.copy();id=Objects.hash(pos,side,ItemStack.hashItemAndComponents(stack),stack.getCount());}
    public ProvisionRequest(Entity entity,ItemStack stack){seal=null;this.entity=entity;this.stack=stack.copy();id=Objects.hash(entity.getUUID(),ItemStack.hashItemAndComponents(stack),stack.getCount());}
    public long getTimeout(){return timeout;}
    public int getId(){return id;}
    public void setId(int id){this.id=id;}
    public void setUI(int ui){this.ui=ui;}
    public int getUI(){return ui;}
    public ISealEntity getSeal(){return seal;}
    public Entity getEntity(){return entity;}
    public ItemStack getStack(){return stack;}
    public BlockPos getPos(){return pos;}
    public void setPos(BlockPos pos){this.pos=pos==null?null:pos.immutable();}
    public Direction getSide(){return side;}
    public void setSide(Direction side){this.side=side;}
    public Task getLinkedTask(){return linkedTask;}
    public void setLinkedTask(Task task){linkedTask=task;timeout=System.currentTimeMillis()+120000;}
    public boolean isInvalid(){return invalid;}
    public void setInvalid(boolean invalid){this.invalid=invalid;}
    @Override public boolean equals(Object other){return other instanceof ProvisionRequest request&&id==request.id&&ui==request.ui;}
    @Override public int hashCode(){return Objects.hash(id,ui);}
}
