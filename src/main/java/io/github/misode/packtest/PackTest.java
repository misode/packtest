package io.github.misode.packtest;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackTest implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("packtest");

	@Override
	public void onInitialize() {
		PackTestRegistry.register(new ResourceLocation("packtest:example1"), (helper) -> {
			LOGGER.info("Example 1 is running!");
			helper.fail("Oh no");
		});
		PackTestRegistry.register(new ResourceLocation("packtest:example2"), (helper) -> {
			LOGGER.info("Example 2 is running!");
			helper.succeed();
		});
	}
}
