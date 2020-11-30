package org.kgrid.adapter.resource;

import java.net.URI;
import java.util.ArrayList;

public class ArtifactEntry {
    ArrayList<String> artifacts;
    URI baseLocation;

    public ArtifactEntry(ArrayList<String> artifacts, URI baseLocation){
        this.artifacts = artifacts;
        this.baseLocation = baseLocation;
    }
    public void setArtifacts(ArrayList<String> artifacts) {
        this.artifacts = artifacts;
    }

    public void setBaseLocation(URI baseLocation) {
        this.baseLocation = baseLocation;
    }

    public ArrayList<String> getArtifacts() {
        return artifacts;
    }

    public URI getBaseLocation() {
        return baseLocation;
    }
}
