package org.kgrid.adapter.resource;

import com.fasterxml.jackson.databind.JsonNode;
import org.kgrid.adapter.api.ActivationContext;
import org.kgrid.adapter.api.Adapter;
import org.kgrid.adapter.api.Executor;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResourceAdapter implements Adapter {

    private static ActivationContext context;

    @Override
    public List<String> getEngines() {
        return Collections.singletonList("resource");
    }

    @Override
    public void initialize(ActivationContext activationContext) {
        context = activationContext;
        context.refresh(getEngines().get(0));
    }

    @Override
    public Executor activate(URI absoluteLocation, URI endpointUri, JsonNode deploymentSpec) {
        final List<String> artifacts = new ArrayList<>();
        JsonNode artifactNode = deploymentSpec.at("/artifact");
        if (artifactNode.isArray()) {
            artifactNode.forEach(file -> {
                artifacts.add(file.asText());
            });
        } else {
            artifacts.add(artifactNode.asText());
        }
        return new Executor() {
            @Override
            public Object execute(Object inputs, String inputMimetype) {
                if(inputs == null){
                    return artifacts;
                }
                if(artifacts.contains(inputs)) {
                    return context.getBinary(
                            URI.create(absoluteLocation + "/" + inputs));
                } else {
                    throw new AdapterResourceNotFoundException("Requested resource " + inputs + " is not available.");
                }
            }
        };
    }

    @Override
    public String status() {
        return "UP";
    }
}
