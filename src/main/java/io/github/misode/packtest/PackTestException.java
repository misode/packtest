package io.github.misode.packtest;

import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

public class PackTestException extends GameTestAssertException {
    final int line;
    final String command;

    public PackTestException(Component message, int tick, int line, String command) {
        super(message, tick);
        this.line = line;
        this.command = command;
    }

    public String getLine() {
        return this.command.isEmpty()
            ? " on line " + this.line
            : " \"" + this.command + "\" on line " + this.line;
    }

    @Override
    public @NonNull Component getDescription() {
        String prefix = this.command.isEmpty()
                ? "On line " + this.line + ": "
                : "\"" + this.command + "\" on line " + this.line + ": ";
        return Component.literal(prefix).append(super.getDescription());
    }
}