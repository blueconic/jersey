/*
 * Copyright (c) 2012, 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.jersey.server;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.glassfish.jersey.message.internal.Statuses;

/**
 * Jersey container response context.
 *
 * @author Marek Potociar
 */
public class ContainerResponse implements ContainerResponseContext {

    private Response.StatusType status;
    private final ContainerRequest requestContext;
    private final OutboundMessageContext messageContext;
    private boolean mappedFromException;
    private boolean closed;

    /**
     * Create a new Jersey container response context.
     *
     * @param requestContext associated container request context.
     * @param response response instance initializing the response context.
     */
    public ContainerResponse(final ContainerRequest requestContext, final Response response) {
        this(requestContext, OutboundJaxrsResponse.from(response, requestContext.getConfiguration()));
    }

    /**
     * Create a new Jersey container response context.
     *
     * @param requestContext associated container request context.
     * @param response response instance initializing the response context.
     */
    ContainerResponse(final ContainerRequest requestContext, final OutboundJaxrsResponse response) {
        this.requestContext = requestContext;
        this.requestContext.inResponseProcessing();
        this.status = response.getStatusInfo();
        this.messageContext = response.getContext();

        final String varyValue = requestContext.getVaryValue();
        if (varyValue != null && !this.messageContext.getHeaders().containsKey(HttpHeaders.VARY)) {
            /**
             * Add a Vary header using the value computed in the request if present
             * and if the Vary header was not explicitly set in the response already.
             */
            this.messageContext.getHeaders().add(HttpHeaders.VARY, varyValue);
        }
    }

    /**
     * Returns true if the response is result of the exception (for example created during
     * {@link javax.ws.rs.ext.ExceptionMapper exception mapping}).
     *
     * @return True if this response was created based on the exception, false otherwise.
     */
    public boolean isMappedFromException() {
        return mappedFromException;
    }

    /**
     * Sets the flag indicating whether the response was created based on the exception.
     * @param mappedFromException True if this exception if result of the exception (for example result of
     *                      {@link javax.ws.rs.ext.ExceptionMapper exception mapping}).
     */
    public void setMappedFromException(final boolean mappedFromException) {
        this.mappedFromException = mappedFromException;
    }

    @Override
    public int getStatus() {
        return status.getStatusCode();
    }

    @Override
    public void setStatus(final int code) {
        this.status = Statuses.from(code);
    }

    @Override
    public void setStatusInfo(final Response.StatusType status) {
        if (status == null) {
            throw new NullPointerException("Response status must not be 'null'");
        }
        this.status = status;
    }

    @Override
    public Response.StatusType getStatusInfo() {
        return status;
    }

    /**
     * Get the associated container request context paired with this response context.
     *
     * @return associated container request context.
     */
    public ContainerRequest getRequestContext() {
        return requestContext;
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return messageContext.getResponseCookies();
    }

    /**
     * Get the wrapped response message context.
     *
     * @return wrapped response message context.
     */
    public OutboundMessageContext getWrappedMessageContext() {
        return messageContext;
    }

    @Override
    public String getHeaderString(final String name) {
        return messageContext.getHeaderString(name);
    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return messageContext.getHeaders();
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        return messageContext.getStringHeaders();
    }

    @Override
    public Date getDate() {
        return messageContext.getDate();
    }

    @Override
    public Locale getLanguage() {
        return messageContext.getLanguage();
    }

    @Override
    public MediaType getMediaType() {
        return messageContext.getMediaType();
    }

    @Override
    public Set<String> getAllowedMethods() {
        return messageContext.getAllowedMethods();
    }

    @Override
    public int getLength() {
        return messageContext.getLength();
    }

    @Override
    public EntityTag getEntityTag() {
        return messageContext.getEntityTag();
    }

    @Override
    public Date getLastModified() {
        return messageContext.getLastModified();
    }

    @Override
    public URI getLocation() {
        return messageContext.getLocation();
    }

    @Override
    public Set<Link> getLinks() {
        return messageContext.getLinks();
    }

    @Override
    public boolean hasLink(final String relation) {
        return messageContext.hasLink(relation);
    }

    @Override
    public Link getLink(final String relation) {
        return messageContext.getLink(relation);
    }

    @Override
    public Link.Builder getLinkBuilder(final String relation) {
        return messageContext.getLinkBuilder(relation);
    }

    @Override
    public boolean hasEntity() {
        return messageContext.hasEntity();
    }

    @Override
    public Object getEntity() {
        return messageContext.getEntity();
    }

    /**
     * Set a new message message entity.
     *
     * @param entity entity object.
     * @see javax.ws.rs.ext.MessageBodyWriter
     */
    public void setEntity(final Object entity) {
        messageContext.setEntity(entity);
    }

    /**
     * Set a new message message entity.
     *
     * @param entity      entity object.
     * @param annotations annotations attached to the entity.
     * @see javax.ws.rs.ext.MessageBodyWriter
     */
    public void setEntity(final Object entity, final Annotation[] annotations) {
        messageContext.setEntity(entity, annotations);
    }

    /**
     * Set a new message message entity.
     *
     * @param entity      entity object.
     * @param type        declared entity class.
     * @param annotations annotations attached to the entity.
     * @see javax.ws.rs.ext.MessageBodyWriter
     */
    public void setEntity(final Object entity, final Type type, final Annotation[] annotations) {
        messageContext.setEntity(entity, type, annotations);
    }

    @Override
    public void setEntity(final Object entity, final Annotation[] annotations, final MediaType mediaType) {
        messageContext.setEntity(entity, annotations, mediaType);
    }

    /**
     * Set the message content media type.
     *
     * @param mediaType message content media type.
     */
    public void setMediaType(final MediaType mediaType) {
        messageContext.setMediaType(mediaType);
    }

    @Override
    public Class<?> getEntityClass() {
        return messageContext.getEntityClass();
    }

    @Override
    public Type getEntityType() {
        return messageContext.getEntityType();
    }

    /**
     * Set the message entity type information.
     *
     * This method overrides any computed or previously set entity type information.
     *
     * @param type overriding message entity type.
     */
    public void setEntityType(final Type type) {
        Type t = type;
        if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            if (parameterizedType.getRawType().equals(GenericEntity.class)) {
                t = parameterizedType.getActualTypeArguments()[0];
            }
        }

        messageContext.setEntityType(t);
    }

    @Override
    public Annotation[] getEntityAnnotations() {
        return messageContext.getEntityAnnotations();
    }

    /**
     * Set the annotations attached to the entity.
     *
     * @param annotations entity annotations.
     */
    public void setEntityAnnotations(final Annotation[] annotations) {
        messageContext.setEntityAnnotations(annotations);
    }

    @Override
    public OutputStream getEntityStream() {
        return messageContext.getEntityStream();
    }

    @Override
    public void setEntityStream(final OutputStream outputStream) {
        messageContext.setEntityStream(outputStream);
    }


    /**
     * Set the output stream provider callback.
     * <p/>
     * This method must be called before first bytes are written to the {@link #getEntityStream() entity stream}.
     *
     * @param streamProvider non-{@code null} output stream provider.
     */
    public void setStreamProvider(final OutboundMessageContext.StreamProvider streamProvider) {
        messageContext.setStreamProvider(streamProvider);
    }

    /**
     * Enable a buffering of serialized entity. The buffering will be configured from configuration. The property
     * determining the size of the buffer is {@link org.glassfish.jersey.CommonProperties#OUTBOUND_CONTENT_LENGTH_BUFFER}.
     * <p/>
     * The buffering functionality is by default disabled and could be enabled by calling this method. In this case
     * this method must be called before first bytes are written to the {@link #getEntityStream() entity stream}.
     *
     * @param configuration runtime configuration.
     */
    public void enableBuffering(final Configuration configuration) {
        messageContext.enableBuffering(configuration);
    }

    /**
     * Commit the {@link #getEntityStream() entity stream} unless already committed.
     *
     * @throws IOException in case of the IO error.
     */
    public void commitStream() throws IOException {
        messageContext.commitStream();
    }

    /**
     * Returns {@code true} if the entity stream has been committed.
     * @return {@code true} if the entity stream has been committed. Otherwise returns {@code false}.
     */
    public boolean isCommitted() {
        return messageContext.isCommitted();
    }

    /**
     * Closes the response. Flushes and closes the entity stream, frees up container resources associated with
     * the corresponding request.
     */
    public void close() {
        if (!closed) {
            closed = true;
            messageContext.close();
            requestContext.getResponseWriter().commit();
            requestContext.setWorkers(null);
        }
    }

    /**
     * Returns {@code true} if the response entity is a {@link ChunkedOutput} instance.
     * @return {@code true} if the entity is a {@link ChunkedOutput} instance, {@code false} otherwise.
     */
    public boolean isChunked() {
        return hasEntity() && ChunkedOutput.class.isAssignableFrom(getEntity().getClass());
    }
}
