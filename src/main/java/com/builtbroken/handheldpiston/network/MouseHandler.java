package com.builtbroken.handheldpiston.network;

import com.builtbroken.handheldpiston.item.ItemHandheldPiston;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MouseHandler implements IMessageHandler<MousePacket, IMessage>
{

    @Override
    public IMessage onMessage(MousePacket p, MessageContext ctx)
    {

        EntityPlayerMP player = ctx.getServerHandler().player;

        ItemStack stack = player.inventory.getStackInSlot(p.slot);
        if (stack != null && stack.getItem() instanceof ItemHandheldPiston)
        {
            ((ItemHandheldPiston)stack.getItem()).handleMouseWheelAction(stack, player, false, p.forward);
        }

        return null;
    }
}