package com.builtbroken.handheldpiston.api;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Handles interaction between {@link com.builtbroken.cardboardboxes.box.ItemBlockBox} and tiles
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 7/28/2015.
 */
public class HandlerManager
{
    /** Map of tiles to handlers that provide special interaction */
    public static HashMap<Class<? extends TileEntity>, Handler> pickupHandlerMap = new HashMap();

    /** Map of block to handlers that provide special interaction */
    public static HashMap<Block, Handler> handlerMap = new HashMap();

    /** List of tiles that are banned */
    public static List<Class<? extends TileEntity>> tileEntityBanList = new ArrayList();

    /** List of Blocks that are banned */
    public static List<String> blockBanList = new ArrayList();

    /** Primary manager */
    public final static HandlerManager INSTANCE = new HandlerManager();

    /** If the banned block list is a whitelist **/
    public static boolean isWhitelist = false;


    /**
     * Called to register a handler for managing the pickup state of a tile
     *
     * @param clazz
     * @param handler
     */
    public void registerPickupHandler(Class<? extends TileEntity> clazz, Handler handler) //TODO implement
    {
        pickupHandlerMap.put(clazz, handler);
    }

    /**
     * Called to register an interaction handler to manage the overall state of a block
     *
     * @param block   - block to handle
     * @param handler - object to manage handler calls
     */
    public void registerHandler(Block block, Handler handler)
    {
        handlerMap.put(block, handler);
    }

    public Handler getHandler(Block block)
    {
        return handlerMap.get(block);
    }

    /**
     * Called to ban a tile
     *
     * @param clazz
     */
    public void banTile(Class<? extends TileEntity> clazz)
    {
        if (!tileEntityBanList.contains(clazz))
        {
            tileEntityBanList.add(clazz);
        }
    }

    /**
     * Called to ban a block
     *
     * @param block
     */
    public void banBlock(Block block)
    {
        if (!blockBanList.contains(block.getRegistryName().toString()))
        {
            this.blockBanList.add(block.getRegistryName().toString());
        }
    }

    /**
     * Called to check if a block can be picked up inside a box
     *
     * @param world - position
     * @param pos   - position
     * @return result of the interaction
     */
    public CanPushResult canPush(World world, BlockPos pos)
    {
        Block block = world.getBlockState(pos).getBlock();
        if (!blockBanList.contains(block.getRegistryName().toString()) || (isWhitelist && blockBanList.contains(block.getRegistryName().toString())))
        {
            TileEntity tile = world.getTileEntity(pos);
            if (tile != null)
            {
                if (!tileEntityBanList.contains(tile.getClass()))
                {
                    return CanPushResult.CAN_PUSH;
                }
                return CanPushResult.BANNED_TILE;
            }
            return CanPushResult.NO_TILE;
        }
        return CanPushResult.BANNED_BLOCK;
    }

}