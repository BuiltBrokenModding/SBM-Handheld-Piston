package com.builtbroken.handheldpiston.item;

import com.builtbroken.handheldpiston.HandheldPiston;
import com.builtbroken.handheldpiston.api.CanPushResult;
import com.builtbroken.handheldpiston.api.HandheldPistonMoveEvent;
import com.builtbroken.handheldpiston.api.Handler;
import com.builtbroken.handheldpiston.api.HandlerManager;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import javax.vecmath.Vector3d;
import java.util.HashSet;
import java.util.Set;


public class ItemHandheldPiston extends Item
{
    public static final String NBT_MODE = "toolMode";
    public static final String NBT_TICKS = "extendTick";

    public final Set<PistonMode> modes = new HashSet();

    public ItemHandheldPiston(ResourceLocation registryName, PistonMode... modes)
    {
        this.setRegistryName(registryName);
        this.setTranslationKey(HandheldPiston.MODID + "." + registryName);
        this.setCreativeTab(CreativeTabs.TOOLS);
        this.setMaxStackSize(1);

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

        final ItemStack stack = player.getHeldItem(hand);
        final PistonMode mode = getMode(stack);
        EnumFacing facing = sideHit;
        if (mode == PistonMode.ADVANCED)
        {
            facing = getPlacement(sideHit, hitX, hitY, hitZ);
        }

        final BlockPos newPos = pos.offset(facing.getOpposite());
        final IBlockState oldState = world.getBlockState(pos);
        final IBlockState filledState = world.getBlockState(newPos);


        //Fail out if we are already extended
        if (world.getTotalWorldTime() - getExtendedTime(stack) < 15) //TODO make config driven
        {
            return EnumActionResult.FAIL;
        }

        //Check if we can push
        if (this.canTryPush(world, oldState, filledState, pos, newPos, hand, player, facing))
        {
            //Do push
            return this.tryToMoveBlock(player, world, pos, newPos, hand, facing, oldState, filledState);
        }
        return EnumActionResult.SUCCESS;
    }

    protected boolean canTryPush(World world, IBlockState oldState, IBlockState filledState, BlockPos pos, BlockPos newPos, EnumHand hand, EntityPlayer player, EnumFacing facing)
    {
        final ItemStack stack = player.getHeldItem(hand);
        final PistonMode mode = getMode(stack);
        if (newPos.getY() < world.getHeight() && newPos.getY() > 0)
        {
            boolean pushable = (oldState.getPushReaction() == EnumPushReaction.NORMAL || oldState.getPushReaction() == EnumPushReaction.PUSH_ONLY);
            boolean hardness = oldState.getBlockHardness(world, pos) != -1.0F;
            boolean replaceable = (filledState.getBlock() == Blocks.AIR || filledState.getBlock().isReplaceable(world, newPos));
            boolean extras = oldState.getBlock() != Blocks.OBSIDIAN && world.getWorldBorder().contains(pos);
            if (((pushable && hardness && replaceable) || oldState.getPushReaction() == EnumPushReaction.DESTROY) && extras && mode.canPushBlocks)
            {
                if (!world.isRemote)
                {
                    boolean eventsPass = postMovementEvents(world, player, pos, newPos, hand);
                    boolean canEdit = canEdit(world, oldState, pos, newPos, player, facing, hand);
                    if (eventsPass && canEdit)
                    {
                        HandheldPistonMoveEvent event = new HandheldPistonMoveEvent(HandheldPistonMoveEvent.PistonMoveType.BLOCK, pos, newPos, null);
                        MinecraftForge.EVENT_BUS.post(event);
                        if (!event.isCanceled())
                        {
                            return true;
                        }
                    }
                }
            }
            else
            {
                //Movement
                final Vector3d vector = EntityEvent.getVelocityForPush(facing, player, stack, mode);
                player.addVelocity(vector.getX(), vector.getY(), vector.getZ());

                //audio
                world.playSound((EntityPlayer) null, pos, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.5F, world.rand.nextFloat() * 0.25F + 0.6F);

                //animation
                this.setExtended(stack, world);
            }

        }
        return false;
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

    protected boolean canEdit(World world, IBlockState state, BlockPos pos, BlockPos newPos, EntityPlayer player, EnumFacing facing, EnumHand hand)
    {
        //Normal edit checks
        boolean place = world.mayPlace(state.getBlock(), newPos, false, EnumFacing.UP, player);
        boolean edit1 = player.canPlayerEdit(newPos, EnumFacing.UP, new ItemStack(state.getBlock()));
        boolean edit2 = player.canPlayerEdit(pos, facing, player.getHeldItem(hand));

        //Handler override
        final Handler handler = HandlerManager.INSTANCE.getHandler(state.getBlock());
        if (handler != null)
        {
            final EnumActionResult result = handler.canEdit(world, state, pos, newPos, player, facing, hand, place, edit1, edit2);
            if (result != EnumActionResult.PASS)
            {
                return result == EnumActionResult.SUCCESS;
            }
        }


        return place && edit1 && edit2;
    }

    protected EnumActionResult tryToMoveBlock(EntityPlayer player, World world, BlockPos pos, BlockPos newPos, EnumHand hand, EnumFacing facing, IBlockState oldState, IBlockState filledState)
    {
        //Check that we can pick up block
        final CanPushResult result = HandlerManager.INSTANCE.canPush(world, pos);
        if (result == CanPushResult.CAN_PUSH || result == CanPushResult.NO_TILE)
        {
            if (oldState.getPushReaction() == EnumPushReaction.DESTROY)
            {
                float chance = oldState.getBlock() instanceof BlockSnow ? -1.0f : 1.0f;
                oldState.getBlock().dropBlockAsItemWithChance(world, pos, oldState, chance, 0);
                world.setBlockToAir(pos);
                world.notifyNeighborsOfStateChange(pos, oldState.getBlock(), true);
            }
            else
            {
                final Handler handler = HandlerManager.INSTANCE.getHandler(oldState.getBlock());

                //Pre handling
                NBTTagCompound data = handler != null ? handler.preMoveBlock(player, world, pos, newPos) : null;

                //Copy tile data
                if (result != CanPushResult.NO_TILE)
                {
                    final TileEntity oldTile = world.getTileEntity(pos);
                    //Copy tile data
                    final NBTTagCompound compound = new NBTTagCompound();
                    oldTile.writeToNBT(compound);

                    //Remove location data
                    compound.removeTag("x");
                    compound.removeTag("y");
                    compound.removeTag("z");

                    //Kill old
                    world.removeTileEntity(pos);
                    world.setBlockToAir(pos);

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
                    world.setBlockToAir(pos);
                    world.setBlockState(newPos, oldState);
                }

                //Post handling
                if (handler != null)
                {
                    handler.postMoveBlock(player, world, pos, newPos, data != null ? data : new NBTTagCompound());
                }
            }

            //Trigger updates
            this.updateEverything(world, pos, newPos, oldState, filledState);

            //Animation
            this.setExtended(player.getHeldItem(hand), world);
            world.playSound((EntityPlayer) null, pos, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.5F, world.rand.nextFloat() * 0.25F + 0.6F);

            return EnumActionResult.SUCCESS;
        }
        else if (result == CanPushResult.BANNED_TILE)
        {
            player.sendStatusMessage(new TextComponentTranslation(this.getTranslationKey() + ".banned.tile.name"), true);
        }
        else if (result == CanPushResult.BANNED_BLOCK)
        {
            player.sendStatusMessage(new TextComponentTranslation(this.getTranslationKey() + ".banned.block.name"), true);
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
        player.sendStatusMessage(new TextComponentTranslation(getTranslationKey() + ".mode." + getMode(stack).name().toLowerCase() + ".info"), true);
    }

    public void toggleMode(ItemStack stack, boolean forward)
    {
        if (modes.size() > 1)
        {
            final PistonMode currentMode = getMode(stack);
            PistonMode nextMode = null;

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
        lines.add(I18n.format(getTranslationKey() + ".mode." + getMode(stack).name().toLowerCase() + ".info"));
        if (GuiScreen.isShiftKeyDown())
        {
            lines.add(I18n.format(getTranslationKey() + ".info"));
            lines.add(I18n.format(getTranslationKey() + ".use.info"));
            lines.add(I18n.format(getTranslationKey() + ".wheel.info"));
        }
        else
        {
            lines.add(I18n.format(getTranslationKey() + ".more.info"));
        }
    }


}
