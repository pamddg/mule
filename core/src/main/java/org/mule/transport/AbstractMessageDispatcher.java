/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;

import org.mule.DefaultMuleEvent;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.MessageDispatcher;
import org.mule.service.ServiceAsyncReplyCompositeMessageSource;

import java.util.List;

/**
 * Abstract implementation of an outbound channel adaptors. Outbound channel adaptors
 * send messages over over a specific transport. Different implementations may
 * support different Message Exchange Patterns.
 */
public abstract class AbstractMessageDispatcher extends AbstractConnectable implements MessageDispatcher
{

    protected List<Transformer> defaultOutboundTransformers;;       
    protected List<Transformer> defaultResponseTransformers;;       

    public AbstractMessageDispatcher(OutboundEndpoint endpoint)
    {
        super(endpoint);
    }

    @Override
    protected ConnectableLifecycleManager createLifecycleManager()
    {
        defaultOutboundTransformers = connector.getDefaultOutboundTransformers(endpoint);       
        defaultResponseTransformers = connector.getDefaultResponseTransformers(endpoint);       
        return new ConnectableLifecycleManager<MessageDispatcher>(getDispatcherName(), this);
    }

    protected String getDispatcherName()
    {
        return getConnector().getName() + ".dispatcher." + System.identityHashCode(this);
    }
    


    public MuleEvent process(MuleEvent event) throws MuleException
    {
        MuleEvent resultEvent = null;
        try
        {
            connect();

            String prop = (String) event.getMessage().getProperty(MuleProperties.MULE_DISABLE_TRANSPORT_TRANSFORMER_PROPERTY);
            boolean disableTransportTransformer = (prop != null && Boolean.parseBoolean(prop)) || endpoint.isDisableTransportTransformer();
                        
            if (!disableTransportTransformer)
            {
                applyOutboundTransformers(event);            
            }
            if (endpoint.getExchangePattern().hasResponse())
            {
                MuleMessage resultMessage = doSend(event);
                if (resultMessage != null)
                {
                    resultEvent = new DefaultMuleEvent(resultMessage, event);
                    // TODO It seems like this should go here but it causes unwanted behaviour and breaks test cases.
                    //if (!disableTransportTransformer)
                    //{
                    //    applyResponseTransformers(resultEvent);            
                    //}
                }
            }
            else
            {
                doDispatch(event);
            }
        }
        catch (MuleException muleException)
        {
            throw muleException;
        }
        catch (Exception e)
        {
            throw new DispatchException(event.getMessage(), endpoint, e);
        }
        return resultEvent;
    }

    /**
     * @deprecated
     */
    @Deprecated
    protected boolean returnResponse(MuleEvent event)
    {
        // Pass through false to conserve the existing behavior of this method but
        // avoid duplication of code.
        return returnResponse(event, false);
    }

    /**
     * Used to determine if the dispatcher implementation should wait for a response
     * to an event on a response channel after it sends the event. The following
     * rules apply:
     * <ol>
     * <li>The connector has to support "back-channel" response. Some transports do
     * not have the notion of a response channel.
     * <li>Check if the endpoint is synchronous (outbound synchronicity is not
     * explicit since 2.2 and does not use the remoteSync message property).
     * <li>Or, if the send() method on the dispatcher was used. (This is required
     * because the ChainingRouter uses send() with async endpoints. See MULE-4631).
     * <li>Finally, if the current service has a response router configured, that the
     * router will handle the response channel event and we should not try and
     * receive a response in the Message dispatcher If remotesync should not be used
     * we must remove the REMOTE_SYNC header Note the MuleClient will automatically
     * set the REMOTE_SYNC header when client.send(..) is called so that results are
     * returned from remote invocations too.
     * </ol>
     * 
     * @param event the current event
     * @return true if a response channel should be used to get a response from the
     *         event dispatch.
     */
    protected boolean returnResponse(MuleEvent event, boolean doSend)
    {
        boolean remoteSync = false;
        if (event.getEndpoint().getConnector().isResponseEnabled())
        {
            boolean hasResponse = event.getEndpoint().getExchangePattern().hasResponse();
            remoteSync = hasResponse || doSend;
            if (remoteSync)
            {
                // service will be null for client calls
                if (event.getFlowConstruct() != null && event.getFlowConstruct() instanceof Service)
                {
                    ServiceAsyncReplyCompositeMessageSource responseRouters = ((Service) event.getFlowConstruct()).getAsyncReplyMessageSource();
                    if (responseRouters != null && responseRouters.getEndpoints().size() > 0)
                    {
                        remoteSync = false;
                    }
                    else
                    {
                        remoteSync = true;
                    }
                }
            }
        }
        if (!remoteSync)
        {
            event.getMessage().removeProperty(MuleProperties.MULE_REMOTE_SYNC_PROPERTY);
        }
        return remoteSync;
    }

    @Override
    protected WorkManager getWorkManager()
    {
        try
        {
            return connector.getDispatcherWorkManager();
        }
        catch (MuleException e)
        {
            logger.error(e);
            return null;
        }
    }

    @Override
    public OutboundEndpoint getEndpoint()
    {
        return (OutboundEndpoint) super.getEndpoint();
    }
    
    protected void applyOutboundTransformers(MuleEvent event) throws TransformerException
    {
        event.getMessage().applyTransformers(defaultOutboundTransformers);
    }

    protected void applyResponseTransformers(MuleEvent event) throws TransformerException
    {
        event.getMessage().applyTransformers(defaultResponseTransformers);
    }

    protected abstract void doDispatch(MuleEvent event) throws Exception;

    protected abstract MuleMessage doSend(MuleEvent event) throws Exception;
}
