package cloud.cave.command;

import org.junit.Before;
import org.junit.Test;

import cloud.cave.client.*;
import cloud.cave.common.CommonCaveTests;
import cloud.cave.domain.*;
import cloud.cave.doubles.*;
import cloud.cave.ipc.Invoker;
import cloud.cave.server.*;

/**
 * TDD the command pattern over network.
 * 
 * @author Henrik Baerbak Christensen, Aarhus University
 *
 */
public class TestNewCommandProxy {

  private Player player;

  @Before
  public void setup() {
    // Create the server tier
    Cave cave = CommonCaveTests.createTestDoubledConfiguredCave();
    
    // create the invoker on the server side, bind it to the cave
    Invoker srh = new StandardInvoker(cave);
    
    // create the client request handler as a test double that
    // simply uses method calls to call the 'server side'
    LocalMethodCallClientRequestHandler crh = new LocalMethodCallClientRequestHandler(srh);
    
    // Create the cave proxy, and login mikkel
    Cave caveProxy = new CaveProxy(crh);
    Login loginResult = caveProxy.login( "mikkel_aarskort", "123");
    
    player = (PlayerProxy) loginResult.getPlayer();
  }

  @Test
  public void shouldExecuteHomeCommand() {
    Common.shouldExecuteHomeCommand(player);
  }

  @Test
  public void shouldExecuteJumpCommand() {
    Common.shouldExecuteJumpCommand(player);
  }

  @Test
  public void shouldNotExecuteUnknownCommand() {
    Common.shouldExecuteUnknownCommand(player);
  }
}
