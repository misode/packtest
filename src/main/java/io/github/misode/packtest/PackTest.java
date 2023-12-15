package io.github.misode.packtest;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackTest implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("packtest");

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing");
	}
}