package com.builtbroken.handheldpiston.mod;

import java.util.function.Supplier;

/**
 * Created by Dark(DarkGuardsman, Robert) on 6/16/2019.
 */
public class LazyModLoader
{
    private ModHandler handler;
    private Supplier<ModHandler> builder;

    public LazyModLoader(Supplier<ModHandler> builder)
    {
        this.builder = builder;
    }

    public ModHandler get()
    {
        if(handler == null)
        {
            handler = builder.get();
        }
        return handler;
    }
}
