package io.github.misode.packtest;

import java.util.ArrayList;
import java.util.List;

public class LoadDiagnostics {
    private static final List<Diagnostic> DIAGNOSTICS = new ArrayList<>();

    public static void error(String resource, String id, String message) {
        DIAGNOSTICS.add(new Diagnostic(resource, id, message));
    }

    public static List<Diagnostic> loadErrors() {
        return DIAGNOSTICS;
    }

    public record Diagnostic(String resource, String id, String message) {}
}
