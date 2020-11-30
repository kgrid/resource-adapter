package org.kgrid.adapter.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ResourceAdapterTest {
    public static final String ENGINE_NAME = "resource";
    public static final String NAAN = "naan";
    public static final String NAME = "name";
    public static final String API_VERSION = "apiVersion";
    public static final String VERSION = "version";
    public static final String ENDPOINT = "endpoint";
    public static final String ARTIFACT_NAME1 = "hello.txt";
    public static final String ARTIFACT_NAME2 = "goodbye.txt";
    public static final URI ENDPOINT_URI = URI.create("/" + NAAN + "/" + NAME + "/" + API_VERSION + "/" + ENDPOINT + "/");
    public static final String ENDPOINT_MAP_KEY = ENDPOINT_URI.getPath().substring(1, ENDPOINT_URI.getPath().length() - 1);
    public static final String REQUEST_URI = ENDPOINT_URI + ARTIFACT_NAME1;
    public static final String ARTIFACT_CONTENTS = "hello world!";

    @Mock
    ActivationContext context;
    @InjectMocks
    ResourceAdapter adapter;
    URI locationUri = URI.create(NAAN + "/" + NAME + "/" + VERSION);
    private final String singleArtifactDeploymentSpec =
            "{\"artifact\":\"" + ARTIFACT_NAME1 + "\",\"engine\":\"" + ENGINE_NAME + "\"}";
    private final String twoArtifactDeploymentSpec = "{\"artifact\":[\"" + ARTIFACT_NAME1 + "\", \"" + ARTIFACT_NAME2
            + "\"],\"engine\":\"" + ENGINE_NAME + "\"}";


    @After
    public void tearDown() {
        ResourceAdapter.activatedObjectArtifacts.clear();
    }

    @Test
    public void reportsResourceEngineType() {
        assertEquals(ENGINE_NAME, adapter.getEngines().get(0));
    }

    @Test
    public void reportsStatusUp() {
        assertEquals("UP", adapter.status());
    }

    @Test
    public void getResource_CanActivateObjects() throws JsonProcessingException {
        activateDeploymentSpec(singleArtifactDeploymentSpec);

        ArtifactEntry artifactEntry = ResourceAdapter.activatedObjectArtifacts.get(
                URI.create(ENDPOINT_MAP_KEY));
        assertEquals(1, artifactEntry.getArtifacts().size());
        assertTrue(artifactEntry.getArtifacts().contains(ARTIFACT_NAME1));
    }

    @Test
    public void getResource_objectArrayActivate() throws JsonProcessingException {
        activateDeploymentSpec(twoArtifactDeploymentSpec);

        ArtifactEntry artifactEntry = ResourceAdapter.activatedObjectArtifacts.get(URI.create(ENDPOINT_MAP_KEY));
        assertEquals(2, artifactEntry.getArtifacts().size());
        assertTrue(artifactEntry.getArtifacts().contains(ARTIFACT_NAME1));
        assertTrue(artifactEntry.getArtifacts().contains(ARTIFACT_NAME2));
    }

    @Test
    public void getResource_returnActivatedArtifact() throws JsonProcessingException {
        adapter.initialize(context);
        when(context.getBinary(URI.create(locationUri + "/" + ARTIFACT_NAME1))).thenReturn(ARTIFACT_CONTENTS.getBytes());
        activateDeploymentSpec(singleArtifactDeploymentSpec);

        HttpServletRequest request = new MockHttpServletRequest("GET", REQUEST_URI);
        ResponseEntity<byte[]> response = adapter.getResource(request, NAAN, NAME, API_VERSION, ENDPOINT);

        assertEquals(ARTIFACT_CONTENTS,
                new String(Objects.requireNonNull(response.getBody()), StandardCharsets.UTF_8));
    }

    @Test
    public void getResource_unactivatedObjectThrowsException() throws JsonProcessingException {
        adapter.initialize(context);
        activateDeploymentSpec(singleArtifactDeploymentSpec);
        HttpServletRequest request =
                new MockHttpServletRequest("GET", ENDPOINT_URI + "missingFile.txt");
        AdapterException adapterException = assertThrows(AdapterException.class, () ->
                adapter.getResource(request, NAAN, NAME, API_VERSION, ENDPOINT));
        assertEquals("Object at " + ENDPOINT_MAP_KEY + "/missingFile.txt is not available",
                adapterException.getMessage());
    }

    @Test
    public void getResourceList_returnsListOfArtifacts() throws JsonProcessingException {
        activateDeploymentSpec(twoArtifactDeploymentSpec);
        HttpServletRequest request = new MockHttpServletRequest("GET", ENDPOINT_URI.getPath());

        ResponseEntity<List<String>> response = adapter.getResourceList(request);
        assertEquals(2, Objects.requireNonNull(response.getBody()).size());
        assertTrue(response.getBody().contains(ARTIFACT_NAME1));
        assertTrue(response.getBody().contains(ARTIFACT_NAME2));
    }

    @Test
    public void getResourceList_throwsAdapterExceptionIfListIsEmpty() throws JsonProcessingException {
        String noArtifactDeploymentSpec = "{\"engine\":\"" + ENGINE_NAME + "\"}";
        activateDeploymentSpec(noArtifactDeploymentSpec);
        HttpServletRequest request = new MockHttpServletRequest("GET", ENDPOINT_URI.getPath());

        AdapterException adapterException = assertThrows(AdapterException.class, () ->
                adapter.getResourceList(request));
        assertEquals("No artifacts specified for endpoint: " + ENDPOINT_MAP_KEY, adapterException.getMessage());
    }

    @Test
    public void getResourceList_throwsAdapterExceptionIfListIsNull() {
        HttpServletRequest request = new MockHttpServletRequest("GET", ENDPOINT_URI.getPath());

        AdapterException adapterException = assertThrows(AdapterException.class, () ->
                adapter.getResourceList(request));
        assertEquals("No artifacts specified for endpoint: " + ENDPOINT_MAP_KEY, adapterException.getMessage());
    }

    private void activateDeploymentSpec(String deploymentSpecJson) throws JsonProcessingException {
        JsonNode deploymentSpec = new ObjectMapper().readTree(
                deploymentSpecJson);
        adapter.activate(locationUri, URI.create(ENDPOINT_MAP_KEY), deploymentSpec);
    }
}
