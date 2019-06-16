package com.builtbroken.handheldpiston.item;

import com.builtbroken.handheldpiston.api.HandheldPistonMoveEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
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
            if (mode.canPushEntity)
            {

                float rot = event.getEntityPlayer().getRotationYawHead();
                EnumFacing facing = EnumFacing.fromAngle(rot);

                //Movement
                final Vector3d vec = getVelocityForPush(facing, target, event.getItemStack(), mode);
                target.addVelocity(vec.getX(), vec.getY(), vec.getZ());

                //Audio
                world.playSound((EntityPlayer) null, event.getEntityPlayer().getPosition(), SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.5F, event.getWorld().rand.nextFloat() * 0.25F + 0.6F);

                //Animation
                ItemHandheldPiston.setExtended(event.getEntityPlayer().getHeldItem(event.getHand()), event.getWorld());
            }
        }
    }

    public static Vector3d getVelocityForPush(EnumFacing facing, Entity entity, ItemStack stack, PistonMode mode)
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
        if (mode == PistonMode.SELF && entity instanceof EntityPlayer)
        {
            velX *= 1.5F;
            velY *= 2.5F;
            velZ *= 1.5F;
        }
        HandheldPistonMoveEvent event = new HandheldPistonMoveEvent(entity instanceof EntityPlayer ? HandheldPistonMoveEvent.PistonMoveType.PLAYER : HandheldPistonMoveEvent.PistonMoveType.ENTITY, null, null, new Vector3d(velX, velY, velZ));
        MinecraftForge.EVENT_BUS.post(event);
        if (!event.isCanceled())
        {
            return event.velocityAdded;
        }
        return new Vector3d(0, 0, 0);
    }
}
