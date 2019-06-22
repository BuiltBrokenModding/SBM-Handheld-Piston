package com.builtbroken.handheldpiston.item;

import com.builtbroken.handheldpiston.HandheldPiston;
import com.builtbroken.handheldpiston.api.CanPushResult;
import com.builtbroken.handheldpiston.api.Handler;
import com.builtbroken.handheldpiston.api.HandlerManager;
import com.builtbroken.handheldpiston.api.events.PistonMoveBlockEvent;
import com.builtbroken.handheldpiston.item.mode.PistonMode;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;


public class ItemHandheldPiston extends Item
{

    public static final String NBT_MODE = "toolMode";
    public static final String NBT_TICKS = "extendTick";

    public final Set<PistonMode> modes = new HashSet();
    public final boolean inverse;

    public ItemHandheldPiston(ResourceLocation registryName, boolean inverse, PistonMode... modes)
    {
        this.setRegistryName(registryName);
        this.setTranslationKey(registryName.toString());
        this.setCreativeTab(CreativeTabs.TOOLS);
        this.setMaxStackSize(1);

        this.inverse = inverse;

        for (PistonMode mode : modes)
        {
            this.modes.add(mode);
        }

        //handles extended state
        this.addPropertyOverride(new ResourceLocation("extending"), new IItemPropertyGetter()
        {
            @SideOnly(Side.CLIENT)
            public float apply(ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn)
            {
                if (stack.getItem() == ItemHandheldPiston.this)
                {
                    int ticks = ((ItemHandheldPiston) stack.getItem()).getExtendedTime(stack);
                    if (worldIn != null && worldIn.getTotalWorldTime() - ticks < 15) //TODO make config driven
                    {
                        return 1.0F;
                    }
                }
                return 0.0F;
            }
        });
    }

    @Override
    public EnumActionResult onItemUseFirst(EntityPlayer player, World world, BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ, EnumHand hand)
    {
        return onItemUse(player, world, pos, hand, side, hitX, hitY, hitZ);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing sideHit, float hitX, float hitY, float hitZ)
    {
        if (world.isRemote)
        {
            return EnumActionResult.PASS;
        }

        final ItemStack stack = player.getHeldItem(hand);

        //Fail out if we are already extended
        if (world.getTotalWorldTime() - getExtendedTime(stack) < 15) //TODO make config driven
        {
            return EnumActionResult.FAIL;
        }

        final PistonMode mode = getMode(stack);

        //Get push direction
        EnumFacing pushDirection = sideHit;
        if (mode == PistonMode.ADVANCED)
        {
            pushDirection = getPlacement(sideHit, hitX, hitY, hitZ);
        }
        else if (inverse)
        {
            pushDirection = pushDirection.getOpposite();
        }

        //Try to push using custom mode logic
        EnumActionResult result = mode.tryPush(this, world, pos, pushDirection, player, hand);

        //Do default logic
        if (result == EnumActionResult.PASS && mode.canPushBlocks)
        {
            result = doPushLogic(world, pos, pushDirection, player, hand);
        }

        //As long as we don't skip extend
        if (result != EnumActionResult.PASS)
        {
            extend(world, pos, player, hand);
        }

        //IF we fail push the play backwards
        if (result == EnumActionResult.FAIL)
        {
            pushPlayer(player, pushDirection);
        }
        return EnumActionResult.SUCCESS;
    }

    public EnumActionResult doPushLogic(World world, BlockPos pos, EnumFacing pushDirection, EntityPlayer player, EnumHand hand)
    {
        final BlockPos newPos = pos.offset(pushDirection.getOpposite());
        final IBlockState oldState = world.getBlockState(pos);
        final IBlockState filledState = world.getBlockState(newPos);

        //Check if we can push
        if (this.canTryPush(world, oldState, filledState, pos, newPos, hand, player, pushDirection))
        {
            final PistonMoveBlockEvent event = new PistonMoveBlockEvent(world, pos, oldState, pushDirection);
            if (MinecraftForge.EVENT_BUS.post(event))
            {
                return EnumActionResult.FAIL;
            }
            return this.tryToMoveBlock(player, world, pos, newPos, pushDirection, event.newState, filledState);
        }
        return EnumActionResult.FAIL;
    }

    public EnumActionResult push3(World world, BlockPos pos, EnumFacing pushDirection, EntityPlayer player, EnumHand hand)
    {
        for (int i = 0; i < 3; i++)
        {
            EnumActionResult result = doPushLogic(world, pos, pushDirection, player, hand);
            if (result == EnumActionResult.SUCCESS)
            {
                pos = pos.offset(pushDirection.getOpposite());
            }
            else
            {
                return result;
            }
        }
        return EnumActionResult.SUCCESS;
    }

    protected boolean canTryPush(World world, IBlockState oldState, IBlockState filledState, BlockPos pos, BlockPos newPos, EnumHand hand, EntityPlayer player, EnumFacing pushDirection)
    {
        if (newPos.getY() < world.getHeight() && newPos.getY() > 0)
        {
            boolean pushable = (oldState.getPushReaction() == EnumPushReaction.NORMAL || oldState.getPushReaction() == EnumPushReaction.PUSH_ONLY);
            boolean hardness = oldState.getBlockHardness(world, pos) != -1.0F;
            boolean replaceable = (filledState.getBlock() == Blocks.AIR || filledState.getBlock().isReplaceable(world, newPos));
            boolean extras = oldState.getBlock() != Blocks.OBSIDIAN && world.getWorldBorder().contains(pos);
            if (((pushable && hardness && replaceable) || oldState.getPushReaction() == EnumPushReaction.DESTROY) && extras)
            {
                if (!world.isRemote)
                {
                    boolean eventsPass = postMovementEvents(world, player, pos, newPos, hand);
                    boolean canEdit = canEdit(world, oldState, pos, newPos, player, pushDirection, hand);
                    return eventsPass && canEdit;
                }
            }
        }
        return false;
    }

    public void extend(World world, BlockPos pos, EntityPlayer player, EnumHand hand)
    {
        //Animation
        this.setExtended(player.getHeldItem(hand), world);
        world.playSound((EntityPlayer) null, pos, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.5F, world.rand.nextFloat() * 0.25F + 0.6F);
    }

    public void applyMotion(Entity target, Vec3d velocity)
    {
        target.addVelocity(velocity.x, velocity.y, velocity.z);
        if (target instanceof EntityPlayerMP)
        {
            ((EntityPlayerMP) target).connection.sendPacket(new SPacketEntityVelocity(target));
        }
    }

    public EnumActionResult selfModePush(EntityPlayer player)
    {
        Vec3d vector = player.getLookVec().scale(inverse ? 1 : -1).normalize().scale(2); //TODO config
        applyMotion(player, vector);
        return EnumActionResult.SUCCESS;
    }

    public void pushPlayer(EntityPlayer player, EnumFacing facing)
    {
       applyMotion(player, EntityEvent.getVelocityForPush(facing, player));
    }

    public static EnumFacing getPlacement(EnumFacing blockSide, float hitX, float hitY, float hitZ)
    {
        final float spacing = 0.3f;
        EnumFacing placement;

        if (blockSide == EnumFacing.UP || blockSide == EnumFacing.DOWN)
        {
            //WEST
            boolean right = hitX <= spacing;
            //EAST
            boolean left = hitX >= (1 - spacing);
            //NORTH
            boolean down = hitZ <= spacing;
            //SOUTH
            boolean up = hitZ >= (1 - spacing);

            if (!up && !down && (left || right))
            {
                placement = left ? EnumFacing.WEST : EnumFacing.EAST;
            }
            else if (!left && !right && (up || down))
            {
                placement = up ? EnumFacing.NORTH : EnumFacing.SOUTH;
            }
            else if (!left && !right && !up && !down)
            {
                placement = blockSide;
            }
            else
            {
                placement = blockSide.getOpposite();
            }
        }
        else
        {
            boolean z = blockSide.getAxis() == EnumFacing.Axis.Z;
            boolean right = (z ? hitX : hitZ) <= spacing;
            boolean left = (z ? hitX : hitZ) >= (1 - spacing);

            boolean up = hitY <= spacing;
            boolean down = hitY >= (1 - spacing);

            if (!up && !down && (left || right))
            {
                if (z)
                {
                    placement = left ? EnumFacing.WEST : EnumFacing.EAST;
                }
                else
                {
                    placement = left ? EnumFacing.NORTH : EnumFacing.SOUTH;
                }
            }
            else if (!left && !right && (up || down))
            {
                placement = up ? EnumFacing.UP : EnumFacing.DOWN;
            }
            else if (!left && !right && !up && !down)
            {
                placement = blockSide;
            }
            else
            {
                placement = blockSide.getOpposite();
            }
        }
        return placement;
    }

    protected boolean postMovementEvents(World world, EntityPlayer player, BlockPos pos, BlockPos newPos, EnumHand hand)
    {
        int breakE = ForgeHooks.onBlockBreakEvent(world, ((EntityPlayerMP) player).interactionManager.getGameType(), (EntityPlayerMP) player, pos); // fire break on old pos
        BlockEvent.PlaceEvent place = new BlockEvent.PlaceEvent(BlockSnapshot.getBlockSnapshot(world, newPos), world.getBlockState(pos), player, hand);
        MinecraftForge.EVENT_BUS.post(place);
        return breakE != -1 && !place.isCanceled();
    }

    protected boolean canEdit(World world, IBlockState state, BlockPos pos, BlockPos newPos, EntityPlayer player, EnumFacing pushDirection, EnumHand hand)
    {
        //Normal edit checks
        boolean place = world.mayPlace(state.getBlock(), newPos, false, EnumFacing.UP, player);
        boolean edit1 = player.canPlayerEdit(newPos, EnumFacing.UP, new ItemStack(state.getBlock()));
        boolean edit2 = player.canPlayerEdit(pos, pushDirection, player.getHeldItem(hand));

        //Handler override
        final Handler handler = HandlerManager.INSTANCE.getHandler(state.getBlock());
        if (handler != null)
        {
            final EnumActionResult result = handler.canEdit(world, state, pos, newPos, player, pushDirection, hand, place, edit1, edit2);
            if (result != EnumActionResult.PASS)
            {
                return result == EnumActionResult.SUCCESS;
            }
        }


        return place && edit1 && edit2;
    }

    protected EnumActionResult tryToMoveBlock(EntityPlayer player, World world, BlockPos oldPos, BlockPos newPos, EnumFacing pushDirection, IBlockState oldState, IBlockState filledState)
    {
        final PistonMoveBlockEvent event = new PistonMoveBlockEvent(world, oldPos, oldState, pushDirection);
        if (MinecraftForge.EVENT_BUS.post(event))
        {
            return EnumActionResult.FAIL;
        }
        else if(event.getState() != event.newState)
        {
            //Kill old
            world.removeTileEntity(oldPos);
            world.setBlockToAir(oldPos);

            //Set new
            world.setBlockState(newPos, event.newState, 3);

            //Trigger updates
            this.updateEverything(world, oldPos, newPos, oldState, filledState);

            return EnumActionResult.SUCCESS;
        }

        //Check that we can pick up block
        final CanPushResult result = HandlerManager.INSTANCE.canPush(world, oldPos);
        if (result == CanPushResult.CAN_PUSH || result == CanPushResult.NO_TILE)
        {
            if (oldState.getPushReaction() == EnumPushReaction.DESTROY)
            {
                float chance = oldState.getBlock() instanceof BlockSnow ? -1.0f : 1.0f;
                oldState.getBlock().dropBlockAsItemWithChance(world, oldPos, oldState, chance, 0);
                world.setBlockToAir(oldPos);
                world.notifyNeighborsOfStateChange(oldPos, oldState.getBlock(), true);
            }
            else
            {
                final Handler handler = HandlerManager.INSTANCE.getHandler(oldState.getBlock());

                //Pre handling
                NBTTagCompound data = handler != null ? handler.preMoveBlock(player, world, oldPos, newPos) : null;

                //Copy tile data
                if (result != CanPushResult.NO_TILE)
                {
                    final TileEntity oldTile = world.getTileEntity(oldPos);
                    //Copy tile data
                    final NBTTagCompound compound = new NBTTagCompound();
                    oldTile.writeToNBT(compound);

                    //Remove location data
                    compound.removeTag("x");
                    compound.removeTag("y");
                    compound.removeTag("z");

                    //Kill old
                    world.removeTileEntity(oldPos);
                    world.setBlockToAir(oldPos);

                    //Place new
                    world.setBlockState(newPos, oldState);

                    //Create new tile
                    final TileEntity newTile = oldTile.create(world, compound);
                    if (newTile != null)
                    {
                        newTile.readFromNBT(compound);
                        newTile.setPos(newPos);
                        world.setTileEntity(newPos, newTile);
                        newTile.updateContainingBlockInfo();
                        if (newTile instanceof ITickable)
                        {
                            ((ITickable) newTile).update();
                        }
                    }
                }
                else
                {
                    world.setBlockToAir(oldPos);
                    world.setBlockState(newPos, oldState);
                }

                //Post handling
                if (handler != null)
                {
                    handler.postMoveBlock(player, world, oldPos, newPos, data != null ? data : new NBTTagCompound());
                }
            }

            //Trigger updates
            this.updateEverything(world, oldPos, newPos, oldState, filledState);

            return EnumActionResult.SUCCESS;
        }
        else if (result == CanPushResult.BANNED_TILE)
        {
            player.sendStatusMessage(new TextComponentTranslation("item." + HandheldPiston.MODID + ":banned.tile.name"), true);
        }
        else if (result == CanPushResult.BANNED_BLOCK)
        {
            player.sendStatusMessage(new TextComponentTranslation("item." + HandheldPiston.MODID + ":banned.block.name"), true);
        }
        return EnumActionResult.FAIL;
    }

    protected void updateEverything(World world, BlockPos pos, BlockPos newPos, IBlockState oldState, IBlockState filledState)
    {
        world.markBlockRangeForRenderUpdate(pos, pos);
        world.notifyBlockUpdate(pos, oldState, Blocks.AIR.getDefaultState(), 3);
        world.scheduleBlockUpdate(pos, Blocks.AIR, 0, 0);
        world.markBlockRangeForRenderUpdate(newPos, newPos);
        world.notifyBlockUpdate(newPos, filledState, oldState, 3);
        world.scheduleBlockUpdate(newPos, oldState.getBlock(), 0, 0);
        world.notifyNeighborsOfStateChange(pos, oldState.getBlock(), true);
        world.notifyNeighborsOfStateChange(newPos, oldState.getBlock(), true);
    }

    public static void setExtended(ItemStack stack, World world)
    {
        if (!stack.hasTagCompound())
        {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setLong(NBT_TICKS, world.getTotalWorldTime());
    }

    protected int getExtendedTime(ItemStack stack)
    {
        if (stack.getTagCompound() != null)
        {
            return stack.getTagCompound().getInteger(NBT_TICKS);
        }
        return 0;
    }

    public void handleMouseWheelAction(ItemStack stack, EntityPlayer player, boolean ctrl, boolean forward)
    {
        toggleMode(stack, forward);
        player.inventoryContainer.detectAndSendChanges();
        player.sendStatusMessage(new TextComponentTranslation("item." + HandheldPiston.MODID + ":mode." + getMode(stack).name().toLowerCase() + ".info"), true);
    }

    public void toggleMode(ItemStack stack, boolean forward)
    {
        if (modes.size() > 1)
        {
            final PistonMode currentMode = getMode(stack);
            PistonMode nextMode = currentMode;
            do
            {
                nextMode = forward ? nextMode.next() : nextMode.prev();
                if (modes.contains(nextMode))
                {
                    setMode(stack, nextMode);
                    return;
                }
            }
            while (nextMode != currentMode);
        }
    }

    public PistonMode getMode(ItemStack stack)
    {
        if (stack.getTagCompound() != null)
        {
            return PistonMode.get(stack.getTagCompound().getInteger(NBT_MODE));
        }
        return PistonMode.ALL;
    }

    public void setMode(ItemStack stack, PistonMode mode)
    {
        if (stack.getTagCompound() == null)
        {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setInteger(NBT_MODE, mode.ordinal());
    }

    @Override
    public void addInformation(ItemStack stack, World worldIn, java.util.List<String> lines, ITooltipFlag flagIn)
    {
        super.addInformation(stack, worldIn, lines, flagIn);
        lines.add(I18n.format("item." + HandheldPiston.MODID + ":mode." + getMode(stack).name().toLowerCase() + ".info"));
        if (GuiScreen.isShiftKeyDown())
        {
            lines.add(I18n.format("item." + HandheldPiston.MODID + ":info"));
            lines.add(I18n.format("item." + HandheldPiston.MODID + ":use.info"));
            lines.add(I18n.format("item." + HandheldPiston.MODID + ":wheel.info"));
        }
        else
        {
            lines.add(I18n.format("item." + HandheldPiston.MODID + ":more.info"));
        }
    }


}
