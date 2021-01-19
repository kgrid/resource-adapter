package org.kgrid.adapter.resource;

import org.kgrid.adapter.api.AdapterClientErrorException;

public class AdapterResourceNotFoundException extends AdapterClientErrorException {
  public AdapterResourceNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public AdapterResourceNotFoundException(String message) {
    super(message);
  }
}
