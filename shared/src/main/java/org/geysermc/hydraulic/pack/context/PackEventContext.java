package org.geysermc.hydraulic.pack.context;

import org.geysermc.event.Event;
import org.geysermc.hydraulic.HydraulicImpl;
import org.geysermc.hydraulic.pack.PackModule;
import org.geysermc.hydraulic.platform.mod.ModInfo;
import org.geysermc.pack.converter.type.model.ModelStitcher;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the context of a pack for an event.
 *
 * @param <T> the module type
 */
public class PackEventContext<E extends Event, T extends PackModule<T>> extends PackContext<T> {
    private final E event;
    private final ModelStitcher.Provider modelProvider;

    public PackEventContext(
        @NotNull HydraulicImpl hydraulic,
        @NotNull ModInfo mod,
        @NotNull T module,
        @NotNull E event,
        @NotNull ModelStitcher.Provider modelProvider
    ) {
        super(hydraulic, mod, module);

        this.event = event;
        this.modelProvider = modelProvider;
    }

    /**
     * Gets the event that this context is part of.
     *
     * @return the event that this context is part of
     */
    @NotNull
    public E event() {
        return this.event;
    }

    @NotNull
    public ModelStitcher.Provider modelProvider() {
        return this.modelProvider;
    }
}
