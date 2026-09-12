package org.geysermc.hydraulic.compat.runtime;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiBlockHighlightOverlayTest {
    private static final Identifier MACHINE = Identifier.fromNamespaceAndPath("hydraulic", "crushing_wheel");

    @Test
    void createsMultiBlockHighlightBoxesWithValidDimensions() {
        MultiBlockHighlightOverlay.HighlightBox box = new MultiBlockHighlightOverlay.HighlightBox(
            "0,64,0",
            0, 64, 0,
            2, 66, 2,
            MultiBlockHighlightOverlay.HighlightStatus.UNFORMED,
            "Structure incomplete: missing 4 casing blocks"
        );

        assertEquals("0,64,0", box.positionKey());
        assertEquals(27, box.volume());
        assertEquals(MultiBlockHighlightOverlay.HighlightStatus.UNFORMED, box.status());
        assertEquals("Structure incomplete: missing 4 casing blocks", box.message());
    }

    @Test
    void rejectsInvertedCoordinates() {
        assertThrows(IllegalArgumentException.class, () ->
            new MultiBlockHighlightOverlay.HighlightBox(
                "0,64,0",
                5, 64, 0,
                2, 66, 2,
                MultiBlockHighlightOverlay.HighlightStatus.INVALID,
                null
            )
        );
    }

    @Test
    void encodesAndDispatchesMultiBlockHighlightChanges() {
        MultiBlockHighlightOverlay.HighlightBox box = new MultiBlockHighlightOverlay.HighlightBox(
            "10,70,10",
            10, 70, 10,
            12, 72, 12,
            MultiBlockHighlightOverlay.HighlightStatus.FORMED,
            "Multi-block assembled"
        );

        StateChangeSet.FieldChange stateChange = MultiBlockHighlightOverlay.createHighlightStateChange(MACHINE, box);
        assertEquals("multiblock.highlight.10,70,10", stateChange.field());
        assertTrue(stateChange.after().toString().contains("formed:10,70,10->12,72,12"));

        SyncBatch batch = new SyncBatch(List.of(new SyncChange(MACHINE, stateChange.field(), stateChange.before(), stateChange.after(), SyncPriority.IMMEDIATE)));
        SyncEncoder encoder = new SyncEncoder();
        List<EncodedSyncChange> encodedList = encoder.encode(batch);

        assertEquals(1, encodedList.size());
        EncodedSyncChange encoded = encodedList.getFirst();
        assertEquals(EncodedSyncKind.MULTIBLOCK_HIGHLIGHT, encoded.kind());

        GeyserSyncTransport transport = new GeyserSyncTransport(
            null,
            packet -> {},
            (s, id, c) -> org.cloudburstmc.protocol.bedrock.data.inventory.ItemData.AIR,
            () -> 0
        );

        List<SyncDeliveryResult> results = transport.deliver(encodedList);
        assertEquals(1, results.size());
        assertEquals(SyncDeliveryStatus.SENT, results.getFirst().status());
        assertTrue(results.getFirst().reason().contains("Multi-block"));
    }
}
