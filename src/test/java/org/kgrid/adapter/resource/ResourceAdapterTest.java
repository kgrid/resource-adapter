package org.kgrid.adapter.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kgrid.adapter.api.ActivationContext;
import org.kgrid.adapter.api.AdapterException;
import org.kgrid.adapter.api.ClientRequestBuilder;
import org.kgrid.adapter.api.Executor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Resource adapter tests")
public class ResourceAdapterTest {
    public static final String ENGINE_NAME = "resource";
    public static final String NAAN = "naan";
    public static final String NAME = "name";
    public static final String API_VERSION = "apiVersion";
    public static final String VERSION = "version";
    public static final String ENDPOINT = "endpoint";
    public static final String ARTIFACT_NAME1 = "hello.txt";
    public static final String ARTIFACT_NAME2 = "goodbye.txt";
    public static final URI ENDPOINT_URI =
            URI.create("/" + NAAN + "/" + NAME + "/" + API_VERSION + "/" + ENDPOINT + "/");
    public static final String ENDPOINT_MAP_KEY =
            ENDPOINT_URI.getPath().substring(1, ENDPOINT_URI.getPath().length() - 1);
    public static final URI REQUEST_URI_WITH_ARTIFACT = URI.create("http://localhost:8080/" + NAAN + "/" + NAME + "/" + ENDPOINT + "/" + ARTIFACT_NAME1 + "?v=" + VERSION);
    public static final URI REQUEST_URI = URI.create("http://localhost:8080/" + NAAN + "/" + NAME + "/" + ENDPOINT + "?v=" + VERSION);
    public static final String ARTIFACT_CONTENTS = "hello world!";

    @Mock
    ActivationContext context;
    @InjectMocks
    ResourceAdapter adapter;

    URI locationUri = URI.create(NAAN + "/" + NAME + "/" + VERSION);
    private final String singleArtifactDeploymentSpec =
            "{\"artifact\":\"" + ARTIFACT_NAME1 + "\",\"engine\":\"" + ENGINE_NAME + "\"}";
    private final String twoArtifactDeploymentSpec =
            "{\"artifact\":[\""
                    + ARTIFACT_NAME1
                    + "\", \""
                    + ARTIFACT_NAME2
                    + "\"],\"engine\":\""
                    + ENGINE_NAME
                    + "\"}";

    @BeforeEach
    public void setup() {
        adapter.initialize(context);
    }

    @Test
    @DisplayName("Reports resource engine type")
    public void reportsResourceEngineType() {
        assertEquals(ENGINE_NAME, adapter.getEngines().get(0));
    }

    @Test
    @DisplayName("Reports status up")
    public void reportsStatusUp() {
        assertEquals("UP", adapter.status());
    }

    @Test
    @DisplayName("Can activate object")
    public void getResource_CanActivateObjects() throws IOException {
        when(context.getBinary(URI.create(locationUri + "/" + ARTIFACT_NAME1)))
                .thenReturn(new ByteArrayInputStream(ARTIFACT_CONTENTS.getBytes()));
        Executor ex = activateDeploymentSpec(singleArtifactDeploymentSpec);
        List<String> artifacts = new ArrayList<>();
        artifacts.add("hello.txt");
        InputStream inputStream = (InputStream) ex.execute(new ClientRequestBuilder()
                .body(artifacts)
                .url(REQUEST_URI_WITH_ARTIFACT)
                .build()).getBody();
        assertEquals(ARTIFACT_CONTENTS, new String(inputStream.readAllBytes()));
    }

    @Test
    @DisplayName("Can activate an array of objects")
    public void getResource_objectArrayActivate() throws JsonProcessingException {
        Executor ex = activateDeploymentSpec(twoArtifactDeploymentSpec);
        List<String> artifacts = new ArrayList<>();
        artifacts.add("hello.txt");
        artifacts.add("goodbye.txt");
        assertEquals(artifacts, ex.execute(new ClientRequestBuilder().url(REQUEST_URI).build()).getBody());
    }

    @Test
    @DisplayName("Getting a resource returns activated artifact")
    public void getResource_returnActivatedArtifact() throws JsonProcessingException {
        adapter.initialize(context);
        InputStream stream = (new ByteArrayInputStream(ARTIFACT_CONTENTS.getBytes()));
        when(context.getBinary(URI.create(locationUri + "/" + ARTIFACT_NAME1)))
                .thenReturn(stream);
        Executor ex = activateDeploymentSpec(singleArtifactDeploymentSpec);
        String fileContents = new BufferedReader(new InputStreamReader((InputStream) ex.execute(
                new ClientRequestBuilder()
                        .body("hello.txt")
                        .url(REQUEST_URI_WITH_ARTIFACT)
                        .build()).getBody(),
                Charset.defaultCharset())).lines().collect(Collectors.joining("\n"));
        assertEquals(ARTIFACT_CONTENTS, fileContents);
    }

    @Test
    @DisplayName("Attempting to get an unactivated object throws error")
    public void getResource_unactivatedObjectThrowsException() throws JsonProcessingException {
        adapter.initialize(context);
        Executor ex = activateDeploymentSpec(singleArtifactDeploymentSpec);
        AdapterException adapterException =
                assertThrows(AdapterException.class, () -> ex.execute(
                        new ClientRequestBuilder()
                                .body("notHere.txt")
                                .url(URI.create("http://localhost:8080/resource/simple/welcome/notHere.txt?v=1.0"))
                                .build()));
        assertEquals("Requested resource notHere.txt is not available.", adapterException.getMessage());
    }

    private Executor activateDeploymentSpec(String deploymentSpecJson)
            throws JsonProcessingException {
        JsonNode deploymentSpec = new ObjectMapper().readTree(deploymentSpecJson);
        return adapter.activate(locationUri, URI.create(ENDPOINT_MAP_KEY), deploymentSpec);
    }
}
