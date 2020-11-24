package org.kgrid.adapter.resource;

import com.fasterxml.jackson.databind.JsonNode;
import org.kgrid.adapter.api.ActivationContext;
import org.kgrid.adapter.api.Adapter;
import org.kgrid.adapter.api.AdapterException;
import org.kgrid.adapter.api.Executor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ResourceAdapter implements Adapter {

    public static final Map<URI, URI> activatedObjects = new HashMap<>();
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
        URI endpoint = URI.create(endpointUri + "/");
        URI location = URI.create(absoluteLocation + "/");
        JsonNode artifact = deploymentSpec.at("/artifact");
        if (artifact.isArray()) {
            artifact.forEach(file ->
                    activatedObjects.put(endpoint.resolve(file.asText()), location.resolve(file.asText())));
        } else {
            activatedObjects.put(endpoint.resolve(artifact.asText()), location.resolve(artifact.asText()));
        }

        return null;
    }

    @Override
    public String status() {
        return "UP";
    }

    @GetMapping(value = "{naan}/{name}/{apiVersion}/{endpoint}/**", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> getResource(HttpServletRequest request) {
        String requestURI = request.getRequestURI().substring(1);
        URI artifactId = URI.create(requestURI);
        if (activatedObjects.get(artifactId) != null) {
            byte[] bytes = context.getBinary(activatedObjects.get(artifactId));
            return new ResponseEntity<>(bytes, HttpStatus.OK);
        }
        throw new AdapterException("Object at " + artifactId + " is not available");
    }
}
