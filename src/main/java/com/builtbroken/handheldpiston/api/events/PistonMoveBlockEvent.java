package com.builtbroken.handheldpiston.api.events;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

@Cancelable
public class PistonMoveBlockEvent extends BlockEvent
{
    public final EnumFacing pushDirection;

    public IBlockState newState;

    public PistonMoveBlockEvent(World world, BlockPos pos, IBlockState state, EnumFacing pushDirection)
    {
        super(world, pos, state);
        this.newState = state;
        this.pushDirection = pushDirection;
    }
}