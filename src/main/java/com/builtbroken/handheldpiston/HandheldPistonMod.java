package com.builtbroken.handheldpiston;

import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber
@Mod(modid = HandheldPistonMod.MODID, name = HandheldPistonMod.NAME, version = HandheldPistonMod.VERSION)
public class HandheldPistonMod {
	
    public static final String MODID = "handheldpiston";
    public static final String NAME = "[SBM] Handheld Piston";
    public static final String VERSION = "1.0.0";

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
    	
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
    	
    }
    
    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
    	event.getRegistry().registerAll(new ItemHandheldPiston("handheldpiston"));
    }
}
