package io.github.misode.packtest;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class LoadDiagnostics {
    private static final List<Diagnostic> DIAGNOSTICS = new ArrayList<>();

    public static void error(Logger logger, String type, String id, String message) {
        DIAGNOSTICS.add(new Diagnostic(type, id, message));
        if (PackTest.isAnnotationsEnabled()) {
            logger.info(PackTest.wrapError("Failed to load {} {}") + "\n::error title=Failed to load {} {}::{}", type, id, type, id, message);
        } else {
            logger.info(PackTest.wrapError("Failed to load {} {} - {}"), type, id, message);
        }
    }

    public static List<Diagnostic> loadErrors() {
        return DIAGNOSTICS;
    }

    public record Diagnostic(String resource, String id, String message) {}
}
