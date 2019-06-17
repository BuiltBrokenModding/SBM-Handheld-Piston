package com.builtbroken.handheldpiston;

import com.builtbroken.handheldpiston.item.ItemHandheldPiston;
import com.builtbroken.handheldpiston.item.PistonMode;
import com.builtbroken.handheldpiston.mod.LazyModLoader;
import com.builtbroken.handheldpiston.mod.ModHandler;
import com.builtbroken.handheldpiston.mod.vanilla.VanillaHandler;
import com.builtbroken.handheldpiston.network.MouseHandler;
import com.builtbroken.handheldpiston.network.MousePacket;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber
@Mod(modid = HandheldPiston.MODID, name = HandheldPiston.NAME, version = HandheldPiston.VERSION)
public class HandheldPiston
{

    public static final String MODID = "handheldpiston";
    public static final String NAME = "[SBM] Handheld Piston";
    public static final String VERSION = "1.0.0";

    public static final SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(HandheldPiston.MODID);

    public static Logger LOGGER = null;

    public static final ResourceLocation ITEM_BASIC = new ResourceLocation(MODID, "basic");
    public static final ResourceLocation ITEM_STICKY = new ResourceLocation(MODID, "sticky");
    public static final ResourceLocation ITEM_ADVANCED = new ResourceLocation(MODID, "advanced");

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        LOGGER = event.getModLog();
        NETWORK.registerMessage(MouseHandler.class, MousePacket.class, 0, Side.SERVER);
        ModHandler.modSupportHandlerMap.put("minecraft", new LazyModLoader(() -> new VanillaHandler()));
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        ModHandler.loadHandlerData();
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event)
    {
        event.getRegistry().registerAll(new ItemHandheldPiston(ITEM_BASIC, false, PistonMode.ALL, PistonMode.ENTITY, PistonMode.SELF));
        event.getRegistry().registerAll(new ItemHandheldPiston(ITEM_STICKY, true, PistonMode.ALL, PistonMode.ENTITY, PistonMode.SELF));
        event.getRegistry().registerAll(new ItemHandheldPiston(ITEM_ADVANCED, false, PistonMode.ALL, PistonMode.ENTITY, PistonMode.SELF, PistonMode.ADVANCED));
    }
}
