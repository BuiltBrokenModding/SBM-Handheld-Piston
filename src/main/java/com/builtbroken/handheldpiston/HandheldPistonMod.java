package com.builtbroken.handheldpiston;

import com.builtbroken.handheldpiston.network.MouseHandler;
import com.builtbroken.handheldpiston.network.MousePacket;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;

import javax.vecmath.Vector3d;
import java.io.File;

@Mod.EventBusSubscriber
@Mod(modid = HandheldPistonMod.MODID, name = HandheldPistonMod.NAME, version = HandheldPistonMod.VERSION)
public class HandheldPistonMod
{

    public static final String MODID = "handheldpiston";
    public static final String NAME = "[SBM] Handheld Piston";
    public static final String VERSION = "1.0.0";

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(HandheldPistonMod.MODID);

    public static Logger LOGGER = null;

    private static Configuration config;

    public static final ItemHandheldPiston piston = new ItemHandheldPiston("handheldpiston");

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        LOGGER = event.getModLog();
        config = new Configuration(new File(event.getModConfigurationDirectory(), "bbm/handheld_piston.cfg"));
        INSTANCE.registerMessage(MouseHandler.class, MousePacket.class, 0, Side.SERVER);
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        ModHandler.modSupportHandlerMap.put("minecraft", VanillaHandler.class);
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        config.load();
        ModHandler.loadHandlerData(config);
        config.save();
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event)
    {
        event.getRegistry().registerAll(piston);
    }

    @SubscribeEvent
    public static void entityInteract(PlayerInteractEvent.EntityInteract e)
    {
        if (e.getItemStack().getItem() != piston || !(e.getTarget() instanceof EntityLivingBase))
        {
            return;
        }
        if (piston.getMode(e.getItemStack()) == PistonMode.SELF)
        {
            return;
        }
        float rot = e.getEntityPlayer().getRotationYawHead();
        EnumFacing facing = EnumFacing.fromAngle(rot);
        Vector3d vec = ItemHandheldPiston.getVelocityForPush(facing, (EntityLivingBase) e.getTarget(), e.getItemStack());
        e.getTarget().addVelocity(vec.getX(), vec.getY(), vec.getZ());
        e.getWorld().playSound((EntityPlayer) null, e.getEntityPlayer().getPosition(), SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.5F, e.getWorld().rand.nextFloat() * 0.25F + 0.6F);
        ItemHandheldPiston.setExtended(e.getEntityPlayer().getHeldItem(e.getHand()), e.getWorld());
    }
}
