/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.block.SoundType
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.SoundEvent
 *  net.minecraftforge.event.RegistryEvent$Register
 *  net.minecraftforge.registries.IForgeRegistryEntry
 */
package thaumcraft.common.lib;

import net.minecraft.block.SoundType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.registries.IForgeRegistryEntry;

public class SoundsTC {
    public static SoundEvent zap;
    public static SoundEvent heartbeat;
    public static SoundEvent spill;
    public static SoundEvent bubble;
    public static SoundEvent creak;
    public static SoundEvent squeek;
    public static SoundEvent jar;
    public static SoundEvent swarm;
    public static SoundEvent swarmattack;
    public static SoundEvent fly;
    public static SoundEvent key;
    public static SoundEvent ticks;
    public static SoundEvent clack;
    public static SoundEvent pump;
    public static SoundEvent poof;
    public static SoundEvent page;
    public static SoundEvent learn;
    public static SoundEvent write;
    public static SoundEvent erase;
    public static SoundEvent brain;
    public static SoundEvent crystal;
    public static SoundEvent wispdead;
    public static SoundEvent wisplive;
    public static SoundEvent wand;
    public static SoundEvent wandfail;
    public static SoundEvent rumble;
    public static SoundEvent ice;
    public static SoundEvent jacobs;
    public static SoundEvent hhoff;
    public static SoundEvent hhon;
    public static SoundEvent pech_idle;
    public static SoundEvent pech_trade;
    public static SoundEvent pech_dice;
    public static SoundEvent pech_hit;
    public static SoundEvent pech_death;
    public static SoundEvent pech_charge;
    public static SoundEvent shock;
    public static SoundEvent craftfail;
    public static SoundEvent scan;
    public static SoundEvent craftstart;
    public static SoundEvent runicShieldEffect;
    public static SoundEvent runicShieldCharge;
    public static SoundEvent wind;
    public static SoundEvent tool;
    public static SoundEvent gore;
    public static SoundEvent tentacle;
    public static SoundEvent upgrade;
    public static SoundEvent whispers;
    public static SoundEvent monolith;
    public static SoundEvent infuser;
    public static SoundEvent infuserstart;
    public static SoundEvent egidle;
    public static SoundEvent egattack;
    public static SoundEvent egdeath;
    public static SoundEvent egscreech;
    public static SoundEvent crabclaw;
    public static SoundEvent crabdeath;
    public static SoundEvent crabtalk;
    public static SoundEvent chant;
    public static SoundEvent coins;
    public static SoundEvent urnbreak;
    public static SoundEvent evilportal;
    public static SoundEvent grind;
    public static SoundEvent dust;
    public static SoundEvent pageturn;
    public static SoundType GORE;
    public static SoundType CRYSTAL;
    public static SoundType JAR;
    public static SoundType URN;

    public static void registerSoundTypes() {
        GORE = new SoundType(0.5f, 1.0f, gore, gore, gore, gore, gore);
        CRYSTAL = new SoundType(0.5f, 1.0f, crystal, crystal, crystal, crystal, crystal);
        JAR = new SoundType(0.5f, 1.0f, jar, jar, jar, jar, jar);
        URN = new SoundType(0.5f, 1.5f, urnbreak, urnbreak, urnbreak, urnbreak, urnbreak);
    }

    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        zap = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:zap");
        heartbeat = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:heartbeat");
        spill = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:spill");
        bubble = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:bubble");
        creak = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:creak");
        squeek = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:squeek");
        jar = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:jar");
        swarm = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:swarm");
        swarmattack = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:swarmattack");
        fly = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:fly");
        key = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:key");
        ticks = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:ticks");
        clack = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:clack");
        pump = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:pump");
        poof = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:poof");
        page = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:page");
        pageturn = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:pageturn");
        learn = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:learn");
        write = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:write");
        erase = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:erase");
        brain = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:brain");
        crystal = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:crystal");
        wispdead = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:wispdead");
        wisplive = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:wisplive");
        wand = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:wand");
        wandfail = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:wandfail");
        rumble = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:rumble");
        ice = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:ice");
        jacobs = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:jacobs");
        hhoff = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:hhoff");
        hhon = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:hhon");
        pech_idle = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:pech_idle");
        pech_trade = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:pech_trade");
        pech_dice = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:pech_dice");
        pech_hit = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:pech_hit");
        pech_death = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:pech_death");
        pech_charge = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:pech_charge");
        shock = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:shock");
        craftfail = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:craftfail");
        scan = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:scan");
        craftstart = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:craftstart");
        runicShieldEffect = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:runicshieldeffect");
        runicShieldCharge = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:runicshieldcharge");
        wind = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:wind");
        tool = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:tool");
        gore = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:gore");
        tentacle = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:tentacle");
        upgrade = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:upgrade");
        whispers = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:whispers");
        monolith = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:monolith");
        infuser = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:infuser");
        infuserstart = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:infuserstart");
        egidle = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:egidle");
        egattack = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:egattack");
        egdeath = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:egdeath");
        egscreech = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:egscreech");
        crabclaw = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:crabclaw");
        crabdeath = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:crabdeath");
        crabtalk = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:crabtalk");
        chant = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:chant");
        coins = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:coins");
        urnbreak = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:urnbreak");
        evilportal = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:evilportal");
        grind = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:grind");
        dust = SoundsTC.getRegisteredSoundEvent(event, "thaumcraft:dust");
    }

    private static SoundEvent getRegisteredSoundEvent(RegistryEvent.Register<SoundEvent> event, String id) {
        SoundEvent soundevent = new SoundEvent(new ResourceLocation(id));
        soundevent.setRegistryName(new ResourceLocation(id));
        event.getRegistry().register((IForgeRegistryEntry)soundevent);
        return soundevent;
    }
}

