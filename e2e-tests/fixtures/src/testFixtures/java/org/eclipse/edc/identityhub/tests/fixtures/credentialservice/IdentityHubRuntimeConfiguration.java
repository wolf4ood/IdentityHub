/*
 *  Copyright (c) 2025 Cofinity-X
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Cofinity-X - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.tests.fixtures.credentialservice;

import org.eclipse.edc.identityhub.tests.fixtures.common.CommonRuntimeConfiguration;
import org.eclipse.edc.identityhub.tests.fixtures.common.Endpoint;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.eclipse.edc.boot.BootServicesExtension.PARTICIPANT_ID;
import static org.eclipse.edc.util.io.Ports.getFreePort;

/**
 * The IdentityHubRuntimeConfiguration class represents an IdentityHub Runtime configuration and provides various information, such as API endpoints
 */
public class IdentityHubRuntimeConfiguration extends CommonRuntimeConfiguration {

    private Endpoint presentationEndpoint;
    private Endpoint identityEndpoint;
    private Endpoint storageEndpoint;
    private String id;
    private String name;

    public Endpoint getPresentationEndpoint() {
        return presentationEndpoint;
    }

    public Map<String, String> config() {
        var did = didFor("user1");
        var publicKeyId = did + "#user1-key";
        return new HashMap<>() {
            {
                put(PARTICIPANT_ID, id);
                put("web.http.port", String.valueOf(getFreePort()));
                put("web.http.path", "/api/v1");
                put("web.http.presentation.port", String.valueOf(presentationEndpoint.getUrl().getPort()));
                put("web.http.presentation.path", presentationEndpoint.getUrl().getPath());
                put("web.http.storage.port", String.valueOf(storageEndpoint.getUrl().getPort()));
                put("web.http.storage.path", String.valueOf(storageEndpoint.getUrl().getPath()));
                put("web.http.identity.port", String.valueOf(identityEndpoint.getUrl().getPort()));
                put("web.http.identity.path", identityEndpoint.getUrl().getPath());
                put("web.http.sts.port", String.valueOf(getFreePort()));
                put("web.http.sts.path", "/api/sts");
                put("web.http.accounts.port", String.valueOf(getFreePort()));
                put("web.http.accounts.path", "/api/accounts");
                put("web.http.did.port", String.valueOf(didEndpoint.getUrl().getPort()));
                put("web.http.did.path", didEndpoint.getUrl().getPath());
                put("edc.runtime.id", name);
                put("edc.ih.iam.id", did);
                put("edc.sql.schema.autocreate", "true");
                put("edc.iam.accesstoken.jti.validation", String.valueOf(true));
                put("edc.iam.sts.publickey.id", publicKeyId);
                put("edc.iam.sts.privatekey.alias", "user1-alias"); //this must be "username"-alias
                put("edc.iam.did.web.use.https", "false");
            }
        };
    }

    public Endpoint getIdentityApiEndpoint() {
        return identityEndpoint;
    }

    public Endpoint getStorageEndpoint() {
        return storageEndpoint;
    }

    public static final class Builder {
        private final IdentityHubRuntimeConfiguration participant;

        private Builder() {
            participant = new IdentityHubRuntimeConfiguration();
        }

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder id(String id) {
            this.participant.id = id;
            return this;
        }

        public Builder name(String name) {
            this.participant.name = name;
            return this;
        }

        public IdentityHubRuntimeConfiguration build() {
            participant.presentationEndpoint = new Endpoint(URI.create("http://localhost:" + getFreePort() + "/api/presentation"), Map.of());
            participant.identityEndpoint = new Endpoint(URI.create("http://localhost:" + getFreePort() + "/api/identity"), Map.of());
            participant.storageEndpoint = new Endpoint(URI.create("http://localhost:" + getFreePort() + "/api/storage"), Map.of());
            participant.didEndpoint = new Endpoint(URI.create("http://localhost:" + getFreePort() + "/"), Map.of());
            return participant;
        }
    }

}
