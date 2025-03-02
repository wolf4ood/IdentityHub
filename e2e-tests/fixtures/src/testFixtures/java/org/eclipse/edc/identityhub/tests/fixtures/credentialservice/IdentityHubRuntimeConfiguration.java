/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.tests.fixtures.credentialservice;

import org.eclipse.edc.identityhub.tests.fixtures.common.AbstractRuntimeConfiguration;
import org.eclipse.edc.identityhub.tests.fixtures.common.Endpoint;
import org.eclipse.edc.spi.system.configuration.Config;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.edc.boot.BootServicesExtension.PARTICIPANT_ID;
import static org.eclipse.edc.util.io.Ports.getFreePort;

/**
 * The IdentityHubRuntimeConfiguration class represents an IdentityHub Runtime configuration and provides various information, such as API endpoints
 */
public class IdentityHubRuntimeConfiguration extends AbstractRuntimeConfiguration {

    private Endpoint identityEndpoint;
    private Endpoint credentialsEndpoint;


    public Config config() {
        return ConfigFactory.fromMap(new HashMap<>() {
            {
                put(PARTICIPANT_ID, id);
                put("web.http.port", String.valueOf(getFreePort()));
                put("web.http.path", "/api/v1");
                put("web.http.credentials.port", String.valueOf(credentialsEndpoint.getUrl().getPort()));
                put("web.http.credentials.path", String.valueOf(credentialsEndpoint.getUrl().getPath()));
                put("web.http.identity.port", String.valueOf(identityEndpoint.getUrl().getPort()));
                put("web.http.identity.path", identityEndpoint.getUrl().getPath());
                put("web.http.sts.port", String.valueOf(getFreePort()));
                put("web.http.sts.path", "/api/sts");
                put("web.http.version.port", String.valueOf(getFreePort()));
                put("web.http.version.path", "/.well-known/version");
                put("web.http.accounts.port", String.valueOf(getFreePort()));
                put("web.http.accounts.path", "/api/accounts");
                put("web.http.did.port", String.valueOf(didEndpoint.getUrl().getPort()));
                put("web.http.did.path", didEndpoint.getUrl().getPath());
                put("edc.runtime.id", name);
                put("edc.sql.schema.autocreate", "true");
                put("edc.iam.accesstoken.jti.validation", String.valueOf(true));
                put("edc.iam.sts.publickey.id", "test-public-key");
                put("edc.iam.sts.privatekey.alias", "user1-alias"); //this must be "username"-alias
                put("edc.iam.did.web.use.https", "false");
            }
        });
    }

    public Endpoint getIdentityApiEndpoint() {
        return identityEndpoint;
    }

    public Endpoint getCredentialsEndpoint() {
        return credentialsEndpoint;
    }

    public static final class Builder extends AbstractRuntimeConfiguration.Builder<IdentityHubRuntimeConfiguration, Builder> {

        private Builder() {
            super(new IdentityHubRuntimeConfiguration());
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public IdentityHubRuntimeConfiguration build() {
            super.build();
            participant.identityEndpoint = new Endpoint(URI.create("http://localhost:" + getFreePort() + "/api/identity"), Map.of());
            participant.credentialsEndpoint = new Endpoint(URI.create("http://localhost:" + getFreePort() + "/api/credentials"), Map.of());
            return participant;
        }
    }

}
