/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.metadata.body;

import org.mule.extension.ws.internal.metadata.InputTypeResolverDelegate;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;

/**
 * Input metadata resolver implementation of {@link BodyElementResolver}.
 *
 * @since 4.0
 */
public final class InputBodyResolver extends BodyElementResolver implements InputTypeResolver<String> {

  public InputBodyResolver() {
    super(new InputTypeResolverDelegate());
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, String operationName)
      throws MetadataResolvingException, ConnectionException {
    return getMetadata(context, operationName);
  }
}
