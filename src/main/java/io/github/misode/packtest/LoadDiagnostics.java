package io.github.misode.packtest;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class LoadDiagnostics {
    private static final List<Diagnostic> DIAGNOSTICS = new ArrayList<>();

    public static void error(Logger logger, String resource, String id, String message) {
        DIAGNOSTICS.add(new Diagnostic(resource, id, message));
        String annotation = "";
        if (PackTest.isAnnotationsEnabled()) {
            annotation = "\n::error title=Failed to load " + resource + " " + id + "::" + message;
        }
        logger.info(PackTest.wrapError("Failed to load {} {}" + (PackTest.isAnnotationsEnabled() ? "" : " - {}")) + annotation, resource, id, message);
    }

    public static List<Diagnostic> loadErrors() {
        return DIAGNOSTICS;
    }

    public record Diagnostic(String resource, String id, String message) {}
}
