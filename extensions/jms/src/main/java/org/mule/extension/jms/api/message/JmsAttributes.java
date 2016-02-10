/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.jms.api.message;

import java.io.Serializable;
import java.util.Map;

public interface JmsAttributes extends Serializable
{

    JmsProperties getProperties();
    JmsHeaders getHeaders();
    void acknowlewdge();

}
