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

package org.eclipse.edc.issuerservice.spi;

import org.eclipse.edc.identityhub.spi.participantcontext.model.ContextResource;
import org.eclipse.edc.identityhub.spi.participantcontext.model.ParticipantContext;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;

/**
 * This is the base class for all resources that are owned by a {@link ParticipantContext} in the Issuer context.
 */
public abstract class IssuerResource implements ContextResource {
    protected String issuerContextId;

    public static QuerySpec.Builder queryByIssuerContextId(String participantContextId) {
        return QuerySpec.Builder.newInstance().filter(filterByIssuerContextId(participantContextId));
    }

    public static Criterion filterByIssuerContextId(String issuerContextId) {
        return new Criterion("issuerContextId", "=", issuerContextId);
    }

    /**
     * The {@link ParticipantContext} that this resource belongs to.
     */

    public String getIssuerContextId() {
        return issuerContextId;
    }

    @Override
    public String getContextId() {
        return issuerContextId;
    }

    public abstract static class Builder<T extends IssuerResource, B extends IssuerResource.Builder<T, B>> {
        protected final T entity;

        protected Builder(T entity) {
            this.entity = entity;
        }

        public abstract B self();

        public B issuerContextId(String issuerContextId) {
            entity.issuerContextId = issuerContextId;
            return self();
        }

        protected T build() {
            return entity;
        }
    }
}