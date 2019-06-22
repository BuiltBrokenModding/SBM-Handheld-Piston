package com.builtbroken.handheldpiston.item;

import com.builtbroken.handheldpiston.HandheldPiston;
import com.builtbroken.handheldpiston.api.events.PistonMoveEntityEvent;
import com.builtbroken.handheldpiston.item.mode.PistonMode;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Created by Dark(DarkGuardsman, Robert) on 6/16/2019.
 */
@Mod.EventBusSubscriber(modid = HandheldPiston.MODID)
public class EntityEvent
{
    @SubscribeEvent
    public static void entityInteract(PlayerInteractEvent.EntityInteract event)
    {
        final World world = event.getWorld();
        final Entity target = event.getTarget();
        final ItemStack heldItem = event.getItemStack();
        if (heldItem.getItem() instanceof ItemHandheldPiston)
        {
            final ItemHandheldPiston piston = ((ItemHandheldPiston) heldItem.getItem());
            final PistonMode mode = piston.getMode(heldItem);
            final boolean inverse = piston.inverse;
            if (mode.canPushEntity)
            {

                float rot = event.getEntityPlayer().getRotationYawHead();
                EnumFacing facing = EnumFacing.fromAngle(rot);

                //Movement
                final Vec3d vec = getVelocityForPush(facing, target).scale(inverse ? -1 : 1);
                piston.applyMotion(target, vec);
                //TODO scale force based on entity size
                //TODO push play back equal to scale
                //TODO config for scale

                piston.extend(world, event.getPos(), event.getEntityPlayer(), event.getHand());
            }
        }
    }

    public static Vec3d getVelocityForPush(EnumFacing facing, Entity entity)
    {
        double velX = facing.getXOffset(); //TODO scale
        double velY = facing.getYOffset();
        double velZ = facing.getZOffset();

        if (!entity.onGround) //TODO why?
        {
            velY /= 2;
        }

        //Fire events
        final PistonMoveEntityEvent event = new PistonMoveEntityEvent(entity, new Vec3d(velX, velY, velZ));
        if (!MinecraftForge.EVENT_BUS.post(event))
        {
            return event.motion;
        }
        return new Vec3d(0, 0, 0);
    }
}
