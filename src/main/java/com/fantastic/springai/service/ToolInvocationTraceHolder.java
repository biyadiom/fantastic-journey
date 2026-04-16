package com.fantastic.springai.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Liste des appels {@link DatabaseToolsService} exécutés pendant un tour de chat (même thread).
 */
public final class ToolInvocationTraceHolder {

    private static final ThreadLocal<List<String>> CALLS = ThreadLocal.withInitial(ArrayList::new);

    private ToolInvocationTraceHolder() {
    }

    public static void clear() {
        CALLS.remove();
    }

    public static void add(String line) {
        CALLS.get().add(line);
    }

    public static List<String> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(CALLS.get()));
    }
}
