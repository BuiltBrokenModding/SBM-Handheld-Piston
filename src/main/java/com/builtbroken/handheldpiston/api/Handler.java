package com.builtbroken.handheldpiston.api;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Handles interaction between the box and a single tile. Allows for customizing placement.
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 7/28/2015.
 */
public class Handler {

    /**
     * Called before movement of the tile
     * @return Data to be passed to postMove for handling
     */
    public NBTTagCompound preMoveBlock(EntityPlayer player, World world, BlockPos oldPos, BlockPos newPos)
    {
        return new NBTTagCompound();
    }

    /**
     * Called after the block has been moved to do post interaction
     */
    public void postMoveBlock(EntityPlayer player, World world, BlockPos oldPos, BlockPos newPos, NBTTagCompound data)
    {

    }
}