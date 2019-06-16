package com.builtbroken.handheldpiston.item;

public enum PistonMode
{
    ALL(true),
    ENTITY(false),
    SELF(false),
    ADVANCED(true);

    public final boolean canPushBlocks;

    PistonMode(boolean canPushBlocks)
    {
        this.canPushBlocks = canPushBlocks;
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
