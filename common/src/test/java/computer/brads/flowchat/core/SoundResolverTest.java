package computer.brads.flowchat.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class SoundResolverTest {

    @Test
    public void resolveNamedAliases() {
        assertEquals("minecraft:entity.experience_orb.pickup", SoundResolver.resolve("ding"));
        assertEquals("minecraft:entity.experience_orb.pickup", SoundResolver.resolve("orb"));
        assertEquals("minecraft:entity.player.levelup", SoundResolver.resolve("levelup"));
        assertEquals("minecraft:entity.player.levelup", SoundResolver.resolve("level"));
        assertEquals("minecraft:block.anvil.land", SoundResolver.resolve("anvil"));
        assertEquals("minecraft:block.note_block.bell", SoundResolver.resolve("note"));
        assertEquals("minecraft:block.note_block.bell", SoundResolver.resolve("bell"));
        assertEquals("minecraft:ui.button.click", SoundResolver.resolve("click"));
        assertEquals("minecraft:entity.item.pickup", SoundResolver.resolve("pop"));
    }

    @Test
    public void resolveCaseInsensitive() {
        assertEquals("minecraft:block.note_block.bell", SoundResolver.resolve("BELL"));
        assertEquals("minecraft:block.note_block.bell", SoundResolver.resolve("Bell"));
        assertEquals("minecraft:entity.experience_orb.pickup", SoundResolver.resolve("DING"));
    }

    @Test
    public void resolveSilent() {
        assertNull(SoundResolver.resolve("none"));
        assertNull(SoundResolver.resolve("silent"));
        assertNull(SoundResolver.resolve("NONE"));
        assertNull(SoundResolver.resolve("Silent"));
    }

    @Test
    public void resolveNull() {
        assertEquals("minecraft:entity.experience_orb.pickup", SoundResolver.resolve(null));
    }

    @Test
    public void resolveEmpty() {
        assertEquals("minecraft:entity.experience_orb.pickup", SoundResolver.resolve(""));
    }

    @Test
    public void resolveArbitraryWithNamespace() {
        assertEquals("minecraft:entity.pig.ambient", SoundResolver.resolve("minecraft:entity.pig.ambient"));
        assertEquals("modid:custom.sound", SoundResolver.resolve("modid:custom.sound"));
    }

    @Test
    public void resolveArbitraryWithoutNamespace() {
        assertEquals("minecraft:entity.cow.ambient", SoundResolver.resolve("entity.cow.ambient"));
    }

    @Test
    public void shouldPlayLogic() {
        assertTrue(SoundResolver.shouldPlay("bell"));
        assertTrue(SoundResolver.shouldPlay("minecraft:entity.pig.ambient"));
        assertTrue(SoundResolver.shouldPlay("true"));
        assertFalse(SoundResolver.shouldPlay(null));
        assertFalse(SoundResolver.shouldPlay("false"));
        assertFalse(SoundResolver.shouldPlay("none"));
        assertFalse(SoundResolver.shouldPlay("silent"));
    }

    @Test
    public void getDefaultReturnsOrbPickup() {
        assertEquals("minecraft:entity.experience_orb.pickup", SoundResolver.getDefault());
    }
}
