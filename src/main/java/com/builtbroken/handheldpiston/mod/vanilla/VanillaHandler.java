package com.builtbroken.handheldpiston.mod.vanilla;

import com.builtbroken.handheldpiston.api.HandlerManager;
import com.builtbroken.handheldpiston.mod.ModHandler;
import net.minecraft.init.Blocks;

public class VanillaHandler extends ModHandler
{
    @Override
    public void load()
    {
        HandlerManager.INSTANCE.registerHandler(Blocks.JUKEBOX, new JukeboxHandler());
        HandlerManager.INSTANCE.registerHandler(Blocks.CHEST, new ChestHandler());
    }
}
