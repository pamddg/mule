/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.lifecycle.LifecycleManager;
import org.mule.runtime.core.context.notification.ServerNotificationManager;

import javax.resource.spi.work.WorkListener;

/**
 * Builder that is used to build instances of {@link MuleContext}. Implementing classes are stateful and should provide public
 * chainable setters for configuring the builder instance and no public getters.
 */
public interface MuleContextBuilder {

  /**
   * Builds a new {@link MuleContext} instance using configured builder instance. Does not initialise or start MuleContext, only
   * constructs the instance.
   */
  MuleContext buildMuleContext();

  void setWorkManager(WorkManager workManager);

  void setWorkListener(WorkListener workListener);

  void setNotificationManager(ServerNotificationManager notificationManager);

  void setLifecycleManager(LifecycleManager lifecycleManager);

  void setMuleConfiguration(MuleConfiguration muleConfiguration);

  /**
   * @param executionClassLoader classloader to use on the created context. Non null.
   */
  void setExecutionClassLoader(ClassLoader executionClassLoader);
}
