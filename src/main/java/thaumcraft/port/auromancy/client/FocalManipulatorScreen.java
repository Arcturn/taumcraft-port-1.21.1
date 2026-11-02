package thaumcraft.port.auromancy.client;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import thaumcraft.port.aspect.Aspect;
import thaumcraft.port.auromancy.focal.FocusElementNode;
import thaumcraft.port.auromancy.focal.FocalManipulatorEntity;
import thaumcraft.port.auromancy.focal.FocalManipulatorMenu;
import thaumcraft.port.auromancy.focus.*;
import thaumcraft.port.client.BookAssets;
import thaumcraft.port.client.ClientJournal;
import thaumcraft.port.content.CrystalItem;
import thaumcraft.port.content.PortSounds;

/** BETA26's focus graph editor, using its original coordinates and texture sheets. */
public final class FocalManipulatorScreen extends AbstractContainerScreen<FocalManipulatorMenu> {
    private static final ResourceLocation FRAME=BookAssets.id("textures/gui/gui_wandtable.png"),
            BACK=BookAssets.id("textures/gui/gui_wandtable2.png"), INVENTORY=BookAssets.id("textures/gui/gui_wandtable3.png"),
            BASE=BookAssets.id("textures/gui/gui_base.png"), MEDIUM=BookAssets.id("textures/foci/_medium.png"), EFFECT=BookAssets.id("textures/foci/_effect.png");
    private static final DecimalFormat NUMBER=new DecimalFormat("#######.##");
    private final FocalManipulatorEntity table;
    private final List<String> parts=new ArrayList<>();
    private final List<Spinner> settings=new ArrayList<>();
    private final Map<Aspect,Integer> crystals=new LinkedHashMap<>();
    private EditBox name;
    private int selected=-1, hovered=-1, nextId, partsStart, scrollX, scrollY, minX, maxX, maxY;
    private int complexity, maximum, costXp, costVis, drag;
    private long revision=Long.MIN_VALUE;
    private boolean incomplete, valid, updatingName;
    private ItemStack lastFocus=ItemStack.EMPTY;

    public FocalManipulatorScreen(FocalManipulatorMenu menu, Inventory inventory, Component title) {
        super(menu,inventory,title);table=menu.entity;imageWidth=231;imageHeight=231;
    }
    @Override protected void init() {
        super.init();
        menu.sendStart=()->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0);
        name=new EditBox(font,leftPos+30,topPos+11,170,12,Component.translatable("item.Focus.name"));
        name.setBordered(false);name.setTextColor(0xffffff);name.setTextColorUneditable(0xffffff);name.setMaxLength(50);
        name.setResponder(value->{if(!updatingName&&table.vis<=0){table.focusName=value;menu.sendName(value);}});
        addRenderableWidget(name);
        refreshFromTable();
    }
    private Map<Integer,FocusElementNode> data(){return table.data;}
    private void refreshFromTable() {
        revision=table.revision;
        if(!data().containsKey(selected)) {
            selected=-1;
            for(var node:data().values())if(node.parent>=0&&(selected<0||node.node==null))selected=node.id;
        }
        lastFocus=table.getItem(0).copy();
        if(table.focusName.isEmpty()&&!lastFocus.isEmpty())table.focusName=lastFocus.getHoverName().getString();
        if(!name.isFocused())setName(table.focusName);
        rebuild();
    }
    private void setName(String value){updatingName=true;name.setValue(value);updatingName=false;}
    @Override protected void containerTick() {
        super.containerTick();
        if(table.getItem(0).isEmpty()) {
            if(!data().isEmpty()||!lastFocus.isEmpty()){data().clear();selected=-1;table.focusName="";setName("");refreshFromTable();}
            table.doGuiReset=false;table.doGather=false;
        } else if(table.doGuiReset) {
            table.doGuiReset=false;
            table.focusName=table.getItem(0).getHoverName().getString();setName(table.focusName);
            resetNodes();refreshFromTable();
        } else if(table.doGather||revision!=table.revision||!ItemStack.isSameItemSameComponents(lastFocus,table.getItem(0))) {
            table.doGather=false;refreshFromTable();
        }
        name.visible=!data().isEmpty();name.setEditable(table.vis<=0);
        checkResources();
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        checkResources();
        super.render(g,mouseX,mouseY,partial);
        for(int i=0;i<6&&partsStart+i<parts.size();i++) {
            var element=FocusEngine.getElement(parts.get(partsStart+i));
            if(element instanceof FocusNode node)drawPart(g,node,leftPos+38,topPos+43+i*25,node.getType()==IFocusElement.EnumUnitType.MOD?24:32,over(mouseX,mouseY,leftPos+28,topPos+33+i*25,20,20));
        }
        renderTooltip(g,mouseX,mouseY);
        renderHints(g,mouseX,mouseY);
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y) { }
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY) {
        g.setColor(1,1,1,1);
        sprite(g,BACK,leftPos,topPos,0,0,231,231);
        sprite(g,INVENTORY,leftPos-71,topPos-3,0,0,71,239);
        drawGraph(g,mouseX,mouseY);
        sprite(g,FRAME,leftPos,topPos,0,0,231,231);
        drawScrollbars(g);
        if(!data().isEmpty()) {
            sprite(g,BASE,leftPos+24,topPos+8,192,224,8,14);
            for(int i=1;i<22;i++)sprite(g,BASE,leftPos+24+i*8,topPos+8,200,224,8,14);
            sprite(g,BASE,leftPos+200,topPos+8,208,224,8,14);
        }
        float tint=valid&&table.vis<=0?(over(mouseX,mouseY,leftPos+230,topPos+10,24,16)?1:.9f):.5f;
        g.setColor(tint,tint,tint,.9f);sprite(g,FRAME,leftPos+230,topPos+10,232,240,24,16);g.setColor(1,1,1,1);
        for(int i=0;i<3;i++)BookAssets.texture(g,BookAssets.id("textures/gui/"+new String[]{"complex","costxp","costvis"}[i]+".png"),leftPos+225,topPos+31+i*14,16);
        if(maximum>0)g.drawString(font,complexity+"/"+maximum,leftPos+242,topPos+36,complexity>maximum?16151160:16761407);
        g.drawString(font,Integer.toString(costXp),leftPos+242,topPos+50,!minecraft.player.getAbilities().instabuild&&costXp>minecraft.player.experienceLevel?16151160:10092429);
        g.drawString(font,Integer.toString(table.vis>0?(int)table.vis:costVis),leftPos+242,topPos+64,0x55ffff);
        if(complexity>0)g.drawString(font,Component.translatable("item.Focus.cost1").append(": "+NUMBER.format(complexity/5f)),leftPos+230,topPos+80,0x55ffff);
        if(!crystals.isEmpty())g.drawString(font,Component.translatable("wandtable.text4"),leftPos+230,topPos+92,0xffaa00);
        int index=0;
        for(var crystal:crystals.entrySet()) {
            ItemStack stack=CrystalItem.stack(crystal.getKey());stack.setCount(crystal.getValue());
            int x=leftPos+226+(index%5)*16,y=topPos+102+(index/5)*16;
            g.renderItem(stack,x,y);g.renderItemDecorations(font,stack,x,y);index++;
        }
        for(var spinner:settings) {
            int x=leftPos+231,y=topPos+spinner.y;
            g.drawString(font,spinner.setting.getLocalizedName(),x,y-13,0xffaa00);
            g.setColor(table.vis>0?.5f:1,table.vis>0?.5f:1,table.vis>0?.5f:1,1);
            sprite(g,BASE,x,y,20,0,10,10);sprite(g,BASE,x+spinner.width,y,30,0,10,10);g.setColor(1,1,1,1);
            g.drawCenteredString(font,spinner.setting.getValueText(),x+spinner.width/2+5,y+1,0xffffff);
        }
    }
    private void drawGraph(GuiGraphics g,int mouseX,int mouseY) {
        int previous=hovered;hovered=-1;
        g.enableScissor(leftPos+63,topPos+31,leftPos+199,topPos+191);
        for(var node:data().values()) {
            int x=leftPos+132-scrollX+node.x*24,y=topPos+48-scrollY+node.y*32;
            boolean hover=inGraph(mouseX,mouseY)&&over(mouseX,mouseY,x-10,y-10,20,20);
            if(hover&&node.parent>=0)hovered=node.id;
            if(node.node!=null)drawPart(g,node.node,x,y,32,hover);
            else sprite(g,FRAME,x-12,y-12,120,232,24,24);
            if(selected==node.id||(hover&&node.parent>=0))sprite(g,FRAME,x-12,y-12,96,232,24,24);
            var parent=data().get(node.parent);
            if(parent==null)continue;
            sprite(g,FRAME,x-6,y-22,54,232,12,12);
            if(parent.node instanceof FocusModSplit)for(int i=0;i<Math.abs(node.x-parent.x);i++) {
                if(node.x<parent.x) {
                    if(i==0)sprite(g,FRAME,x-4,y-36,8,240,16,16);
                    else sprite(g,FRAME,x-12+i*24,y-36,72,240,24,16);
                } else {
                    if(i==0)sprite(g,FRAME,x-12,y-36,24,240,16,16);
                    else sprite(g,FRAME,x-12-i*24,y-36,72,240,24,16);
                }
            }
            if(node.node==null) {
                int offset=parent.target&&parent.trajectory?4:0;
                if(parent.target)scaledSprite(g,x-offset-4,y-4,152,240);
                if(parent.trajectory)scaledSprite(g,x+offset-4,y-4,168,240);
            }
        }
        g.disableScissor();
        if(hovered>=0&&hovered!=previous)sound("clack",2);
    }
    private static void scaledSprite(GuiGraphics g,int x,int y,int u,int v){BookAssets.prepareImage(g);g.blit(FRAME,x,y,8,8,u,v,16,16,256,256);}
    public static void drawPart(GuiGraphics g,FocusNode node,int x,int y,float scale,boolean hover) {
        // ROOT is a medium in BETA26; its actual key is "ROOT", not the unused
        // "thaumcraft.ROOT" special case in the old GUI.
        boolean root=node.getType()==IFocusElement.EnumUnitType.MOD;
        if(root)scale*=2;
        int color=FocusEngine.getElementColor(node.getKey());
        if(node.getType()==IFocusElement.EnumUnitType.EFFECT||(node.getType()==IFocusElement.EnumUnitType.MEDIUM&&!root)) {
            float size=scale*.9f+(hover?2:0);
            g.setColor(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f,1);
            centeredTexture(g,node.getType()==IFocusElement.EnumUnitType.EFFECT?EFFECT:MEDIUM,x,y,size);
        }
        g.setColor(1,1,1,1);
        centeredTexture(g,FocusEngine.getElementIcon(node.getKey()),x,y,scale/2+(hover?2:0));
    }
    private static void centeredTexture(GuiGraphics g,ResourceLocation texture,int x,int y,float size) {
        g.pose().pushPose();g.pose().translate(x-size/2,y-size/2,0);g.pose().scale(size/16,size/16,1);
        BookAssets.texture(g,texture,0,0,16);g.pose().popPose();
    }
    private void drawScrollbars(GuiGraphics g) {
        if(parts.size()>6)scrollbar(g,leftPos+51,topPos+30,8,149,partsStart,0,parts.size()-6,true);
        if(vertical())scrollbar(g,leftPos+203,topPos+32,8,156,scrollY,0,(maxY-3)*32,true);
        if(horizontal())scrollbar(g,leftPos+64,topPos+195,132,8,scrollX,minX*24,maxX*24,false);
    }
    private void scrollbar(GuiGraphics g,int x,int y,int w,int h,int value,int min,int max,boolean vertical) {
        BookAssets.prepareImage(g);
        if(vertical)g.blit(BASE,x+2,y,4,h,240,176,4,32,256,256);
        else g.blit(BASE,x,y+2,w,4,208,176,32,4,256,256);
        int position=Math.round((value-min)/(float)Math.max(1,max-min)*((vertical?h:w)-8));
        sprite(g,BASE,x+(vertical?0:position),y+(vertical?position:0),20,20,8,8);
    }
    private void renderHints(GuiGraphics g,int mouseX,int mouseY) {
        for(int i=0;i<6&&partsStart+i<parts.size();i++)if(over(mouseX,mouseY,leftPos+28,topPos+33+i*25,20,20)) {
            if(FocusEngine.getElement(parts.get(partsStart+i)) instanceof FocusNode node)tooltip(g,partText(node,null),mouseX,mouseY);
            return;
        }
        var node=data().get(hovered);
        if(node!=null&&node.node!=null){tooltip(g,partText(node.node,node),mouseX,mouseY);return;}
        if(over(mouseX,mouseY,leftPos+230,topPos+10,24,16)){g.renderTooltip(font,Component.translatable("wandtable.text3"),mouseX,mouseY);return;}
        for(int i=0;i<3;i++)if(over(mouseX,mouseY,leftPos+225,topPos+31+i*14,32,14)){g.renderTooltip(font,Component.translatable(new String[]{"wandtable.text5","wandtable.text2","wandtable.text1"}[i]),mouseX,mouseY);return;}
        int i=0;
        for(var crystal:crystals.entrySet()) {
            if(over(mouseX,mouseY,leftPos+226+(i%5)*16,topPos+102+(i/5)*16,16,16)){
                ItemStack stack=CrystalItem.stack(crystal.getKey());stack.setCount(crystal.getValue());g.renderTooltip(font,stack,mouseX,mouseY);return;
            }
            i++;
        }
    }
    private void tooltip(GuiGraphics g,List<Component> text,int x,int y) {
        var lines=new ArrayList<net.minecraft.util.FormattedCharSequence>();
        int wrap=Math.max(120,Math.min(240,width-(leftPos+imageWidth-16)));
        for(var line:text)lines.addAll(font.split(line,wrap));
        g.renderTooltip(font,lines,x,y);
    }
    private List<Component> partText(FocusNode node,FocusElementNode placed) {
        var lines=new ArrayList<Component>();
        lines.add(Component.translatable(node.getUnlocalizedName()));
        lines.add(Component.translatable(node.getUnlocalizedText()).withStyle(ChatFormatting.DARK_PURPLE));
        int cost=(int)(node.getComplexity()*(placed==null?1:placed.complexityMultiplier));
        lines.add(Component.translatable("focuspart.com").withStyle(ChatFormatting.GOLD).append(Component.literal(" "+cost).withStyle(placed!=null&&placed.complexityMultiplier>1?ChatFormatting.RED:ChatFormatting.GOLD)));
        float power=placed==null?node.getPowerMultiplier():placed.getPower(data());
        if(power!=1)lines.add(Component.translatable("focuspart.eff").withStyle(ChatFormatting.GOLD).append(Component.literal(" x"+NUMBER.format(power)).withStyle(power<1?ChatFormatting.RED:ChatFormatting.GREEN)));
        if(node instanceof FocusEffect effect) {
            float damage=effect.getDamageForDisplay(placed==null?1:placed.getPower(data()));
            if(damage!=0)lines.add(Component.translatable("attribute.modifier.equals.0",NUMBER.format(Math.abs(damage)),Component.translatable(damage>0?"attribute.name.generic.attack_damage":"focus.heal.power")).withStyle(damage>0?ChatFormatting.DARK_RED:ChatFormatting.DARK_GREEN));
        }
        return lines;
    }
    private void rebuild() {
        complexity=0;maximum=table.getMaxComplexity();crystals.clear();incomplete=data().isEmpty();
        var count=new HashMap<String,Integer>();
        minX=maxX=maxY=0;
        for(var node:data().values()) {
            minX=Math.min(minX,node.x);maxX=Math.max(maxX,node.x);maxY=Math.max(maxY,node.y);
            if(node.node==null){incomplete=true;continue;}
            node.complexityMultiplier=.5f*(count.merge(node.node.getKey(),1,Integer::sum)+1);
            complexity=(int)(complexity+node.node.getComplexity()*node.complexityMultiplier);
            if(node.node.getAspect()!=null)crystals.merge(node.node.getAspect(),1,Integer::sum);
        }
        costVis=complexity*10+maximum/5;
        // The original GUI used min(1, ...); the server charges max(1, ...).
        costXp=data().isEmpty()?0:Math.max(1,(int)Math.round(Math.sqrt(complexity)));
        settings.clear();
        var selectedNode=data().get(selected);
        if(selectedNode!=null&&selectedNode.node!=null) {
            int i=0,total=selectedNode.node.getSettingList().size();
            for(String key:selectedNode.node.getSettingList()) {
                var setting=selectedNode.node.getSetting(key);
                if(setting.getResearch()!=null&&!ClientJournal.state.knows(setting.getResearch()))continue;
                settings.add(new Spinner(setting,234-total*26+i++*26,setting.getType() instanceof NodeSetting.NodeSettingIntList?72:32));
            }
        }
        gatherParts();clampScroll();checkResources();
    }
    private void checkResources() {
        boolean available=!crystals.isEmpty();
        if(minecraft==null||minecraft.player==null){valid=false;return;}
        for(var crystal:crystals.entrySet()) {
            int count=0;
            for(var stack:minecraft.player.getInventory().items)if(stack.getItem() instanceof CrystalItem&&CrystalItem.aspect(stack)==crystal.getKey())count+=stack.getCount();
            if(count<crystal.getValue())available=false;
        }
        valid=maximum>0&&complexity<=maximum&&!incomplete&&available&&(minecraft.player.getAbilities().instabuild||costXp<=minecraft.player.experienceLevel);
    }
    private void gatherParts() {
        parts.clear();partsStart=0;
        var selectedNode=data().get(selected);
        var parent=selectedNode==null?null:data().get(selectedNode.parent);
        if(parent==null||parent.node==null)return;
        boolean exclusiveMedium=false,hasMedium=false;
        var excluded=new HashSet<String>();
        for(var node:data().values())if(node.node!=null) {
            if(node.node instanceof FocusMedium&&!(node.node instanceof FocusMediumRoot)){hasMedium=true;if(node.node.isExclusive())exclusiveMedium=true;}
            if(node.node.isExclusive())excluded.add(node.node.getKey());
        }
        var media=new ArrayList<String>();var effects=new ArrayList<String>();var mods=new ArrayList<String>();
        for(String key:FocusEngine.elements.keySet()) {
            var element=FocusEngine.getElement(key);
            if(!(element instanceof FocusNode node)||node instanceof FocusMediumRoot||excluded.contains(key)||node.mustBeSupplied()==null||!ClientJournal.state.knows(node.getResearch()))continue;
            for(var supply:node.mustBeSupplied())if(parent.node.canSupply(supply)) {
                switch(node.getType()) {
                    case MEDIUM->{if(!exclusiveMedium&&(!node.isExclusive()||!hasMedium))media.add(key);}
                    case EFFECT->effects.add(key);
                    case MOD->mods.add(key);
                    default->{}
                }
                break;
            }
        }
        Collections.sort(media);Collections.sort(effects);Collections.sort(mods);parts.addAll(media);parts.addAll(effects);parts.addAll(mods);
    }
    private void resetNodes() {
        data().clear();nextId=0;selected=-1;scrollX=scrollY=0;
        if(!table.getItem(0).isEmpty())replaceNode(FocusMediumRoot.class,-1);
        else rebuild();
    }
    private int nextId(){while(data().containsKey(nextId))nextId++;return nextId;}
    private void cullChildren(int id) {
        var node=data().get(id);if(node==null)return;
        for(int child:node.children){cullChildren(child);data().remove(child);}
        node.children=new int[0];
    }
    private void replaceNode(Class<? extends IFocusElement> type,int id) {
        FocusNode instance;
        try {instance=(FocusNode)type.getDeclaredConstructor().newInstance();}
        catch(ReflectiveOperationException|ClassCastException error){return;}
        var previous=data().get(id);boolean same=previous!=null&&previous.node!=null&&previous.node.getClass()==type;
        cullChildren(id);
        FocusElementNode node;
        if(same)node=previous;
        else {
            if(previous!=null)data().remove(id);
            node=new FocusElementNode();node.node=instance;node.id=nextId();
            if(previous!=null){node.x=previous.x;node.y=previous.y;node.parent=previous.parent;}
            var parent=data().get(node.parent);
            if(parent!=null)for(int i=0;i<parent.children.length;i++)if(parent.children[i]==id)parent.children[i]=node.id;
            data().put(node.id,node);
        }
        selected=node.id;
        node.target=node.node.canSupply(FocusNode.EnumSupplyType.TARGET);node.trajectory=node.node.canSupply(FocusNode.EnumSupplyType.TRAJECTORY);
        if(node.target||node.trajectory) {
            var first=blank(node,node.node instanceof FocusModSplit?-1:0);selected=first.id;
            if(node.node instanceof FocusModSplit){var second=blank(node,1);node.children=new int[]{first.id,second.id};}
            else node.children=new int[]{first.id};
        }
        layout();rebuild();menu.sendEdit(data(),table.focusName);sound("crystal",1);
    }
    private FocusElementNode blank(FocusElementNode parent,int dx) {
        var result=new FocusElementNode();result.id=nextId();result.parent=parent.id;result.x=parent.x+dx;result.y=parent.y+1;data().put(result.id,result);return result;
    }
    private void layout() {
        for(var node:data().values())if(node.node instanceof FocusModSplit){int[] bounds={0};layoutLeft(node,bounds);move(node,bounds[0]/2);break;}
        for(var node:data().values())if(node.node instanceof FocusModSplit&&node.children.length>0) {
            var parent=data().get(node.parent);
            if(parent!=null&&parent.node!=null&&!(parent.node instanceof FocusModSplit))node.x=parent.x;
            else {int x=0;for(int child:node.children)x+=data().get(child).x;node.x=x/node.children.length;}
        }
        if(!data().containsKey(selected))selected=-1;
    }
    private void layoutLeft(FocusElementNode node,int[] bounds) {
        if(node.children.length>0)layoutLeft(data().get(node.children[0]),bounds);
        int old=0;
        if(node.children.length==1){old=bounds[0]-1;bounds[0]=data().get(node.children[0]).x;}
        node.x=bounds[0];if(node.children.length==1)bounds[0]=old;bounds[0]++;
        if(node.children.length>1)layoutLeft(data().get(node.children[1]),bounds);
    }
    private void move(FocusElementNode node,int amount){for(int child:node.children)move(data().get(child),amount);node.x-=amount;}
    private boolean vertical(){return maxY*32>130;}
    private boolean horizontal(){return minX*24<-70||maxX*24>70;}
    private void clampScroll(){scrollY=vertical()?Mth.clamp(scrollY,0,(maxY-3)*32):0;scrollX=horizontal()?Mth.clamp(scrollX,minX*24,maxX*24):0;partsStart=Mth.clamp(partsStart,0,Math.max(0,parts.size()-6));}
    private boolean inGraph(double x,double y){return over(x,y,leftPos+63,topPos+31,136,160);}
    private static boolean over(double x,double y,int left,int top,int w,int h){return x>=left&&x<left+w&&y>=top&&y<top+h;}
    private static void sprite(GuiGraphics g,ResourceLocation texture,int x,int y,int u,int v,int w,int h){BookAssets.sprite(g,texture,x,y,u,v,w,h);}
    private void sound(String key,float pitch){if(minecraft!=null&&minecraft.player!=null)minecraft.player.playSound(PortSounds.get(key),.4f,pitch);}
    @Override public boolean mouseClicked(double x,double y,int button) {
        if(button==0) {
            if(parts.size()>6&&over(x,y,leftPos+51,topPos+30,8,149)){drag=2;dragSlider(x,y);return true;}
            if(vertical()&&over(x,y,leftPos+203,topPos+32,8,156)){drag=3;dragSlider(x,y);return true;}
            if(horizontal()&&over(x,y,leftPos+64,topPos+195,132,8)){drag=4;dragSlider(x,y);return true;}
        }
        if(table.vis<=0) {
            if(name.visible&&name.mouseClicked(x,y,button)){setFocused(name);return true;}
            name.setFocused(false);
            if(button==0&&over(x,y,leftPos+230,topPos+10,24,16)) {
                checkResources();if(valid){menu.requestStart();sound("clack",1);}return true;
            }
            for(var spinner:settings)if(button==0&&over(x,y,leftPos+231,topPos+spinner.y,spinner.width+10,10)) {
                int old=spinner.setting.getValue();
                if(x<leftPos+241)spinner.setting.decrement();else if(x>=leftPos+231+spinner.width)spinner.setting.increment();
                if(old!=spinner.setting.getValue()){rebuild();menu.sendEdit(data(),table.focusName);sound("clack",1);}return true;
            }
            for(int i=0;i<6&&partsStart+i<parts.size();i++)if(button==0&&over(x,y,leftPos+28,topPos+33+i*25,20,20)) {
                replaceNode(FocusEngine.elements.get(parts.get(partsStart+i)),selected);return true;
            }
            if(inGraph(x,y))for(var node:new ArrayList<>(data().values())) {
                int nx=leftPos+132-scrollX+node.x*24,ny=topPos+48-scrollY+node.y*32;
                if(node.parent>=0&&over(x,y,nx-10,ny-10,20,20)) {
                    selected=node.id;
                    if(button==1&&node.node!=null){var parent=data().get(node.parent);if(parent!=null&&parent.node!=null)replaceNode(parent.node.getClass(),parent.id);}
                    rebuild();sound("clack",1);if(button==0)drag=1;return true;
                }
            }
        }
        if(button==0&&inGraph(x,y)){drag=1;return true;}
        return super.mouseClicked(x,y,button);
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy) {
        if(button==0&&drag>0) {
            if(drag==1){scrollX-=(int)Math.round(dx);scrollY-=(int)Math.round(dy);clampScroll();}
            else dragSlider(x,y);
            return true;
        }
        return super.mouseDragged(x,y,button,dx,dy);
    }
    private void dragSlider(double x,double y) {
        if(drag==2)partsStart=(int)Math.round(Mth.clamp((y-(topPos+30)-4)/141,0,1)*(parts.size()-6));
        if(drag==3)scrollY=(int)Math.round(Mth.clamp((y-(topPos+32)-4)/148,0,1)*(maxY-3)*32);
        if(drag==4)scrollX=minX*24+(int)Math.round(Mth.clamp((x-(leftPos+64)-4)/124,0,1)*(maxX-minX)*24);
        clampScroll();
    }
    @Override public boolean mouseReleased(double x,double y,int button){if(button==0&&drag>0){drag=0;return true;}return super.mouseReleased(x,y,button);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical) {
        if(parts.size()>6&&over(x,y,leftPos+24,topPos+24,35,157)){partsStart-=(int)Math.signum(vertical);clampScroll();return true;}
        if(inGraph(x,y)){scrollY-=(int)(vertical*16);scrollX-=(int)(horizontal*16);clampScroll();return true;}
        return super.mouseScrolled(x,y,horizontal,vertical);
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(name.isFocused()&&key!=256){name.keyPressed(key,scan,modifiers);return true;}
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean charTyped(char character,int modifiers){return name.isFocused()?name.charTyped(character,modifiers):super.charTyped(character,modifiers);}
    @Override protected boolean hasClickedOutside(double x,double y,int guiLeft,int guiTop,int button) {
        return !over(x,y,guiLeft-71,guiTop-3,392,239);
    }
    private record Spinner(NodeSetting setting,int y,int width) { }
}
