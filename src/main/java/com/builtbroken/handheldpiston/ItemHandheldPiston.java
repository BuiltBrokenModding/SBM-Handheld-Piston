package com.builtbroken.handheldpiston;

import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
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
	}



	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
			EnumFacing facing, float hitX, float hitY, float hitZ) {
		BlockPos newPos = pos.offset(facing.getOpposite());
		if(newPos.getY() < world.getHeight() && newPos.getY() > 0) {

			IBlockState oldState = world.getBlockState(pos);
			IBlockState newState = world.getBlockState(newPos);
			boolean pushable = (oldState.getPushReaction() == EnumPushReaction.NORMAL || oldState.getPushReaction() == EnumPushReaction.PUSH_ONLY);
			boolean hardness = oldState.getBlockHardness(world, pos) != -1.0F;
			boolean replaceable = (newState.getBlock() == Blocks.AIR || newState.getBlock().isReplaceable(world, newPos));
			boolean extras = oldState.getBlock() != Blocks.OBSIDIAN && world.getWorldBorder().contains(pos);
			if(((pushable && hardness && replaceable) || oldState.getPushReaction() == EnumPushReaction.DESTROY) && extras) {
				if(!world.isRemote) {
					int breakE = ForgeHooks.onBlockBreakEvent(world, ((EntityPlayerMP) player).interactionManager.getGameType(), (EntityPlayerMP) player, pos); // fire break on old pos
					BlockEvent.PlaceEvent place = new BlockEvent.PlaceEvent(BlockSnapshot.getBlockSnapshot(world, newPos), world.getBlockState(pos), player, hand);
					MinecraftForge.EVENT_BUS.post(place);
					if(breakE != -1 && !place.isCanceled()) {
						HandheldPistonMoveEvent event = new HandheldPistonMoveEvent(HandheldPistonMoveEvent.PistonMoveType.BLOCK, pos, newPos, null);
						MinecraftForge.EVENT_BUS.post(event);
						if(!event.isCanceled()) {
							world.playSound((EntityPlayer)null, pos, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 0.5F, world.rand.nextFloat() * 0.25F + 0.6F);
							if(oldState.getPushReaction() == EnumPushReaction.DESTROY) {
								world.setBlockToAir(pos);
							} else {
								TileEntity te = world.getTileEntity(pos);
								world.setBlockState(newPos, oldState);
								if(te != null) {
									world.setTileEntity(newPos, te);
									te.setPos(newPos);
									te.markDirty();
								}
								world.setBlockToAir(pos);
								world.removeTileEntity(pos);
							}
							world.markBlockRangeForRenderUpdate(pos, pos);
							world.notifyBlockUpdate(pos, oldState, Blocks.AIR.getDefaultState(), 3);
							world.scheduleBlockUpdate(pos, Blocks.AIR, 0, 0);
							world.markBlockRangeForRenderUpdate(newPos, newPos);
							world.notifyBlockUpdate(newPos, newState, oldState, 3);
							world.scheduleBlockUpdate(newPos, oldState.getBlock(), 0, 0);

						}
					}
				}
			} else {
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
				if(!player.onGround) {
					velY /= 2;
				}
				player.addVelocity(velX, velY, velZ);
			}
		}
		return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
	}

}
