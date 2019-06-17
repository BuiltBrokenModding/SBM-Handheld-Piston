package com.builtbroken.handheldpiston.api;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.vecmath.Vector3d;

@Cancelable
public class HandheldPistonMoveEvent extends Event
{

    @Nonnull
    private final PistonMoveType type;

    private final BlockPos newPos;
    private final BlockPos oldPos;

    @Nullable
    public Vec3d velocityAdded;

    public PistonMoveType getType()
    {
        return type;
    }

    @Nullable
    public BlockPos getNewPos()
    {
        return newPos;
    }

    @Nullable
    public BlockPos getOldPos()
    {
        return oldPos;
    }


    public HandheldPistonMoveEvent(PistonMoveType type, BlockPos oldPos, BlockPos newPos, Vec3d velocityAdded)
    {
        this.type = type;
        this.newPos = newPos;
        this.oldPos = oldPos;
        this.velocityAdded = velocityAdded;
    }

    public static enum PistonMoveType
    {
        ENTITY,
        PLAYER,
        BLOCK;
    }

}