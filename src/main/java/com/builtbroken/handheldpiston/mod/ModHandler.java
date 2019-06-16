package com.builtbroken.handheldpiston.mod;

import com.builtbroken.handheldpiston.api.HandlerManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.RegistryNamespaced;

import java.lang.reflect.Field;
import java.util.HashMap;

import static com.builtbroken.handheldpiston.HandheldPiston.LOGGER;

/**
 * Prefab for handling interaction for a mod or content package
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 7/28/2015.
 */
public class ModHandler
{
    public static HashMap<String, LazyModLoader> modSupportHandlerMap = new HashMap();

    protected static RegistryNamespaced<ResourceLocation, Class<? extends TileEntity>> TILE_REGISTRY;

    /**
     * Helper method to ban TileEntity.class by registry name
     *
     * @param names - registry name of the tile
     */
    protected void banTileNames(ResourceLocation... names)
    {
        if (TILE_REGISTRY != null)
        {
            for (ResourceLocation name : names)
            {
                if (TILE_REGISTRY.containsKey(name))
                {
                    HandlerManager.INSTANCE.banTile(TILE_REGISTRY.getObject(name));
                }
                else
                {
                    LOGGER.error("\tFailed to locate tile by name " + name + ". This is most likely a mod version issue report this error to mod author so it can be updated");
                }
            }
        }
        else
        {
            LOGGER.error("Attempted to ban tiles but TILE_REGISTRY was null. This could cause issues with interaction, check log above for possible cause.");
        }
    }

    /**
     * Called to load and process handlers
     *
     */
    public static void loadHandlerData()
    {
        loadTileRegistry();
        modSupportHandlerMap.values().forEach(lazy -> lazy.get().load());
    }

    public void load()
    {

    }

    private static void loadTileRegistry()
    {
        try
        {
            Field field;
            try
            {
                field = TileEntity.class.getDeclaredField("REGISTRY");
            }
            catch (NoSuchFieldException e)
            {
                field = TileEntity.class.getDeclaredField("field_190562_f");
            }
            field.setAccessible(true);
            TILE_REGISTRY = (RegistryNamespaced) field.get(null);
        }
        catch (NoSuchFieldException e)
        {
            LOGGER.error("ModHandler#loadHandlerData() -> Failed to find the tile registry field. Dumping fields in the clazz, report this error with fields.", e);
            int index = 0;
            for (Field field : TileEntity.class.getDeclaredFields())
            {
                LOGGER.error("\t\tField[" + (index++) + "] -> Name: " + field.getName() + "   Type: " + field.getType());
            }
        }
        catch (IllegalAccessException e)
        {
            LOGGER.error("ModHandler#loadHandlerData() -> Failed to access tile registry\"", e);
        }
        catch (Exception e)
        {
            LOGGER.error("ModHandler#loadHandlerData() -> Unexpected exception while attempting to access tile registry", e);
        }
    }
}
