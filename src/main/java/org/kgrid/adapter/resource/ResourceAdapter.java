package org.kgrid.adapter.resource;

import com.fasterxml.jackson.databind.JsonNode;
import org.kgrid.adapter.api.ActivationContext;
import org.kgrid.adapter.api.Adapter;
import org.kgrid.adapter.api.ClientRequest;
import org.kgrid.adapter.api.Executor;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
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

            @Override
            public Object execute(ClientRequest request) {
                String[] endpointPathParts = request.getUrl().getPath().split("/");
                String[] endpointUriParts = endpointUri.getPath().split("/");

                if (endpointPathParts.length <= endpointUriParts.length) {
                    return artifacts;
                } else {
                    StringBuilder artifactName = new StringBuilder();
                    for(int i = endpointUriParts.length; i < endpointPathParts.length; i++){
                        artifactName.append("/").append(endpointPathParts[i]);
                    }
                    if (artifacts.contains(artifactName.substring(1))) {
                        return context.getBinary(
                                URI.create(absoluteLocation + artifactName.toString()));

                    } else {
                        throw new AdapterResourceNotFoundException("Requested resource " + artifactName + " is not available.");
                    }
                }
            }
        };
    }

    @Override
    public String status() {
        return "UP";
    }
}
