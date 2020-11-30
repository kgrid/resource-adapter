package org.kgrid.adapter.resource;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.kgrid.adapter.api.ActivationContext;
import org.kgrid.adapter.api.Adapter;
import org.kgrid.adapter.api.AdapterException;
import org.kgrid.adapter.api.Executor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.*;

@Controller
public class ResourceAdapter implements Adapter {

    public static final Map<URI, ArtifactEntry> activatedObjectArtifacts = new HashMap<>();
    private static ActivationContext context;

    @Override
    public List<String> getEngines() {
        return Collections.singletonList("resource");
    }

    @Override
    public void initialize(ActivationContext activationContext) {
        context = activationContext;
    }

    @Override
    public Executor activate(URI absoluteLocation, URI endpointUri, JsonNode deploymentSpec) {
        JsonNode artifactNode = deploymentSpec.at("/artifact");
        ArrayList<String> artifactLocations = new ArrayList<>();
        if (artifactNode.isArray()) {
            artifactNode.forEach(file -> {
                artifactLocations.add(file.asText());
            });
        } else {
            artifactLocations.add(artifactNode.asText());
        }
        activatedObjectArtifacts.put(endpointUri, new ArtifactEntry(artifactLocations, absoluteLocation));
        return null;
    }

    @Override
    public String status() {
        return "UP";
    }

    @GetMapping(value = "{naan}/{name}/{apiVersion}/{endpoint}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> getResourceList(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String trimmedUri = requestUri.substring(1, requestUri.length() - 1);
        ArtifactEntry artifactEntry = activatedObjectArtifacts.get(URI.create(trimmedUri));
        if (artifactEntry == null || artifactEntry.getArtifacts().size() == 1 && artifactEntry.getArtifacts().get(0).equals("")) {
            throw new AdapterException("No artifacts specified for endpoint: " + trimmedUri);
        } else {
            return new ResponseEntity<>(artifactEntry.getArtifacts(), HttpStatus.OK);
        }
    }

    @GetMapping(value = "{naan}/{name}/{apiVersion}/{endpoint}/**", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> getResource(HttpServletRequest request,
                                              @PathVariable String naan,
                                              @PathVariable String name,
                                              @PathVariable String apiVersion,
                                              @PathVariable String endpoint) {
        URI endpointUri = URI.create(naan + "/" + name + "/" + apiVersion + "/" + endpoint);
        ArtifactEntry artifactEntry = activatedObjectArtifacts.get(
                endpointUri);
        String artifactName = StringUtils.substringAfterLast(request.getRequestURI().substring(1), endpoint + "/");
        if (artifactEntry != null && artifactEntry.getArtifacts().size() > 0
                && artifactEntry.getArtifacts().contains(artifactName)) {
            byte[] bytes = context.getBinary(
                    URI.create(artifactEntry.getBaseLocation() + "/" + artifactName));
            return new ResponseEntity<>(bytes, HttpStatus.OK);
        }
        throw new AdapterException("Object at " + endpointUri + "/" + artifactName + " is not available");
    }
}
