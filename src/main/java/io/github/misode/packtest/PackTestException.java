package io.github.misode.packtest;

import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class PackTestException extends GameTestAssertException {
    final int line;

    public PackTestException(Component message, int tick, int line) {
        super(message, tick);
        this.line = line;
    }

    public int getLine() {
        return this.line;
    }

    @Override
    public @NonNull Component getDescription() {
        return Component.literal("On line " + this.line + ": ").append(super.getDescription());
    }
}
