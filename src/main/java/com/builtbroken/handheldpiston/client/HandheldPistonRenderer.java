package com.builtbroken.handheldpiston.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;

public class HandheldPistonRenderer extends TileEntityItemStackRenderer
{

    // This is set properly in the model class
    public TransformType transform = TransformType.GUI;

    @Override
    public void renderByItem(ItemStack itemStack)
    {
        super.renderByItem(itemStack);
        IBakedModel model = ClientEventHandler.piston;
        if (itemStack.hasTagCompound())
        {
            long t = Minecraft.getMinecraft().world.getTotalWorldTime() - itemStack.getTagCompound().getLong("extendTick");
            if (t < 15)
            {
                model = ClientEventHandler.piston_extended;
            }
        }
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.5F, 0.5F, 0.5F);
        Minecraft.getMinecraft().getRenderItem().renderItem(itemStack, model);
        GlStateManager.popMatrix();
    }

}
