/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.pgp;

import org.mule.runtime.core.api.security.Authentication;
import org.mule.runtime.core.api.security.SecurityContext;
import org.mule.runtime.core.api.security.SecurityContextFactory;

public class PGPSecurityContextFactory implements SecurityContextFactory {

  public SecurityContext create(Authentication authentication) {
    return new PGPSecurityContext((PGPAuthentication) authentication);
  }
}
