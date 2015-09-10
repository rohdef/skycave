package cloud.cave.server.service;

import cloud.cave.domain.Region;
import cloud.cave.doubles.RequestFake;
import cloud.cave.doubles.RequestSaboteur;
import cloud.cave.server.common.ServerConfiguration;
import cloud.cave.service.WeatherService;
import org.apache.http.client.ClientProtocolException;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.MissingResourceException;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

/**
 * License MIT
 *
 * @author Rohde Fischer
 */
public class TestServerWeatherServiceCircuitBreaker {
    private WeatherService weatherService;
    private String group, user, host;
    private int port;
    private RequestSaboteur fakeRequest;
    private ServerConfiguration serverConfiguration;

    @Before
    public void setup() {
        host = "is-there-anybody-out-there";
        port = 57005;
        group = "TheFooBars";
        user = "WhiskyMonster";
        fakeRequest = new RequestSaboteur(new RequestFake());
        serverConfiguration = new ServerConfiguration(host, port);
        weatherService = new ServerWeatherService(fakeRequest);
        weatherService.initialize(serverConfiguration);
    }

    @Test
    public void shouldGiveDefaultResponseWhenOpen() {
//        JSONObject jsonObject = weatherService.requestWeather(group, user, Region.AALBORG);
//
//        assertThat(jsonObject.containsKey("errorMessage"), is(true));
//        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-OPEN"));
    }

    @Test
    public void shouldGiveUnavailbleErrorWhenUnvailableService() {
        fakeRequest.setThrowNext(new IOException("Could not connect to server"));

        JSONObject jsonObject = weatherService.requestWeather(group, user, Region.AALBORG);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));
    }

    @Test
    public void shouldGiveUnavailbleErrorOnUnexpectetError() {
        fakeRequest.setThrowNext(new MissingResourceException("Yes, sir. ... Oh, I'm sorry sir, I thought you were referring to me, Mr Wensleydale.",
                "Moon.class",
                "getCheese()"));

        JSONObject jsonObject = weatherService.requestWeather(group, user, Region.AALBORG);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));
    }

    @Test
    public void shouldGiveUnavailbleErrorWhenGarbageIsReturned() {
        fakeRequest.setGarbage("Tanja loeber hurtigt");

        JSONObject jsonObject = weatherService.requestWeather(group, user, Region.AALBORG);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));
    }

    @Test
    public void shouldOpenAfterThreeAttempts() {
        fakeRequest.setThrowNext(new ClientProtocolException());
        JSONObject jsonObject = weatherService.requestWeather(group, user, Region.AALBORG);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));

        fakeRequest.setGarbage("Hulubulu - Lotte, hvor er du henne?");
        jsonObject = weatherService.requestWeather(group, user, Region.AALBORG);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));

        fakeRequest.setThrowNext(new IOException());
        jsonObject = weatherService.requestWeather(group, user, Region.AALBORG);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));

        fakeRequest.setThrowNext(new RuntimeException());
        jsonObject = weatherService.requestWeather(group, user, Region.AALBORG);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-OPEN"));
    }

    @Test
    public void shouldHalfOpenAfterThirtySeconds() throws InterruptedException {
        fakeRequest.setThrowNext(new ClientProtocolException());
        JSONObject jsonObject = weatherService.requestWeather(group, user, Region.AALBORG);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));

        fakeRequest.setGarbage("Hulubulu - Lotte, hvor er du henne?");
        jsonObject = weatherService.requestWeather(group, user, Region.AALBORG);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));

        fakeRequest.setThrowNext(new IOException());
        jsonObject = weatherService.requestWeather(group, user, Region.AALBORG);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));

        fakeRequest.setThrowNext(new RuntimeException());
        jsonObject = weatherService.requestWeather(group, user, Region.AALBORG);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-OPEN"));

        Thread.sleep(8000);

        fakeRequest.setThrowNext(new RuntimeException());
        jsonObject = weatherService.requestWeather(group, user, Region.AALBORG);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-OPEN"));

        Thread.sleep(2000);
        fakeRequest.reset();
        jsonObject = weatherService.requestWeather(group, user, Region.AALBORG);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("OK"));
    }

    @Test
    public void shouldHalfOpenAfterThirtySecondsWithError() throws InterruptedException {
        fakeRequest.setThrowNext(new ClientProtocolException());
        JSONObject jsonObject = weatherService.requestWeather(group, user, Region.AALBORG);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));

        fakeRequest.setGarbage("Hulubulu - Lotte, hvor er du henne?");
        jsonObject = weatherService.requestWeather(group, user, Region.AALBORG);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));

        fakeRequest.setThrowNext(new IOException());
        jsonObject = weatherService.requestWeather(group, user, Region.AALBORG);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-CLOSED"));

        fakeRequest.setThrowNext(new RuntimeException());
        jsonObject = weatherService.requestWeather(group, user, Region.AALBORG);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-OPEN"));

        Thread.sleep(8000);

        fakeRequest.setThrowNext(new RuntimeException());
        jsonObject = weatherService.requestWeather(group, user, Region.AALBORG);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-OPEN"));

        Thread.sleep(2000);
        fakeRequest.setThrowNext(new IOException());
        jsonObject = weatherService.requestWeather(group, user, Region.AALBORG);
        assertThat(jsonObject.containsKey("errorMessage"), is(true));
        assertThat(jsonObject.get("errorMessage").toString(), equalTo("UNAVAILABLE-OPEN"));
    }
}
