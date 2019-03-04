package com.builtbroken.handheldpiston;

import javax.vecmath.Vector3d;

import com.builtbroken.handheldpiston.api.CanPushResult;
import com.builtbroken.handheldpiston.api.HandheldPistonMoveEvent;
import com.builtbroken.handheldpiston.api.HandheldPistonMoveEvent.PistonMoveType;
import com.builtbroken.handheldpiston.api.Handler;
import com.builtbroken.handheldpiston.api.HandlerManager;
import com.builtbroken.handheldpiston.client.HandheldPistonRenderer;

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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.world.BlockEvent;


public class ItemHandheldPiston extends Item {

	public ItemHandheldPiston(String registryName) {
		this.setRegistryName(registryName);
		this.setTranslationKey(HandheldPistonMod.MODID + "." + registryName);
		this.setCreativeTab(CreativeTabs.TOOLS);
		this.setTileEntityItemStackRenderer(new HandheldPistonRenderer());
	}



	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
			EnumFacing facing, float hitX, float hitY, float hitZ) {
		BlockPos newPos = pos.offset(facing.getOpposite());
		IBlockState oldState = world.getBlockState(pos);
		IBlockState filledState = world.getBlockState(newPos);
		ItemStack stack = player.getHeldItem(hand);
		if(!stack.hasTagCompound()) {
			stack.setTagCompound(new NBTTagCompound());
		}
		if(world.getTotalWorldTime() - stack.getTagCompound().getInteger("extendTick") < 15) {
			return EnumActionResult.FAIL;
		}
		if(this.canTryPush(world, oldState, filledState, pos, newPos, hand, player, facing)) {
			return this.tryToMoveBlock(player, world, pos, newPos, hand, facing, oldState, filledState);
		}
		return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
	}

	protected boolean canTryPush(World world, IBlockState oldState, IBlockState filledState, BlockPos pos, BlockPos newPos, EnumHand hand, EntityPlayer player, EnumFacing facing) {
		ItemStack stack = player.getHeldItem(hand);
		if(newPos.getY() < world.getHeight() && newPos.getY() > 0) {
			boolean pushable = (oldState.getPushReaction() == EnumPushReaction.NORMAL || oldState.getPushReaction() == EnumPushReaction.PUSH_ONLY);
			boolean hardness = oldState.getBlockHardness(world, pos) != -1.0F;
			boolean replaceable = (filledState.getBlock() == Blocks.AIR || filledState.getBlock().isReplaceable(world, newPos));
			boolean extras = oldState.getBlock() != Blocks.OBSIDIAN && world.getWorldBorder().contains(pos);
			if(((pushable && hardness && replaceable) || oldState.getPushReaction() == EnumPushReaction.DESTROY) && extras && HandheldPistonMod.piston.getMode(stack) == PistonMode.ALL) {
				if(!world.isRemote) {
					boolean eventsPass = postMovementEvents(world, player, pos, newPos, hand);
					boolean canEdit = canEdit(world, oldState, pos, newPos, player, facing, hand);
					if(eventsPass && canEdit) {
						HandheldPistonMoveEvent event = new HandheldPistonMoveEvent(HandheldPistonMoveEvent.PistonMoveType.BLOCK, pos, newPos, null);
						MinecraftForge.EVENT_BUS.post(event);
						if(!event.isCanceled()) {
							return true;
						}
					}

				} else {

				}
			} else {
				Vector3d v = getVelocityForPush(facing, player, stack);
				player.addVelocity(v.getX(), v.getY(), v.getZ());
				world.playSound((EntityPlayer)null, pos, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.5F, world.rand.nextFloat() * 0.25F + 0.6F);
				this.setExtended(stack, world);
			}

		}
		return false;
	}

	protected boolean postMovementEvents(World world, EntityPlayer player, BlockPos pos, BlockPos newPos, EnumHand hand) {
		int breakE = ForgeHooks.onBlockBreakEvent(world, ((EntityPlayerMP) player).interactionManager.getGameType(), (EntityPlayerMP) player, pos); // fire break on old pos
		BlockEvent.PlaceEvent place = new BlockEvent.PlaceEvent(BlockSnapshot.getBlockSnapshot(world, newPos), world.getBlockState(pos), player, hand);
		MinecraftForge.EVENT_BUS.post(place);
		return breakE != -1 && !place.isCanceled();
	}

	protected boolean canEdit(World world, IBlockState state, BlockPos pos, BlockPos newPos, EntityPlayer player, EnumFacing facing, EnumHand hand) {
		return world.mayPlace(state.getBlock(), newPos, false, EnumFacing.UP, player) && 
				player.canPlayerEdit(newPos, EnumFacing.UP, new ItemStack(state.getBlock())) && 
				player.canPlayerEdit(pos, facing, player.getHeldItem(hand));
	}

	public static Vector3d getVelocityForPush(EnumFacing facing, EntityLivingBase entity, ItemStack stack) {
		double velX = 0.5;
		double velY = 0.5;
		double velZ = 0.5;
		if(facing == EnumFacing.UP) {
			velX = 0;
			velY = Math.abs(velY);
			velZ = 0;
		}
		if(facing == EnumFacing.DOWN) {
			velX = 0;
			velY = -Math.abs(velY);
			velZ = 0;
		}
		if(facing == EnumFacing.NORTH) {
			velX = 0;
			velY = 0;
			velZ = -Math.abs(velZ);
		}
		if(facing == EnumFacing.EAST) {
			velX = Math.abs(velX);
			velY = 0;
			velZ = 0;
		}
		if(facing == EnumFacing.SOUTH) {
			velX = 0;
			velY = 0;
			velZ = Math.abs(velZ);
		}
		if(facing == EnumFacing.WEST) {
			velX = -Math.abs(velX);
			velY = 0;
			velZ = 0;
		}
		if(!entity.onGround) {
			velY /= 2;
		}
		if(HandheldPistonMod.piston.getMode(stack) == PistonMode.SELF && entity instanceof EntityPlayer) {
			velX *= 1.5F;
			velY *= 2.5F;
			velZ *= 1.5F;
		}
		HandheldPistonMoveEvent event = new HandheldPistonMoveEvent(entity instanceof EntityPlayer ? PistonMoveType.PLAYER : PistonMoveType.ENTITY, null, null, new Vector3d(velX, velY, velZ));
		MinecraftForge.EVENT_BUS.post(event);
		if(!event.isCanceled()) {
			return event.velocityAdded;
		}
		return new Vector3d(0,0,0);
	}


	protected EnumActionResult tryToMoveBlock(EntityPlayer player, World world, BlockPos pos, BlockPos newPos, EnumHand hand, EnumFacing facing, IBlockState oldState, IBlockState filledState) {
		//Check that we can pick up block
		CanPushResult result = HandlerManager.INSTANCE.canPush(world, pos);
		if (result == CanPushResult.CAN_PUSH || result == CanPushResult.NO_TILE) {

			Handler handler = HandlerManager.INSTANCE.getHandler(oldState.getBlock());

			if(oldState.getPushReaction() == EnumPushReaction.DESTROY) {
				float chance = oldState.getBlock() instanceof BlockSnow ? -1.0f : 1.0f;
				oldState.getBlock().dropBlockAsItemWithChance(world, pos, oldState, chance, 0);
				world.setBlockToAir(pos);
				world.notifyNeighborsOfStateChange(pos, oldState.getBlock(), true);
			} else {
				NBTTagCompound data = new NBTTagCompound();
				if(handler != null) {
					data = handler.preMoveBlock(player, world, pos, newPos);
				}
				world.setBlockState(newPos, oldState);
				if(result != CanPushResult.NO_TILE) {
					TileEntity te = world.getTileEntity(pos);
					//Copy tile data
					NBTTagCompound compound = new NBTTagCompound();
					te.writeToNBT(compound);

					//Remove location data
					compound.removeTag("x");
					compound.removeTag("y");
					compound.removeTag("z");

					world.removeTileEntity(pos);

					TileEntity te2 = te.create(world, compound);
					if(te2 != null) {
						te2.readFromNBT(compound);
						te2.setPos(newPos);
						world.setTileEntity(newPos, te2);
						te2.updateContainingBlockInfo();
						if(te2 instanceof ITickable) {
							((ITickable) te2).update();
						}
					}
				}
				world.setBlockToAir(pos);
				if (handler != null) {
					handler.postMoveBlock(player, world, pos, newPos, data);
				}
			}
			this.setExtended(player.getHeldItem(hand), world);
			world.playSound((EntityPlayer)null, pos, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.5F, world.rand.nextFloat() * 0.25F + 0.6F);
			this.updateEverything(world, pos, newPos, oldState, filledState);
			return EnumActionResult.SUCCESS;
		} else if (result == CanPushResult.BANNED_TILE) {
			player.sendStatusMessage(new TextComponentTranslation(this.getTranslationKey() + ".banned.tile.name"), true);
		} else if (result == CanPushResult.BANNED_BLOCK) {
			player.sendStatusMessage(new TextComponentTranslation(this.getTranslationKey() + ".banned.block.name"), true);
		}
		return EnumActionResult.FAIL;
	}

	protected void updateEverything(World world, BlockPos pos, BlockPos newPos, IBlockState oldState, IBlockState filledState) {
		world.markBlockRangeForRenderUpdate(pos, pos);
		world.notifyBlockUpdate(pos, oldState, Blocks.AIR.getDefaultState(), 3);
		world.scheduleBlockUpdate(pos, Blocks.AIR, 0, 0);
		world.markBlockRangeForRenderUpdate(newPos, newPos);
		world.notifyBlockUpdate(newPos, filledState, oldState, 3);
		world.scheduleBlockUpdate(newPos, oldState.getBlock(), 0, 0);
		world.notifyNeighborsOfStateChange(pos, oldState.getBlock(), true);
		world.notifyNeighborsOfStateChange(newPos, oldState.getBlock(), true);
	}

	public static void setExtended(ItemStack stack, World world) {
		if(!stack.hasTagCompound()) {
			stack.setTagCompound(new NBTTagCompound());
		}
		stack.getTagCompound().setLong("extendTick", world.getTotalWorldTime());
	}

	public static final String NBT_MODE = "toolMode";

	public void handleMouseWheelAction(ItemStack stack, EntityPlayer player, boolean ctrl, boolean forward)
	{
		toggleMode(stack, forward);
		player.inventoryContainer.detectAndSendChanges();
		player.sendStatusMessage(new TextComponentTranslation(getTranslationKey() + ".mode." + getMode(stack).name().toLowerCase() + ".info"), true);
	}

	public void toggleMode(ItemStack stack, boolean forward)
	{
		setMode(stack, forward ? getMode(stack).next() : getMode(stack).prev());
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
	public void addInformation(ItemStack stack, World worldIn, java.util.List<String> lines, ITooltipFlag flagIn) {
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
