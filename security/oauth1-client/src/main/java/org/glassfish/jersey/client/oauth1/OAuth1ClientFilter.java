/*
 * Copyright (c) 2013, 2023 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.client.oauth1;

import java.io.IOException;

import javax.ws.rs.Priorities;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.client.oauth1.internal.LocalizationMessages;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.oauth1.signature.OAuth1Parameters;
import org.glassfish.jersey.oauth1.signature.OAuth1Secrets;
import org.glassfish.jersey.oauth1.signature.OAuth1Signature;
import org.glassfish.jersey.oauth1.signature.OAuth1SignatureException;

/**
 * Client filter that sign requests using OAuth 1 signatures and signature and other OAuth 1
 * parameters to the {@code Authorization} header. The filter can be used to perform authenticated
 * requests to Service Provider but also to perform requests needed for Authorization process (flow).
 *
 * @author Paul C. Bryan
 * @author Martin Matula
 * @author Miroslav Fuksa
 *
 * @since 2.3
 */
@Priority(Priorities.AUTHENTICATION)
class OAuth1ClientFilter implements ClientRequestFilter {

    private Provider<OAuth1Signature> oAuthSignature;
    private Provider<MessageBodyWorkers> messageBodyWorkers;

    @Inject
    public OAuth1ClientFilter(Provider<OAuth1Signature> oAuthSignature, Provider<MessageBodyWorkers> messageBodyWorkers) {
        this.oAuthSignature = oAuthSignature;
        this.messageBodyWorkers = messageBodyWorkers;
    }

    @Override
    public void filter(ClientRequestContext request) throws IOException {
        final ConsumerCredentials consumerFromProperties
                = (ConsumerCredentials) request.getProperty(OAuth1ClientSupport.OAUTH_PROPERTY_CONSUMER_CREDENTIALS);
        request.removeProperty(OAuth1ClientSupport.OAUTH_PROPERTY_CONSUMER_CREDENTIALS);

        final AccessToken tokenFromProperties
                = (AccessToken) request.getProperty(OAuth1ClientSupport.OAUTH_PROPERTY_ACCESS_TOKEN);
        request.removeProperty(OAuth1ClientSupport.OAUTH_PROPERTY_ACCESS_TOKEN);

        OAuth1Parameters parameters = (OAuth1Parameters) request.getProperty(OAuth1ClientSupport.OAUTH_PROPERTY_OAUTH_PARAMETERS);
        if (parameters == null) {
            parameters = (OAuth1Parameters) request.getConfiguration()
                    .getProperty(OAuth1ClientSupport.OAUTH_PROPERTY_OAUTH_PARAMETERS);
        } else {
            request.removeProperty(OAuth1ClientSupport.OAUTH_PROPERTY_OAUTH_PARAMETERS);
        }

        OAuth1Secrets secrets = (OAuth1Secrets) request.getProperty(OAuth1ClientSupport.OAUTH_PROPERTY_OAUTH_SECRETS);
        if (secrets == null) {
            secrets = (OAuth1Secrets) request.getConfiguration().getProperty(OAuth1ClientSupport.OAUTH_PROPERTY_OAUTH_SECRETS);
        } else {
            request.removeProperty(OAuth1ClientSupport.OAUTH_PROPERTY_OAUTH_SECRETS);
        }

        if (request.getHeaders().containsKey("Authorization")) {
            return;
        }

        // Make modifications to clones.
        final OAuth1Parameters paramCopy = parameters.clone();
        final OAuth1Secrets secretsCopy = secrets.clone();

        checkParametersConsistency(paramCopy, secretsCopy);

        if (consumerFromProperties != null) {
            paramCopy.consumerKey(consumerFromProperties.getConsumerKey());
            secretsCopy.consumerSecret(consumerFromProperties.getConsumerSecret());
        }

        if (tokenFromProperties != null) {
            paramCopy.token(tokenFromProperties.getToken());
            secretsCopy.tokenSecret(tokenFromProperties.getAccessTokenSecret());
        }

        if (paramCopy.getTimestamp() == null) {
            paramCopy.setTimestamp();
        }

        if (paramCopy.getNonce() == null) {
            paramCopy.setNonce();
        }

        try {
            oAuthSignature.get().sign(new RequestWrapper(request, messageBodyWorkers.get()), paramCopy, secretsCopy);
        } catch (OAuth1SignatureException se) {
            throw new ProcessingException(LocalizationMessages.ERROR_REQUEST_SIGNATURE(), se);
        }
    }

    private void checkParametersConsistency(OAuth1Parameters oauth1Parameters, OAuth1Secrets oauth1Secrets) {
        if (oauth1Parameters.getSignatureMethod() == null) {
            oauth1Parameters.signatureMethod("HMAC-SHA1");
        }

        if (oauth1Parameters.getVersion() == null) {
            oauth1Parameters.version();
        }

        if (oauth1Secrets.getConsumerSecret() == null || oauth1Parameters.getConsumerKey() == null) {
            throw new ProcessingException(LocalizationMessages.ERROR_CONFIGURATION_MISSING_CONSUMER());
        }

        if (oauth1Parameters.getToken() != null && oauth1Secrets.getTokenSecret() == null) {
            throw new ProcessingException(LocalizationMessages.ERROR_CONFIGURATION_MISSING_TOKEN_SECRET());
        }
    }
}
