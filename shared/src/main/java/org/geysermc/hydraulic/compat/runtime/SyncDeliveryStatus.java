package org.geysermc.hydraulic.compat.runtime;

public enum SyncDeliveryStatus {
    PLANNED,
    ENCODED,
    QUEUED,
    SENT,
    APPLIED,
    ENCODING_FAILED,
    TRANSPORT_FAILED,
    TARGET_UNAVAILABLE,
    STALE_STATE,
    UNSUPPORTED
}
