package org.kgrid.adapter.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.adapter.api.ActivationContext;
import org.kgrid.adapter.api.AdapterException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class ResourceAdapterTest {

    @Mock
    ActivationContext context;
    @InjectMocks
    ResourceAdapter adapter;
    URI locationUri = URI.create("naan-name-version-endpoint");
    URI endpointUri = URI.create("naan/name/version/endpoint");

    @Test
    public void reportsResourceEngineType(){
        assertEquals("resource", adapter.getEngines().get(0));
    }

    @Test
    public void reportsStatusUp(){
        assertEquals("UP", adapter.status());
    }

    @Test
    public void activateGoodCase() throws JsonProcessingException {

        JsonNode deploymentSpec = new ObjectMapper().readTree("{\"artifact\":\"hello.txt\",\"engine\":\"resource\"}");
        adapter.activate(locationUri, endpointUri, deploymentSpec);
        assertEquals(URI.create(locationUri + "/hello.txt"), ResourceAdapter.activatedObjects.get(URI.create("naan/name/version/endpoint/hello.txt")));

    }

    @Test
    public void activateUriEndingWithSlash() throws JsonProcessingException {
        URI locationUri = URI.create("naan-name-version-endpoint/");
        URI endpointUri = URI.create("naan/name/version/endpoint/");
        JsonNode deploymentSpec = new ObjectMapper().readTree("{\"artifact\":\"hello.txt\",\"engine\":\"resource\"}");
        adapter.activate(locationUri, endpointUri, deploymentSpec);
        assertEquals(URI.create(locationUri + "hello.txt"), ResourceAdapter.activatedObjects.get(URI.create("naan/name/version/endpoint/hello.txt")));

    }

    @Test
    public void objectArrayActivate() throws JsonProcessingException {
        JsonNode deploymentSpec = new ObjectMapper().readTree("{\"artifact\":[\"hello.txt\", \"goodbye.txt\"],\"engine\":\"resource\"}");
        adapter.activate(locationUri, endpointUri, deploymentSpec);
        assertEquals(URI.create(locationUri + "/hello.txt"), ResourceAdapter.activatedObjects.get(URI.create("naan/name/version/endpoint/hello.txt")));
        assertEquals(URI.create(locationUri + "/goodbye.txt"), ResourceAdapter.activatedObjects.get(URI.create("naan/name/version/endpoint/goodbye.txt")));
    }

    @Test
    public void returnActivatedArtifact() throws JsonProcessingException {
        adapter.initialize(context);
        when(context.getBinary(any())).thenReturn("hello world!".getBytes());
        JsonNode deploymentSpec = new ObjectMapper().readTree("{\"artifact\":\"hello.txt\",\"engine\":\"resource\"}");
        adapter.activate(locationUri, endpointUri, deploymentSpec);
        HttpServletRequest request = new MockHttpServletRequest("GET", "/naan/name/version/endpoint/hello.txt");
        ResponseEntity<byte[]> response = adapter.getResource(request);

        assertEquals("hello world!", new String(Objects.requireNonNull(response.getBody()), StandardCharsets.UTF_8));

    }

    @Test
    public void unactivatedObjectThrowsException() throws {
        adapter.initialize(context);
        HttpServletRequest request = new MockHttpServletRequest("GET", "/naan/name/version/endpoint/hello2.txt");
        assertThrows(AdapterException.class, () -> adapter.getResource(request));

    }


}