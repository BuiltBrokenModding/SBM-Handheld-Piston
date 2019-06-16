package com.builtbroken.handheldpiston.item;

public enum PistonMode
{
    ALL(true, true),
    ENTITY(false, true),
    SELF(false, false),
    ADVANCED(true, false);

    public final boolean canPushBlocks;
    public final boolean canPushEntity;

    PistonMode(boolean canPushBlocks, boolean canPushEntity)
    {
        this.canPushBlocks = canPushBlocks;
        this.canPushEntity = canPushEntity;
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
