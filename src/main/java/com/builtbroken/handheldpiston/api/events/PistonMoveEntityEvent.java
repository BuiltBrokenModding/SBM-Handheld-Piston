package com.builtbroken.handheldpiston.api.events;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.fml.common.eventhandler.Cancelable;

/**
 * Called any time the piston is pushing or pulling an entity
 * <p>
 * Created by Dark(DarkGuardsman, Robert) on 6/22/2019.
 */
@Cancelable
public class PistonMoveEntityEvent extends EntityEvent
{
    /** Motion added */
    public Vec3d motion;

    public PistonMoveEntityEvent(Entity entity, Vec3d motion)
    {
        super(entity);
        this.motion = motion;
    }
}
