package computer.brads.flowchat.core;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.UUID;

/**
 * Tests for PlayerActivityTracker — ring buffer, threshold logic, expiry.
 */
public class PlayerActivityTrackerTest {

    private static final UUID TEST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    public void newTracker_notReal() {
        PlayerActivityTracker t = new PlayerActivityTracker(TEST_UUID, "Steve");
        assertFalse(t.isRealPlayer(System.currentTimeMillis()));
        assertEquals(0, t.getPacketCount(System.currentTimeMillis()));
    }

    @Test
    public void belowThreshold_notReal() {
        PlayerActivityTracker t = new PlayerActivityTracker(TEST_UUID, "Steve");
        long now = System.currentTimeMillis();
        for (int i = 0; i < 4; i++) t.recordMovement(now - i * 1000);
        assertFalse(t.isRealPlayer(now));
        assertEquals(4, t.getPacketCount(now));
    }

    @Test
    public void atThreshold_isReal() {
        PlayerActivityTracker t = new PlayerActivityTracker(TEST_UUID, "Steve");
        long now = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) t.recordMovement(now - i * 1000);
        assertTrue(t.isRealPlayer(now));
        assertEquals(5, t.getPacketCount(now));
    }

    @Test
    public void aboveThreshold_isReal() {
        PlayerActivityTracker t = new PlayerActivityTracker(TEST_UUID, "Steve");
        long now = System.currentTimeMillis();
        for (int i = 0; i < 20; i++) t.recordMovement(now - i * 1000);
        assertTrue(t.isRealPlayer(now));
        assertEquals(20, t.getPacketCount(now));
    }

    @Test
    public void expiredPackets_notCounted() {
        PlayerActivityTracker t = new PlayerActivityTracker(TEST_UUID, "Steve");
        long now = System.currentTimeMillis();
        long fiveMinAgo = 5 * 60 * 1000;
        // Record 10 packets, all expired (6+ minutes ago)
        for (int i = 0; i < 10; i++) t.recordMovement(now - fiveMinAgo - 60_000 - i * 1000);
        assertFalse(t.isRealPlayer(now));
        assertEquals(0, t.getPacketCount(now));
    }

    @Test
    public void mixedFreshAndExpired() {
        PlayerActivityTracker t = new PlayerActivityTracker(TEST_UUID, "Steve");
        long now = System.currentTimeMillis();
        long fiveMinAgo = 5 * 60 * 1000;
        // 3 fresh, 3 expired
        for (int i = 0; i < 3; i++) t.recordMovement(now - i * 1000);
        for (int i = 0; i < 3; i++) t.recordMovement(now - fiveMinAgo - 60_000 - i * 1000);
        assertEquals(3, t.getPacketCount(now));
        assertFalse(t.isRealPlayer(now));
    }

    @Test
    public void ringBufferWraps() {
        PlayerActivityTracker t = new PlayerActivityTracker(TEST_UUID, "Steve");
        long now = System.currentTimeMillis();
        // Fill past buffer size (64), all within window
        for (int i = 0; i < 100; i++) t.recordMovement(now - i * 100);
        // Should cap at buffer size (64 most recent)
        assertTrue(t.isRealPlayer(now));
        assertEquals(64, t.getPacketCount(now));
    }

    @Test
    public void nameAndUuid() {
        PlayerActivityTracker t = new PlayerActivityTracker(TEST_UUID, "Alex");
        assertEquals("Alex", t.name);
        assertEquals(TEST_UUID, t.uuid);
    }

    @Test
    public void noArgRecordMovement() {
        PlayerActivityTracker t = new PlayerActivityTracker(TEST_UUID, "Steve");
        for (int i = 0; i < 5; i++) t.recordMovement();
        assertTrue(t.isRealPlayer());
    }

    @Test
    public void exactWindowBoundary() {
        PlayerActivityTracker t = new PlayerActivityTracker(TEST_UUID, "Steve");
        long now = System.currentTimeMillis();
        long windowMs = 5 * 60 * 1000;
        // Packet at exactly 5 min ago — should be included (cutoff = now - window, packet >= cutoff)
        for (int i = 0; i < 5; i++) t.recordMovement(now - windowMs);
        assertTrue(t.isRealPlayer(now));
    }

    @Test
    public void justPastWindow() {
        PlayerActivityTracker t = new PlayerActivityTracker(TEST_UUID, "Steve");
        long now = System.currentTimeMillis();
        long windowMs = 5 * 60 * 1000;
        // Packet at 5min + 1ms ago — should be excluded
        for (int i = 0; i < 5; i++) t.recordMovement(now - windowMs - 1);
        assertFalse(t.isRealPlayer(now));
    }
}
