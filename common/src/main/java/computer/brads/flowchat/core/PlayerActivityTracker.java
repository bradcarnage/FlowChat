package computer.brads.flowchat.core;

import java.util.UUID;

/**
 * Tracks movement packets per player to distinguish real players from bots/NPCs.
 * Threshold: >=5 movement packets in last 5 minutes = real player.
 */
public class PlayerActivityTracker {
    private static final int WINDOW_MS = 5 * 60 * 1000; // 5 minutes
    private static final int THRESHOLD = 5;
    private static final int BUFFER_SIZE = 64;

    public final UUID uuid;
    public final String name;
    private final long[] timestamps = new long[BUFFER_SIZE];
    private int head = 0;
    private int size = 0;

    public PlayerActivityTracker(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    /**
     * Record a movement packet at the given timestamp.
     */
    public void recordMovement(long timestampMs) {
        timestamps[head] = timestampMs;
        head = (head + 1) % BUFFER_SIZE;
        if (size < BUFFER_SIZE) size++;
    }

    /**
     * Record a movement packet at current time.
     */
    public void recordMovement() {
        recordMovement(System.currentTimeMillis());
    }

    /**
     * Count packets within the rolling window.
     */
    public int getPacketCount(long nowMs) {
        long cutoff = nowMs - WINDOW_MS;
        int count = 0;
        for (int i = 0; i < size; i++) {
            int idx = (head - 1 - i + BUFFER_SIZE) % BUFFER_SIZE;
            if (timestamps[idx] >= cutoff) count++;
        }
        return count;
    }

    /**
     * Is this player considered "real" (enough movement packets)?
     */
    public boolean isRealPlayer(long nowMs) {
        return getPacketCount(nowMs) >= THRESHOLD;
    }

    /**
     * Is this player considered "real" at current time?
     */
    public boolean isRealPlayer() {
        return isRealPlayer(System.currentTimeMillis());
    }
}
