package com.builtbroken.handheldpiston.item.mode;

import com.builtbroken.handheldpiston.item.ItemHandheldPiston;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.BiFunction;

public enum PistonMode
{
    ALL(true, true),
    ENTITY(false, true),
    SELF(false, false, (item, player) -> item.selfModePush(player)),
    ADVANCED(true, false),
    PUSH_3(true, false, (item, world, pos, face, player, hand) -> item.push3(world, pos, face, player, hand));

    public final boolean canPushBlocks;
    public final boolean canPushEntity;

    public PistonModeFunction blockFunction;

    PistonMode(boolean canPushBlocks, boolean canPushEntity)
    {
        this.canPushBlocks = canPushBlocks;
        this.canPushEntity = canPushEntity;
    }

    PistonMode(boolean canPushBlocks, boolean canPushEntity, BiFunction<ItemHandheldPiston, EntityPlayer, EnumActionResult> blockFunction)
    {
        this(canPushBlocks, canPushEntity);
        this.blockFunction = (item, w, p, f, player, h) -> blockFunction.apply(item, player);
    }

    PistonMode(boolean canPushBlocks, boolean canPushEntity, PistonModeFunction blockFunction)
    {
        this(canPushBlocks, canPushEntity);
        this.blockFunction = blockFunction;
    }

    public EnumActionResult tryPush(ItemHandheldPiston piston, World world, BlockPos pos, EnumFacing facing, EntityPlayer player, EnumHand hand)
    {
        if (blockFunction != null)
        {
            return blockFunction.tryPush(piston, world, pos, facing, player, hand);
        }
        return EnumActionResult.PASS;
    }

    public static PistonMode get(int value)
    {
        if (value >= 0 && value < values().length)
        {
            return values()[value];
        }
        return ALL;
    }

    public PistonMode next()
    {
        int i = ordinal() + 1;
        if (i >= values().length)
        {
            i = 0;
        }
        return values()[i];
    }

    public PistonMode prev()
    {
        int i = ordinal() - 1;
        if (i < 0)
        {
            i = values().length - 1;
        }
        return values()[i];
    }

}
