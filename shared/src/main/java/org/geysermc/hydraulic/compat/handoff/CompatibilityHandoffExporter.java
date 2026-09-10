package org.geysermc.hydraulic.compat.handoff;

import org.geysermc.hydraulic.Constants;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Processes the durable handoff queue asynchronously so startup never blocks on transport success.
 */
public final class CompatibilityHandoffExporter {
    private final Logger logger;
    private final CompatibilityHandoffQueue queue;
    private final Path exportRoot;
    private final ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public CompatibilityHandoffExporter(@NotNull Logger logger, @NotNull CompatibilityHandoffQueue queue, @NotNull Path cacheRoot) {
        this.logger = logger;
        this.queue = queue;
        this.exportRoot = cacheRoot.resolve("handoff-queue").resolve("exports");
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "hydraulic-compatibility-handoff");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void scheduleRetryProcessing() {
        if (!this.running.compareAndSet(false, true)) {
            return;
        }

        this.executor.execute(() -> {
            try {
                this.processPendingEntries();
            } finally {
                this.running.set(false);
            }
        });
    }

    private void processPendingEntries() {
        List<CompatibilityHandoffQueue.QueueEntry> pending = this.queue.pendingEntries();
        if (pending.isEmpty()) {
            return;
        }

        try {
            Files.createDirectories(this.exportRoot);
        } catch (Exception e) {
            this.logger.error("Failed to initialize handoff export directory {}", this.exportRoot, e);
            return;
        }

        for (CompatibilityHandoffQueue.QueueEntry entry : pending) {
            Path exportPath = this.exportRoot.resolve(entry.entryId() + ".json");
            try {
                Files.createDirectories(exportPath.getParent());
                try (var writer = Files.newBufferedWriter(exportPath)) {
                    Constants.GSON.toJson(entry.envelope(), writer);
                }
                this.queue.markCompleted(entry.fingerprint(), "exported:" + exportPath.getFileName());
            } catch (Exception e) {
                this.queue.markFailed(entry.fingerprint(), e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                this.logger.warn("Failed to export compatibility handoff entry {}", entry.entryId(), e);
            }
        }
    }

    public void shutdown() {
        this.executor.shutdownNow();
    }
}
