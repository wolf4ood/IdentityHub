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

package org.eclipse.edc.identityhub.issuance.credentials.attestation.presentation;

import org.eclipse.edc.identityhub.spi.issuance.credentials.model.AttestationDefinition;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PresentationAttestationSourceValidatorTest {

    @Test
    void verify_validate() {
        var validator = new PresentationAttestatonSourceValidator();
        Map<String, Object> configuration = Map.of("credentialType", "TestCredential", "outputClaim", "outputClaim");
        var definition = new AttestationDefinition("presentation1", "presentation", configuration);

        assertThat(validator.validate(definition).succeeded()).isTrue();
    }

    @Test
    void verify_missing_credentialType() {
        var validator = new PresentationAttestatonSourceValidator();
        Map<String, Object> configuration = Map.of("outputClaim", "outputClaim");
        var definition = new AttestationDefinition("presentation1", "presentation", configuration);

        assertThat(validator.validate(definition).succeeded()).isFalse();
    }

    @Test
    void verify_missingOutputClaim() {
        var validator = new PresentationAttestatonSourceValidator();
        Map<String, Object> configuration = Map.of("credentialType", "TestCredential");
        var definition = new AttestationDefinition("presentation1", "presentation", configuration);

        assertThat(validator.validate(definition).succeeded()).isFalse();
    }

}