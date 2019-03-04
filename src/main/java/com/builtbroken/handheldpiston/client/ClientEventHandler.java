package com.builtbroken.handheldpiston.client;

import org.lwjgl.input.Keyboard;

import com.builtbroken.handheldpiston.HandheldPistonMod;
import com.builtbroken.handheldpiston.network.MousePacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = HandheldPistonMod.MODID, value = Side.CLIENT)
public class ClientEventHandler {

	public static final ModelResourceLocation mrl_piston = new ModelResourceLocation(HandheldPistonMod.piston.getRegistryName(), "inventory");
	public static final ModelResourceLocation mrl_piston_extended = new ModelResourceLocation(HandheldPistonMod.piston.getRegistryName() + "_extended", "inventory");

	public static IBakedModel piston = null;
	public static IBakedModel piston_extended = null;

	@SubscribeEvent
	public static void modelRegister(ModelRegistryEvent event) {
		HandheldPistonMod.piston.setTileEntityItemStackRenderer(new HandheldPistonRenderer());
		ModelLoader.setCustomModelResourceLocation(HandheldPistonMod.piston, 0, mrl_piston);
		ModelBakery.registerItemVariants(HandheldPistonMod.piston, mrl_piston_extended);
	}

	@SubscribeEvent
	public static void onModelBake(ModelBakeEvent event) {
		piston = event.getModelRegistry().getObject(mrl_piston);
		event.getModelRegistry().putObject(mrl_piston, new PistonModel(piston));
		piston_extended = event.getModelRegistry().getObject(mrl_piston_extended);
	}

	@SubscribeEvent
	public static void mouseEvent(MouseEvent e) {
		EntityPlayer player = Minecraft.getMinecraft().player;
		
		for(EnumHand hand : EnumHand.values()) {
			ItemStack stack = player.getHeldItem(hand);
			if (stack.getItem() == HandheldPistonMod.piston) {
				if (player.isSneaking() && e.getDwheel() != 0) {
					HandheldPistonMod.INSTANCE.sendToServer(new MousePacket(player.inventory.currentItem, e.getDwheel() > 0));
					e.setCanceled(true);
				}
				break;
			}
		}
	}

}
