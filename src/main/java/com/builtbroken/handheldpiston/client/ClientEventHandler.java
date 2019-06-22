package com.builtbroken.handheldpiston.client;

import com.builtbroken.handheldpiston.HandheldPiston;
import com.builtbroken.handheldpiston.item.ItemHandheldPiston;
import com.builtbroken.handheldpiston.network.MousePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = HandheldPiston.MODID, value = Side.CLIENT)
public class ClientEventHandler
{
    @SubscribeEvent
    public static void modelRegister(ModelRegistryEvent event)
    {
        reg(HandheldPiston.ITEM_BASIC);
        reg(HandheldPiston.ITEM_STICKY);
        reg(HandheldPiston.ITEM_ADVANCED);
        reg(HandheldPiston.ITEM_CREATIVE);
    }

    private static void reg(ResourceLocation name)
    {
        Item item = ForgeRegistries.ITEMS.getValue(name);
        ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(name, "inventory"));
    }

    @SubscribeEvent
    public static void mouseEvent(MouseEvent e)
    {
        final EntityPlayer player = Minecraft.getMinecraft().player;
        for (EnumHand hand : EnumHand.values())
        {
            ItemStack stack = player.getHeldItem(hand);
            if (stack.getItem() instanceof ItemHandheldPiston)
            {
                if (player.isSneaking() && e.getDwheel() != 0)
                {
                    HandheldPiston.NETWORK.sendToServer(new MousePacket(player.inventory.currentItem, e.getDwheel() > 0));
                    e.setCanceled(true);
                }
                break;
            }
        }
    }

}
