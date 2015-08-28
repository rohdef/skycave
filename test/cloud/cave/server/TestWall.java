package cloud.cave.server;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.junit.*;

import java.util.List;

import cloud.cave.common.CommonCaveTests;
import cloud.cave.domain.*;

/**
 * Initial template of TDD of students' exercises
 *
 * @author Henrik Baerbak Christensen, Aarhus University
 */
public class TestWall {

    private Cave cave;

    private Player player;

    @Before
    public void setUp() throws Exception {
        cave = CommonCaveTests.createTestDoubledConfiguredCave();

        Login loginResult = cave.login("mikkel_aarskort", "123");
        player = loginResult.getPlayer();
    }

    /*
    @Test
    public void shouldWriteToAndReadWall() {
        player.addMessage("This is message no. 1");
        List<String> wallContents = player.getMessageList();

        // TODO: Exercise - solve the 'wall' exercise

        assertThat(wallContents.size(), is(1));
        assertThat(wallContents.get(0), containsString("NOT IMPLEMENTED YET"));
    }
    */

    @Test
    public void shouldReadFirstMessage() {
        assertThat(player.getMessageList().get(0), is("[Mark] First Like"));

    }
    @Test
    public void shouldReadFirstThreeMessages(){
        List<String> wallContents = player.getMessageList();

        assertThat(wallContents.size(), is(3));
        assertThat(wallContents.get(0), is("[Mark] First Like"));
        assertThat(wallContents.get(1), is("[Rohde] OMG det sagde du bare ikk"));
        assertThat(wallContents.get(2), is("[Mark] Jo, jeg fik First Like"));

    }
}
