package com.builtbroken.handheldpiston.item;

import com.builtbroken.handheldpiston.api.HandheldPistonMoveEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.vecmath.Vector3d;

/**
 * Created by Dark(DarkGuardsman, Robert) on 6/16/2019.
 */
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
            final PistonMode mode = ((ItemHandheldPiston) heldItem.getItem()).getMode(heldItem);
            final boolean inverse = ((ItemHandheldPiston) heldItem.getItem()).inverse;
            if (mode.canPushEntity)
            {

                float rot = event.getEntityPlayer().getRotationYawHead();
                EnumFacing facing = EnumFacing.fromAngle(rot);

                //Movement
                final Vec3d vec = getVelocityForPush(facing, target).scale(inverse ? -1 : 1);
                target.addVelocity(vec.x, vec.y, vec.z);
                //TODO scale force based on entity size
                //TODO push play back equal to scale
                //TODO config for scale

                //Audio
                world.playSound((EntityPlayer) null, event.getEntityPlayer().getPosition(), SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.5F, event.getWorld().rand.nextFloat() * 0.25F + 0.6F);

                //Animation
                ItemHandheldPiston.setExtended(event.getEntityPlayer().getHeldItem(event.getHand()), event.getWorld());
            }
        }
    }

    public static Vec3d getVelocityForPush(EnumFacing facing, Entity entity)
    {
        double velX = 0.5;
        double velY = 0.5;
        double velZ = 0.5;
        if (facing == EnumFacing.UP)
        {
            velX = 0;
            velY = Math.abs(velY);
            velZ = 0;
        }
        if (facing == EnumFacing.DOWN)
        {
            velX = 0;
            velY = -Math.abs(velY);
            velZ = 0;
        }
        if (facing == EnumFacing.NORTH)
        {
            velX = 0;
            velY = 0;
            velZ = -Math.abs(velZ);
        }
        if (facing == EnumFacing.EAST)
        {
            velX = Math.abs(velX);
            velY = 0;
            velZ = 0;
        }
        if (facing == EnumFacing.SOUTH)
        {
            velX = 0;
            velY = 0;
            velZ = Math.abs(velZ);
        }
        if (facing == EnumFacing.WEST)
        {
            velX = -Math.abs(velX);
            velY = 0;
            velZ = 0;
        }
        if (!entity.onGround)
        {
            velY /= 2;
        }
        HandheldPistonMoveEvent event = new HandheldPistonMoveEvent(entity instanceof EntityPlayer ? HandheldPistonMoveEvent.PistonMoveType.PLAYER : HandheldPistonMoveEvent.PistonMoveType.ENTITY, null, null, new Vec3d(velX, velY, velZ));
        MinecraftForge.EVENT_BUS.post(event);
        if (!event.isCanceled())
        {
            return event.velocityAdded;
        }
        return new Vec3d(0, 0, 0);
    }
}
