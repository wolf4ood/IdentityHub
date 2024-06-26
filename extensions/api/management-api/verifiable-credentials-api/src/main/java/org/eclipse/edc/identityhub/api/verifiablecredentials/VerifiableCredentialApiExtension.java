/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.identityhub.api.verifiablecredentials;

import org.eclipse.edc.identityhub.api.verifiablecredentials.v1.GetAllCredentialsApiController;
import org.eclipse.edc.identityhub.api.verifiablecredentials.v1.VerifiableCredentialsApiController;
import org.eclipse.edc.identityhub.spi.AuthorizationService;
import org.eclipse.edc.identityhub.spi.ManagementApiConfiguration;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantResource;
import org.eclipse.edc.identityhub.spi.store.CredentialStore;
import org.eclipse.edc.identityhub.spi.verifiablecredentials.model.VerifiableCredentialResource;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

import static org.eclipse.edc.identityhub.api.verifiablecredentials.VerifiableCredentialApiExtension.NAME;

@Extension(NAME)
public class VerifiableCredentialApiExtension implements ServiceExtension {
    public static final String NAME = "VerifiableCredentials Management API Extension";

    @Inject
    private ManagementApiConfiguration managementApiConfiguration;
    @Inject
    private WebService webService;
    @Inject
    private CredentialStore credentialStore;
    @Inject
    private AuthorizationService authorizationService;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        authorizationService.addLookupFunction(VerifiableCredentialResource.class, this::queryById);
        var controller = new VerifiableCredentialsApiController(credentialStore, authorizationService);
        var getAllController = new GetAllCredentialsApiController(credentialStore);
        webService.registerResource(managementApiConfiguration.getContextAlias(), controller);
        webService.registerResource(managementApiConfiguration.getContextAlias(), getAllController);
    }

    private ParticipantResource queryById(String credentialId) {
        return credentialStore.query(QuerySpec.Builder.newInstance().filter(new Criterion("id", "=", credentialId)).build())
                .map(list -> list.iterator().next())
                .orElseThrow(f -> new EdcException(f.getFailureDetail()));
    }
}
