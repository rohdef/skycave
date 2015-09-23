package cloud.cave.config;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.junit.*;

import cloud.cave.common.*;
import cloud.cave.doubles.StubEnvironmentReaderStrategy;
import cloud.cave.ipc.*;
import cloud.cave.server.common.ServerConfiguration;
import cloud.cave.service.*;

public class TestFactoryDynamicCreation {

    CaveServerFactory factory;
    StubEnvironmentReaderStrategy envReader;

    @Before
    public void setup() {
        envReader = new StubEnvironmentReaderStrategy();
        factory = new EnvironmentServerFactory(envReader);
    }

    @Test
    public void shouldCreateProperCaveInstances() {
        envReader.setNextExpectation(Config.SKYCAVE_CAVESTORAGE_IMPLEMENTATION,
                "cloud.cave.doubles.FakeCaveStorage");
        envReader.setNextExpectation(Config.SKYCAVE_DBSERVER,
                "192.168.237.130:27017");
        CaveStorage storage = factory.createCaveStorage();
        assertThat(storage.toString(), containsString("FakeCaveStorage"));

        ServerConfiguration config = storage.getConfiguration();
        assertNotNull("The initialization must assign a cave storage configuration.", config);
        assertThat(config.get(0).getHostName(), is("192.168.237.130"));
        assertThat(config.get(0).getPortNumber(), is(27017));
    }

    @Test
    public void shouldCreateProperSubscriptionInstances() {
        envReader.setNextExpectation(Config.SKYCAVE_SUBSCRIPTION_IMPLEMENTATION,
                "cloud.cave.server.service.ServerSubscriptionService");
        envReader.setNextExpectation(Config.SKYCAVE_SUBSCRIPTIONSERVER,
                "subscription.baerbak.com:42042");
        envReader.setNextExpectation(Config.REST_REQUEST_IMPLEMENTATION,
                "cloud.cave.config.socket.RestRequester");
        SubscriptionService service = factory.createSubscriptionServiceConnector();
        assertThat(service.toString(), containsString("ServerSubscriptionService"));
        ServerConfiguration config = service.getConfiguration();
        assertNotNull("The initialization must assign a subscription service configuration.", config);
        assertThat(config.get(0).getHostName(), is("subscription.baerbak.com"));
        assertThat(config.get(0).getPortNumber(), is(42042));
    }

    @Test
    public void shouldCreateProperReactorInstances() {
        envReader.setNextExpectation(Config.SKYCAVE_REACTOR_IMPLEMENTATION,
                "cloud.cave.config.socket.SocketReactor");
        envReader.setNextExpectation(Config.SKYCAVE_APPSERVER,
                "localhost:37123");

        Reactor reactor = factory.createReactor(null);
        assertThat(reactor.toString(), containsString("SocketReactor. Assigned to port: 37123"));
    }

    @Test
    public void shouldCreateProperClientRequestHandler() {
        envReader.setNextExpectation(Config.SKYCAVE_CLIENTREQUESTHANDLER_IMPLEMENTATION,
                "cloud.cave.config.socket.SocketClientRequestHandler");
        envReader.setNextExpectation(Config.SKYCAVE_APPSERVER,
                "skycave.mycompany.com:37123");

        CaveClientFactory factory = new EnvironmentClientFactory(envReader);
        ClientRequestHandler crh = factory.createClientRequestHandler();
        assertThat(crh.toString(), containsString("SocketClientRequestHandler. AppServer Cfg: skycave.mycompany.com:37123."));
    }

    @Test
    public void shouldCreateProperCaveReplicaSet() {
        envReader.setNextExpectation(Config.SKYCAVE_CAVESTORAGE_IMPLEMENTATION,
                "cloud.cave.doubles.FakeCaveStorage");
        envReader.setNextExpectation(Config.SKYCAVE_DBSERVER,
                "192.168.237.130:27017,192.168.237.131:27018,192.168.237.132:27019");
        CaveStorage storage = factory.createCaveStorage();
        assertThat(storage.toString(), containsString("FakeCaveStorage"));

        ServerConfiguration config = storage.getConfiguration();
        assertNotNull("The initialization must assign a cave storage configuration.", config);
        assertThat(config.get(0).getHostName(), is("192.168.237.130"));
        assertThat(config.get(0).getPortNumber(), is(27017));

        assertThat(config.get(1).getHostName(), is("192.168.237.131"));
        assertThat(config.get(1).getPortNumber(), is(27018));

        assertThat(config.get(2).getHostName(), is("192.168.237.132"));
        assertThat(config.get(2).getPortNumber(), is(27019));

        assertThat(config.size(), is(3));
    }

    @Test(expected = CaveClassNotFoundException.class)
    public void shouldThrowExceptionForNonExistingCaveClass() {
        envReader = new StubEnvironmentReaderStrategy();
        envReader.setNextExpectation(Config.SKYCAVE_CAVESTORAGE_IMPLEMENTATION,
                "cloud.cave.doubles.SuperDuperNonExistingClass");
        factory = new EnvironmentServerFactory(envReader);
        @SuppressWarnings("unused")
        ExternalService storage = factory.createCaveStorage();
    }

    @Test(expected = CaveConfigurationNotSetException.class)
    public void shouldThrowExceptionIfEnvVarNotSet() {
        envReader = new StubEnvironmentReaderStrategy();
        envReader.setNextExpectation(Config.SKYCAVE_CAVESTORAGE_IMPLEMENTATION,
                null);
        factory = new EnvironmentServerFactory(envReader);
        @SuppressWarnings("unused")
        ExternalService storage = factory.createCaveStorage();
    }

    @Test(expected=CaveConfigurationNotSetException.class)
    public void shouldThrowExceptionIfIndexError() {
        envReader.setNextExpectation(Config.SKYCAVE_CAVESTORAGE_IMPLEMENTATION,
                "cloud.cave.doubles.FakeCaveStorage");
        envReader.setNextExpectation(Config.SKYCAVE_DBSERVER,
                "192.168.237.130:27017,192.168.237.131:27018,192.168.237.132:27019");
        CaveStorage storage = factory.createCaveStorage();

        ServerConfiguration config = storage.getConfiguration();
        config.get(4);
    }

    @Test
    public void shouldCreateProperWeatherInstances() {
        envReader.setNextExpectation(Config.SKYCAVE_WEATHER_IMPLEMENATION,
                "cloud.cave.server.service.ServerWeatherService");
        envReader.setNextExpectation(Config.SKYCAVE_WEATHERSERVER,
                "weather.baerbak.com:8182");
        envReader.setNextExpectation(Config.REST_REQUEST_IMPLEMENTATION,
                "cloud.cave.config.socket.RestRequester");
        WeatherService service = factory.createWeatherServiceConnector();
        assertThat(service.toString(), containsString("ServerWeatherService"));
        ServerConfiguration config = service.getConfiguration();
        assertNotNull("The initialization must assign a weather service configuration.", config);
        assertThat(config.get(0).getHostName(), is("weather.baerbak.com"));
        assertThat(config.get(0).getPortNumber(), is(8182));
    }

    @Test
    public void shouldIncreaseCoverage() {
        // not really fun except for increasing the amount of green
        // paint in jacoco
        ServerConfiguration cfg = new ServerConfiguration("www.baerbak.com", 12345);
        assertThat(cfg.get(0).getHostName(), is("www.baerbak.com"));
        assertThat(cfg.get(0).getPortNumber(), is(12345));

        assertThat(cfg.toString(), containsString("www.baerbak.com:12345"));
    }
}
