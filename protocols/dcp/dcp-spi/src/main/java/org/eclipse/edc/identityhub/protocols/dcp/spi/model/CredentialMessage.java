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

package org.eclipse.edc.identityhub.protocols.dcp.spi.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

public class CredentialMessage {
    public static final String CREDENTIALS_TERM = "credentials";
    public static final String ISSUER_PID_TERM = "issuerPid";
    public static final String HOLDER_PID_TERM = "holderPid";
    public static final String CREDENTIAL_MESSAGE_TERM = "CredentialMessage";

    private Collection<CredentialContainer> credentials = new ArrayList<>();
    private String issuerPid;
    private String holderPid;

    public Collection<CredentialContainer> getCredentials() {
        return credentials;
    }

    public String getIssuerPid() {
        return issuerPid;
    }

    public String getHolderPid() {
        return holderPid;
    }

    public static final class Builder {
        private final CredentialMessage credentialMessage;

        private Builder(CredentialMessage credentialMessage) {
            this.credentialMessage = credentialMessage;
        }

        public static Builder newInstance() {
            return new Builder(new CredentialMessage());
        }

        public Builder credentials(Collection<CredentialContainer> credentials) {
            this.credentialMessage.credentials = credentials;
            return this;
        }

        public Builder credential(CredentialContainer credential) {
            this.credentialMessage.credentials.add(credential);
            return this;
        }

        public Builder issuerPid(String issuerPid) {
            this.credentialMessage.issuerPid = issuerPid;
            return this;
        }

        public Builder holderPid(String holderPid) {
            this.credentialMessage.holderPid = holderPid;
            return this;
        }

        public CredentialMessage build() {
            Objects.requireNonNull(credentialMessage.issuerPid, "issuerPid");
            Objects.requireNonNull(credentialMessage.holderPid, "holderPid");
            return credentialMessage;
        }
    }
}
