package com.builtbroken.handheldpiston.item.mode;

import com.builtbroken.handheldpiston.item.ItemHandheldPiston;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Created by Dark(DarkGuardsman, Robert) on 6/22/2019.
 */
@FunctionalInterface
public interface PistonModeFunction
{

    /**
     * Tries to push the block at the location
     *
     * @param world  - world to push in
     * @param pos    - location to push
     * @param facing - side to push towards
     * @param player - player doing the action
     * @param hand   - hand the piston is inside
     * @return result of the action, fail to default to fail to push action, success to extend piston, pass to allow default code to run
     */
    EnumActionResult tryPush(ItemHandheldPiston piston, World world, BlockPos pos, EnumFacing facing, EntityPlayer player, EnumHand hand);
}
