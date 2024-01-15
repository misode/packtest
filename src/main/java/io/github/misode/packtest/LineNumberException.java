package io.github.misode.packtest;

import net.minecraft.gametest.framework.GameTestAssertException;

public class LineNumberException extends GameTestAssertException {

    final int lineNumber;

    public LineNumberException(String string, int lineNumber) {
        super(string);
        this.lineNumber = lineNumber;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }
}
