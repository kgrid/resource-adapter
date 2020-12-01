package org.kgrid.adapter.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kgrid.adapter.api.ActivationContext;
import org.kgrid.adapter.api.AdapterException;
import org.kgrid.adapter.api.Executor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
  public static final URI ENDPOINT_URI =
      URI.create("/" + NAAN + "/" + NAME + "/" + API_VERSION + "/" + ENDPOINT + "/");
  public static final String ENDPOINT_MAP_KEY =
      ENDPOINT_URI.getPath().substring(1, ENDPOINT_URI.getPath().length() - 1);
  public static final String REQUEST_URI = ENDPOINT_URI + ARTIFACT_NAME1;
  public static final String ARTIFACT_CONTENTS = "hello world!";

  @Mock ActivationContext context;
  @InjectMocks ResourceAdapter adapter;
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

  @After
  public void tearDown() {}

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
    Executor ex = activateDeploymentSpec(singleArtifactDeploymentSpec);
    List<String> artifacts = new ArrayList<>();
    artifacts.add("hello.txt");
    assertEquals(artifacts, ex.execute(null, null));
  }

  @Test
  public void getResource_objectArrayActivate() throws JsonProcessingException {
    Executor ex = activateDeploymentSpec(twoArtifactDeploymentSpec);
    List<String> artifacts = new ArrayList<>();
    artifacts.add("hello.txt");
    artifacts.add("goodbye.txt");
    assertEquals(artifacts, ex.execute(null, null));
  }

  @Test
  public void getResource_returnActivatedArtifact() throws JsonProcessingException {
    adapter.initialize(context);
    when(context.getBinary(URI.create(locationUri + "/" + ARTIFACT_NAME1)))
        .thenReturn(ARTIFACT_CONTENTS.getBytes());
    Executor ex = activateDeploymentSpec(singleArtifactDeploymentSpec);

    assertEquals(
        ARTIFACT_CONTENTS,
        new String((byte[]) ex.execute("hello.txt", null), StandardCharsets.UTF_8));
  }

  @Test
  public void getResource_unactivatedObjectThrowsException() throws JsonProcessingException {
    adapter.initialize(context);
    Executor ex = activateDeploymentSpec(singleArtifactDeploymentSpec);
    AdapterException adapterException =
        assertThrows(AdapterException.class, () -> ex.execute("notHere.txt", null));
    assertEquals("Requested resource notHere.txt is not available.", adapterException.getMessage());
  }

  private Executor activateDeploymentSpec(String deploymentSpecJson)
      throws JsonProcessingException {
    JsonNode deploymentSpec = new ObjectMapper().readTree(deploymentSpecJson);
    return adapter.activate(locationUri, URI.create(ENDPOINT_MAP_KEY), deploymentSpec);
  }
}
