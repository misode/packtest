package io.github.misode.packtest;

import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.network.chat.Component;

public class LineNumberException extends GameTestAssertException {

    final int lineNumber;

    public LineNumberException(Component message, int tick, int lineNumber) {
        super(message, tick);
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }
}
