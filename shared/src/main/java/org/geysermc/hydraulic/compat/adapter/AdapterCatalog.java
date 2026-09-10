package org.geysermc.hydraulic.compat.adapter;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a catalog of capability adapters available for mod-specific compatibility.
 * This separates built-in generic adapters from mod-specific adapters while maintaining
 * deterministic precedence rules.
 */
public final class AdapterCatalog {
    private final String fingerprint;
    private final Map<String, AdapterBinding> builtInAdapters;
    private final Map<String, AdapterBinding> modSpecificAdapters;
    private final boolean isBuiltinOnly;

    private AdapterCatalog(
        @NotNull String fingerprint,
        @NotNull Map<String, AdapterBinding> builtInAdapters,
        @NotNull Map<String, AdapterBinding> modSpecificAdapters,
        boolean isBuiltinOnly
    ) {
        this.fingerprint = fingerprint;
        this.builtInAdapters = Map.copyOf(builtInAdapters);
        this.modSpecificAdapters = Map.copyOf(modSpecificAdapters);
        this.isBuiltinOnly = isBuiltinOnly;
    }

    @NotNull
    public static AdapterCatalog builtinOnly() {
        return new AdapterCatalog(
            org.geysermc.hydraulic.util.PackUtil.adapterCatalogFingerprint(),
            Map.of(),
            Map.of(),
            true
        );
    }

    @NotNull
    public static AdapterCatalog from(
        @NotNull String fingerprint,
        @NotNull Map<String, AdapterBinding> builtInAdapters,
        @NotNull Map<String, AdapterBinding> modSpecificAdapters
    ) {
        return new AdapterCatalog(
            fingerprint,
            new LinkedHashMap<>(builtInAdapters),
            new LinkedHashMap<>(modSpecificAdapters),
            false
        );
    }

    @NotNull
    public String fingerprint() {
        return this.fingerprint;
    }

    @NotNull
    public Map<String, AdapterBinding> builtInAdapters() {
        return this.builtInAdapters;
    }

    @NotNull
    public Map<String, AdapterBinding> modSpecificAdapters() {
        return this.modSpecificAdapters;
    }

    public boolean isBuiltinOnly() {
        return this.isBuiltinOnly;
    }

    /**
     * Resolves the effective adapter binding for a given adapter key.
     * Mod-specific adapters take precedence over built-in adapters.
     */
    @NotNull
    public AdapterBinding resolve(@NotNull String adapterKey) {
        AdapterBinding modSpecific = this.modSpecificAdapters.get(adapterKey);
        if (modSpecific != null) {
            return modSpecific;
        }
        AdapterBinding builtIn = this.builtInAdapters.get(adapterKey);
        if (builtIn != null) {
            return builtIn;
        }
        return AdapterBinding.unsupported(adapterKey);
    }

    /**
     * Checks if this catalog contains any adapter for the given key.
     */
    public boolean hasAdapter(@NotNull String adapterKey) {
        return this.modSpecificAdapters.containsKey(adapterKey) || this.builtInAdapters.containsKey(adapterKey);
    }
}