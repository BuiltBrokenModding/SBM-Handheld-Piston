package com.builtbroken.handheldpiston;

public enum PistonMode
{

    ALL,
    ENTITY,
    SELF;


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
