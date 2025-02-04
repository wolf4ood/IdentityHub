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

package org.eclipse.edc.identityhub.issuance.credentials.attestation;

import org.eclipse.edc.identityhub.spi.issuance.credentials.attestation.AttestationContext;
import org.eclipse.edc.identityhub.spi.issuance.credentials.attestation.AttestationDefinitionStore;
import org.eclipse.edc.identityhub.spi.issuance.credentials.attestation.AttestationPipeline;
import org.eclipse.edc.identityhub.spi.issuance.credentials.attestation.AttestationSourceFactory;
import org.eclipse.edc.identityhub.spi.issuance.credentials.attestation.AttestationSourceFactoryRegistry;
import org.eclipse.edc.identityhub.spi.issuance.credentials.model.AttestationDefinition;
import org.eclipse.edc.spi.result.Result;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Holds registered {@link AttestationSourceFactory}s that performs attestation pipeline evaluations.
 */
public class AttestationPipelineImpl implements AttestationPipeline, AttestationSourceFactoryRegistry {
    private Map<String, AttestationSourceFactory> factories = new HashMap<>();
    private AttestationDefinitionStore store;

    public AttestationPipelineImpl(AttestationDefinitionStore store) {
        this.store = store;
    }

    @Override
    public Set<String> registeredTypes() {
        return factories.keySet();
    }

    @Override
    public void registerFactory(String type, AttestationSourceFactory factory) {
        factories.put(type, factory);
    }

    @Override
    public Result<Map<String, Object>> evaluate(Set<String> attestations, AttestationContext context) {
        var collated = new HashMap<String, Object>();
        for (var attestationId : attestations) {
            // if the attestation is not found it is a programming error
            var definition = requireNonNull(store.resolveDefinition(attestationId), "Unknown attestation: " + attestationId);
            var result = execute(definition, context);
            if (result.failed()) {
                return result;
            }
            collated.putAll(result.getContent());
        }
        return Result.success(collated);
    }

    private Result<Map<String, Object>> execute(AttestationDefinition definition, AttestationContext context) {
        var factory = requireNonNull(factories.get(definition.attestationType()), "Unknown attestation type: " + definition.attestationType());
        return requireNonNull(factory.createSource(definition), "Invalid definition for type: " + definition.attestationType()).execute(context);
    }
}


