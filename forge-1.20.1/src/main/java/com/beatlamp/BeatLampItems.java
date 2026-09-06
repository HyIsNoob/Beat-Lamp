package com.beatlamp;

import com.beatlamp.item.GroupLinkerItem;
import com.beatlamp.item.LampControllerItem;
import com.beatlamp.item.StageBlockItem;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public class BeatLampItems {
	public static StageBlockItem BEAT_LAMP;
	public static StageBlockItem BEAT_EMITTER;
	public static StageBlockItem STAGE_LIGHT;
	public static StageBlockItem FOUNTAIN;
	public static StageBlockItem LASER_PROJECTOR;
	public static StageBlockItem FOG_GENERATOR;
	public static StageBlockItem STAGE_JUKEBOX;
	public static StageBlockItem DMX_CONSOLE;
	public static StageBlockItem DJ_DECK;
	public static StageBlockItem STAGE_SPEAKER;
	public static GroupLinkerItem LINKER;
	public static LampControllerItem CONTROLLER;
	public static CreativeModeTab TAB;

	public static GroupLinkerItem createLinkerItem() {
		return new GroupLinkerItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
	}

	public static LampControllerItem createControllerItem() {
		return new LampControllerItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
	}
}
