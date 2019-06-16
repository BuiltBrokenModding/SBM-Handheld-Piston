package com.builtbroken.handheldpiston.mod.vanilla;

import com.builtbroken.handheldpiston.api.Handler;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Created by Dark(DarkGuardsman, Robert) on 6/16/2019.
 */
public class ChestHandler extends Handler
{
    @Override
    public EnumActionResult canEdit(World world, IBlockState state, BlockPos oldPos, BlockPos newPos,
                                    EntityPlayer player, EnumFacing facing, EnumHand hand,
                                    boolean mayPlace, boolean canEditBlock, boolean canEditWithTool)
    {
        if (canEditBlock && canEditWithTool)
        {
            if (world.getBlockState(newPos).getBlock().isReplaceable(world, newPos) && canPlaceBlockAt(world, newPos, oldPos))
            {
                return EnumActionResult.SUCCESS;
            }
        }
        return EnumActionResult.FAIL;
    }

    private boolean canPlaceBlockAt(World worldIn, BlockPos checkPos, BlockPos posToIgnore)
    {
        int i = 0;

        for (EnumFacing facing : EnumFacing.HORIZONTALS)
        {
            final BlockPos pos = checkPos.offset(facing);
            if (!pos.equals(posToIgnore) && worldIn.getBlockState(pos).getBlock() == Blocks.CHEST)
            {
                i++;
                if (isDoubleChest(worldIn, pos))
                {
                    return false;
                }
            }
        }

        return i <= 1;
    }

    private boolean isDoubleChest(World worldIn, BlockPos pos)
    {
        for (EnumFacing enumfacing : EnumFacing.Plane.HORIZONTAL)
        {
            if (worldIn.getBlockState(pos.offset(enumfacing)).getBlock() == Blocks.CHEST)
            {
                return true;
            }
        }

        return false;
    }
}
