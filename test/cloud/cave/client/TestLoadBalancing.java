package cloud.cave.client;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import cloud.cave.server.service.ServerSubscriptionService;
import cloud.cave.server.service.ServerWeatherService;
import org.json.simple.JSONObject;
import org.junit.*;

import cloud.cave.config.CaveServerFactory;
import cloud.cave.common.CommonCaveTests;
import cloud.cave.domain.*;
import cloud.cave.doubles.*;
import cloud.cave.ipc.*;
import cloud.cave.server.*;
import cloud.cave.server.common.ServerConfiguration;
import cloud.cave.service.*;

/**
 * Test scalability of servers, what happens when a request hits a server that
 * has never handled any previous requests from the player before, because
 * the session was initialized on some other (load balanced) server.
 * <p/>
 * To scale from one server to several handling load, each server has to be
 * stateless, or rather handle session state in some well defined manner
 * across the entire server cluster.
 *
 * @author Henrik Baerbak Christensen, Aarhus University.
 */
public class TestLoadBalancing {
    private Player player;
    private LoadBalancedLocalMethodCallClientRequestHandler crh;
    private CaveProxy caveProxy;
    private CaveStorage storage;
    private WeatherService weatherService;
    private Player player2;

    @Before
    public void setup() {
        // In a multi server scenario that facilitate testing a
        // 'Session Database' handling of session data, we of course
        // must have two servers that access the same underlying
        // storage system. Thus we create one FakeStorage, and
        // let the two servers both share that.
        storage = new FakeCaveStorage();
        storage.initialize(null);

        // In a multi server scenario, each server creates its own
        // cave object during initialization; we mimic the multi server
        // configuration by creating two caves, assigned to different
        // servers but with the same underlying storage
        CaveServerFactory factory = new FactoryWithSharedStorage(storage);
        weatherService = factory.createWeatherServiceConnector();

        Cave caveServer1 = new StandardServerCave(factory);
        Cave caveServer2 = new StandardServerCave(factory);

        // Create the server side invokers
        Invoker srh1 = new StandardInvoker(caveServer1);
        Invoker srh2 = new StandardInvoker(caveServer2);

        // and here comes the trick; we create a client side
        // requester that takes the two server invokers as parameters, and
        // provides a method for setting which one the
        // next requests will be forwarded to.
        crh = new LoadBalancedLocalMethodCallClientRequestHandler(srh1, srh2);

        // Create the cave proxy, and login mikkel; server communication
        // will be made with server 1 as this is the default for the
        // load balancing requester.
        caveProxy = new CaveProxy(crh);
        Login loginResult = caveProxy.login("mikkel_aarskort", "123");

        player = loginResult.getPlayer();
        loginResult = caveProxy.login("mathilde_aarskort", "321");
        player2 = loginResult.getPlayer();
    }

    @Test
    public void shouldProveThatOurServerIsStatefulAndThusDoesNotScale() {
        // Verify that requests are forwarded to server 1
        assertThat(crh.toString(), containsString("server 1"));
        // Verify that method calls runs smoothly to the server
        assertThat(player.getLongRoomDescription(0), containsString("[0] Mikkel"));

        // now we simulate that the next request will be forwarded to server 2
        // by the load balancing of the SkyCave system
        crh.setWhichServerToUse(2);
        // Verify that requests are forwarded to server 2
        assertThat(crh.toString(), containsString("server 2"));

        // Verify that the shit hits the fan now!
        try {
            String d = player.getLongRoomDescription(0);

            assertThat(d, containsString("[0] Mikkel"));
        } catch (NullPointerException exc) {
            // In a proper scalable implementation, session state
            // must be available across all servers. This is 
            // future exercise. Enable the fail below once this
            // is implemented.
            fail("The server is statefull which disallows scaling!");
        }
    }
}

/**
 * A factory that creates test doubles but in case of the
 * storage, returns the SAME storage to allow different caves
 * access to the same underlying storage.
 *
 * @author Henrik Baerbak Christensen, Aarhus University.
 */
class FactoryWithSharedStorage implements CaveServerFactory {
    private CaveStorage storage;

    public FactoryWithSharedStorage(CaveStorage storage) {
        this.storage = storage;
    }

    @Override
    public CaveStorage createCaveStorage() {
        // Return the SAME for both caves
        return storage;
    }

    @Override
    public SubscriptionService createSubscriptionServiceConnector() {
        String host = "is-there-anybody-out-there";
        int port = 57005;
        ServerConfiguration serverConfiguration = new ServerConfiguration(host, port);

        SubscriptionService service = new ServerSubscriptionService();
        service.setRestRequester(new SubscriptionServiceRequestFake());
        service.initialize(serverConfiguration);
        return service;
    }

    @Override
    public IRestRequest createRestRequester() {
        return null;
    }

    @Override
    public WeatherService createWeatherServiceConnector() {
        String host = "is-there-anybody-out-there";
        int port = 57005;
        ServerConfiguration serverConfiguration = new ServerConfiguration(host, port);

        WeatherService service = new ServerWeatherService();
        service.initialize(serverConfiguration); // no config object required
        service.setRestRequester(new WeatherServiceRequestFake());
        service.setSecondsDelay(1);

        return service;
    }

    @Override
    public Reactor createReactor(Invoker invoker) {
        // Not used...
        return null;
    }

    @Override
    public PlayerSessionCache createPlayerSessionCache(CaveStorage storage, WeatherService weatherService) {
        return new DatabaseCache(storage, weatherService);
    }
}


/**
 * A test double request handler which simulates load balancing
 * requests over two servers, by allowing to choose which
 * of two request handlers to forward the next requests to.
 *
 * @author Henrik Baerbak Christensen, Aarhus University.
 */
class LoadBalancedLocalMethodCallClientRequestHandler implements ClientRequestHandler {
    private Invoker requesterServer1;
    private Invoker requesterServer2;

    private int whichOne;


    public LoadBalancedLocalMethodCallClientRequestHandler(
            Invoker srh1, Invoker srh2) {
        this.requesterServer1 = srh1;
        this.requesterServer2 = srh2;
        this.whichOne = 1;
    }

    public void setWhichServerToUse(int no) {
        assert no == 1 || no == 2;
        whichOne = no;
    }

    @Override
    public void initialize(ServerConfiguration config) {
        // Not relevant, as this request handler is only used in
        // testing and under programmatic control
    }

    @Override
    public JSONObject sendRequestAndBlockUntilReply(JSONObject requestJson) {
        Invoker requestHandler = null;
        if (whichOne == 1)
            requestHandler = requesterServer1;
        else
            requestHandler = requesterServer2;
        return requestHandler.handleRequest(requestJson);
    }

    @Override
    public String toString() {
        return "LoadBalancedLocalMethodCallClientRequestHandler, dispatching to server " + whichOne;
    }
}