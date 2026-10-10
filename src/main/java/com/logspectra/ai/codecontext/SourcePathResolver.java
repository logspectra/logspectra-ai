package com.logspectra.ai.codecontext;

import com.logspectra.ai.model.StackFrame;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves candidate relative source paths from stack frames / logger FQCNs.
 * Shared by local filesystem and remote (GitHub) context providers.
 */
public final class SourcePathResolver {

    private SourcePathResolver() {}

    public static List<String> candidatePaths(StackFrame frame, List<String> sourceRoots) {
        Set<String> paths = new LinkedHashSet<>();
        if (frame == null) {
            return List.of();
        }
        String fileName = frame.getFileName();
        if (fileName == null || fileName.isBlank()) {
            String simple = simpleName(frame.getClassName());
            if (simple != null && !simple.isBlank()) {
                fileName = simple + ".java";
            }
        }
        if (fileName == null || fileName.isBlank()) {
            return List.of();
        }

        String packagePath = frame.packagePrefix() == null ? "" : frame.packagePrefix().replace('.', '/');
        List<String> roots = (sourceRoots == null || sourceRoots.isEmpty())
            ? List.of("src/main/java", "src/main/kotlin")
            : sourceRoots;

        for (String sourceRoot : roots) {
            String base = trimSlashes(sourceRoot);
            if (packagePath.isBlank()) {
                paths.add(base + "/" + fileName);
            } else {
                paths.add(base + "/" + packagePath + "/" + fileName);
            }
        }
        return new ArrayList<>(paths);
    }

    public static String simpleName(String fqcn) {
        if (fqcn == null || fqcn.isBlank()) {
            return "";
        }
        int idx = fqcn.lastIndexOf('.');
        return idx >= 0 ? fqcn.substring(idx + 1) : fqcn;
    }

    private static String trimSlashes(String value) {
        String v = value.trim();
        while (v.startsWith("/")) {
            v = v.substring(1);
        }
        while (v.endsWith("/")) {
            v = v.substring(0, v.length() - 1);
        }
        return v;
    }
}
