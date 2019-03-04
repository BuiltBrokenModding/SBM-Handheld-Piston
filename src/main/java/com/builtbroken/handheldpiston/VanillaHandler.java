package com.builtbroken.handheldpiston;

import com.builtbroken.handheldpiston.api.Handler;
import com.builtbroken.handheldpiston.api.HandlerManager;

import net.minecraft.block.BlockJukebox;
import net.minecraft.block.BlockJukebox.TileEntityJukebox;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;
import scala.actors.threadpool.Arrays;

public class VanillaHandler extends ModHandler {

	@Override
	public void load(Configuration configuration) {
		HandlerManager.INSTANCE.registerHandler(Blocks.JUKEBOX, new Handler() {
			public NBTTagCompound preMoveBlock(EntityPlayer player, World world, BlockPos oldPos, BlockPos newPos) {
				TileEntity te = world.getTileEntity(oldPos);
				if(te != null && te instanceof TileEntityJukebox) {
					TileEntityJukebox juke = (TileEntityJukebox) te;
					ItemStack itemstack = juke.getRecord();

					if (!itemstack.isEmpty()) {
						world.playEvent(1010, oldPos, 0);
						world.playRecord(oldPos, (SoundEvent)null);
						juke.setRecord(ItemStack.EMPTY);
						world.setBlockState(oldPos, world.getBlockState(oldPos).withProperty(BlockJukebox.HAS_RECORD, Boolean.valueOf(false)));
						return itemstack.writeToNBT(new NBTTagCompound());
					}
				}
				return super.preMoveBlock(player, world, oldPos, newPos);
			}

			public void postMoveBlock(EntityPlayer player, World world, BlockPos oldPos, BlockPos newPos, NBTTagCompound data) {
				((BlockJukebox) Blocks.JUKEBOX).insertRecord(world, newPos, world.getBlockState(newPos), new ItemStack(data));
			}
		});
		
    	HandlerManager.INSTANCE.blockBanList.addAll(Arrays.asList(configuration.getStringList("block_ban_list", "block_ban_list", new String[] {"minecraft:example_block", "minecraft:example_block2"}, "Default blacklist. Config provided to switch whitelist.")));
    	HandlerManager.INSTANCE.isWhitelist = configuration.getBoolean("is_whitelist", "block_ban_list", false, "Set to true to use block_ban_list as a whitelist.");
		
	}

}
