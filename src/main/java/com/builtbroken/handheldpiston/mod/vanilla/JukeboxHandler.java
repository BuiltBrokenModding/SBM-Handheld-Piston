package com.builtbroken.handheldpiston.mod.vanilla;

import com.builtbroken.handheldpiston.api.Handler;
import net.minecraft.block.BlockJukebox;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Created by Dark(DarkGuardsman, Robert) on 6/16/2019.
 */
public class JukeboxHandler extends Handler
{
    @Override
    public NBTTagCompound preMoveBlock(EntityPlayer player, World world, BlockPos oldPos, BlockPos newPos)
    {
        final TileEntity te = world.getTileEntity(oldPos);
        if (te instanceof BlockJukebox.TileEntityJukebox)
        {
            BlockJukebox.TileEntityJukebox juke = (BlockJukebox.TileEntityJukebox) te;
            ItemStack itemstack = juke.getRecord();

            if (!itemstack.isEmpty())
            {
                world.playEvent(1010, oldPos, 0);
                world.playRecord(oldPos, (SoundEvent) null);
                juke.setRecord(ItemStack.EMPTY);
                world.setBlockState(oldPos, world.getBlockState(oldPos).withProperty(BlockJukebox.HAS_RECORD, Boolean.valueOf(false)));
                return itemstack.writeToNBT(new NBTTagCompound());
            }
        }
        return super.preMoveBlock(player, world, oldPos, newPos);
    }

    @Override
    public void postMoveBlock(EntityPlayer player, World world, BlockPos oldPos, BlockPos newPos, NBTTagCompound data)
    {
        ((BlockJukebox) Blocks.JUKEBOX).insertRecord(world, newPos, world.getBlockState(newPos), new ItemStack(data));
    }
}
