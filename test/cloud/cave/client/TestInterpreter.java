package cloud.cave.client;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.io.*;

import org.junit.*;

import cloud.cave.common.CommonCaveTests;
import cloud.cave.domain.Cave;
import cloud.cave.doubles.LocalMethodCallClientRequestHandler;
import cloud.cave.ipc.Invoker;
import cloud.cave.server.StandardInvoker;

/**
 * Testing the command line interpreter. We replace system in and out with
 * ByteArray input and output streams and manipulate these through Strings to
 * 'type stuff' into the interpreter and next evaluate the output. Basically
 * the input is a test stub and the output is a spy.
 *
 * @author Henrik Baerbak Christensen, Aarhus University.
 */
public class TestInterpreter {

    private ByteArrayOutputStream baos;
    private PrintStream ps;
    private CaveProxy caveProxy;

    @Before
    public void setup() {
        baos = new ByteArrayOutputStream();
        ps = new PrintStream(baos);

        Cave cave = CommonCaveTests.createTestDoubledConfiguredCave();
        Invoker srh = new StandardInvoker(cave);
        LocalMethodCallClientRequestHandler crh = new LocalMethodCallClientRequestHandler(srh);
        caveProxy = new CaveProxy(crh);

    }

    @Test
    public void shouldSeeProperOutputForAllCommands() {
        // The command sequence is
        // look, who, weather, sys, exec, n, s, e, w, d, u, p, h, z, dig, u, dig,
        // post, read, exec, exit
        String cmdList =
                "l\nwho\nweather\nsys\nn\ns\ne\nw\nd\nu\np\nh\nz\ndig u Another upper room\n" +
                        "u\ndig d NotPossible\npost A message\nread\n" +
                        "exec HomeCommand null\nexec BimseCommand null\nexec HomeCommand\n" +
                        "exit\nq\n";

        CmdInterpreter cmd = new CmdInterpreter(caveProxy, "magnus_aarskort", "312",
                ps, makeToInputStream(cmdList));
        cmd.readEvalLoop();

        String output = baos.toString();

        // System.out.println(output);

        // look
        assertThat(output, containsString("NORTH   EAST   WEST   UP"));
        assertThat(output, containsString("[0] Magnus"));

        // who
        assertThat(output, containsString("You are: Magnus/user-002 in Region COPENHAGEN"));

        // weather
        assertThat(output, containsString("The weather at: COPENHAGEN"));
        assertThat(output, containsString("The weather in COPENHAGEN is Clear, temperature 27.4C (feelslike -2.7C). Wind: 1.2 m/s, direction West. This report is dated: Thu, 05 Mar 2015 09:38:37 +0100."));

        // sys
        assertThat(output, containsString("ClientRequestHandler: cloud.cave.doubles.LocalMethodCallClientRequestHandler"));

        // north
        assertThat(output, containsString("You moved NORTH"));
        assertThat(output, containsString("You are in open forest, with a deep valley to one side."));
        // south
        assertThat(output, containsString("You moved SOUTH"));
        // east
        assertThat(output, containsString("You moved EAST"));
        // west
        assertThat(output, containsString("You moved WEST"));
        // down
        assertThat(output, containsString("There is no exit going DOWN"));
        // up
        assertThat(output, containsString("You moved UP"));
        // p
        assertThat(output, containsString("Your position in the cave is: (0,0,1)"));
        // h
        assertThat(output, containsString("=== Help on the SkyCave commands. ==="));

        // z
        assertThat(output, containsString("I do not understand that command. (Type 'h' for help)"));

        // dig
        assertThat(output, containsString("You dug a new room in direction UP"));
        assertThat(output, containsString("Another upper room"));

        // invalid dig
        assertThat(output, containsString("You cannot dig there as there is already a room in direction DOWN"));
        assertThat(output, not(containsString("NotPossible")));

        // post and read
        assertThat(output, containsString("*** Message stored ***"));
        assertThat(output, containsString("*** WALL CONTENTS ***"));
        assertThat(output, containsString("A message"));

        // exec
        assertThat(output, containsString("You executed command:HomeCommand"));
        assertThat(output, containsString("Response as JSON: {"));
        // exec 2
        assertThat(output, containsString("Player.execute failed to load Command class: BimseCommand"));
        // exec 3
        assertThat(output, containsString("Exec commands require at least one parameter. Set it to null if irrelevant"));
        // exit
        assertThat(output, containsString("I do not understand that long command. (Type 'h' for help)"));

        // quit
        assertThat(output, containsString("Logged player out, result = SUCCESS"));

    }

    @Test
    public void shouldReportIfUserIsAlreadyLoggedIn() {
        String cmdList = "q\n";

        // Ensure dual login
        caveProxy.login("magnus_aarskort", "312");

        CmdInterpreter cmd = new CmdInterpreter(caveProxy, "magnus_aarskort", "312",
                ps, makeToInputStream(cmdList));
        cmd.readEvalLoop();

        String output = baos.toString();
        // System.out.println(output);

        assertThat(output, containsString("*** WARNING! User 'Magnus' is ALREADY logged in! ***"));
        assertThat(output, containsString("*** The previous session will be disconnected. ***"));
    }

    @Ignore // Require the System.exit(-1) to be refactored out of the interpreter to work.
    @Test
    public void shouldReportIfUserGivesWrongCredentials() {
        String cmdList = "q\n";

        CmdInterpreter cmd = new CmdInterpreter(caveProxy, "magnus_aarskort", "689",
                ps, makeToInputStream(cmdList));
        cmd.readEvalLoop();

        String output = baos.toString();
        // System.out.println(output);

        assertThat(output, containsString("*** Sorry! The login failed. Reason:"));
    }

    private InputStream makeToInputStream(String cmdList) {
        InputStream is = new ByteArrayInputStream(cmdList.getBytes());
        return is;
    }
}
