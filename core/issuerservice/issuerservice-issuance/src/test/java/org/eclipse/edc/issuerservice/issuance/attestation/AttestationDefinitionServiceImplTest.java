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

package org.eclipse.edc.issuerservice.issuance.attestation;

import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionValidatorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.issuerservice.spi.participant.model.Participant;
import org.eclipse.edc.issuerservice.spi.participant.store.ParticipantStore;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.ServiceFailure;
import org.eclipse.edc.spi.result.StoreResult;
import org.eclipse.edc.transaction.spi.NoopTransactionContext;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class AttestationDefinitionServiceImplTest {

    private final AttestationDefinitionStore attestationDefinitionStore = mock();
    private final ParticipantStore participantStore = mock();
    private final AttestationDefinitionValidatorRegistry definitionValidatorRegistry = mock();

    private final AttestationDefinitionService attestationDefinitionService = new AttestationDefinitionServiceImpl(new NoopTransactionContext(),
            attestationDefinitionStore,
            participantStore,
            definitionValidatorRegistry);

    @Test
    void createAttestation() {
        when(attestationDefinitionStore.create(any())).thenReturn(StoreResult.success());
        when(definitionValidatorRegistry.validateDefinition(any())).thenReturn(ValidationResult.success());

        assertThat(attestationDefinitionService.createAttestation(new AttestationDefinition("id", "type", Map.of())))
                .isSucceeded();
        verify(attestationDefinitionStore).create(any());
        verifyNoMoreInteractions(attestationDefinitionStore);
    }

    @Test
    void createAttestation_shouldFail_whenValidationFails() {
        when(attestationDefinitionStore.create(any())).thenReturn(StoreResult.success());
        when(definitionValidatorRegistry.validateDefinition(any())).thenReturn(ValidationResult.failure(Violation.violation("failure", "bar")));

        assertThat(attestationDefinitionService.createAttestation(new AttestationDefinition("id", "type", Map.of())))
                .isFailed()
                .detail().contains("failure");
    }

    @Test
    void deleteAttestation() {
        when(attestationDefinitionStore.deleteById(anyString())).thenReturn(StoreResult.success());

        assertThat(attestationDefinitionService.deleteAttestation("id"))
                .isSucceeded();
        verify(attestationDefinitionStore).deleteById(any());
        verifyNoMoreInteractions(attestationDefinitionStore);
    }

    @Test
    void linkAttestation_whenNotLinked() {
        when(participantStore.findById(anyString()))
                .thenReturn(StoreResult.success(new Participant("foo", "did:web:bar", "foo bar")));

        when(attestationDefinitionStore.resolveDefinition(anyString()))
                .thenReturn(new AttestationDefinition("id", "type", Map.of("foo", "bar")));

        when(participantStore.update(any())).thenReturn(StoreResult.success());

        assertThat(attestationDefinitionService.linkAttestation("foo", "id"))
                .isSucceeded()
                .isEqualTo(true); // no, it's not pretty

        verify(participantStore).findById(anyString());
        verify(participantStore).update(any());
        verify(attestationDefinitionStore).resolveDefinition(anyString());
        verifyNoMoreInteractions(participantStore, attestationDefinitionStore);
    }

    @Test
    void linkAttestation_whenAlreadyLinked() {
        when(participantStore.findById(anyString()))
                .thenReturn(StoreResult.success(new Participant("foo", "did:web:bar", "foo bar", List.of("att1"))));

        when(attestationDefinitionStore.resolveDefinition(anyString()))
                .thenReturn(new AttestationDefinition("att1", "type", Map.of("foo", "bar")));


        assertThat(attestationDefinitionService.linkAttestation("att1", "foo"))
                .isSucceeded()
                .isEqualTo(false); // no, it's not pretty

        verify(participantStore).findById(anyString());
        verify(attestationDefinitionStore).resolveDefinition(anyString());
        verifyNoMoreInteractions(participantStore, attestationDefinitionStore);
    }

    @Test
    void linkAttestation_participantNotFound_expectFailure() {
        when(attestationDefinitionStore.resolveDefinition(anyString()))
                .thenReturn(new AttestationDefinition("att1", "type", Map.of("foo", "bar")));
        when(participantStore.findById(anyString()))
                .thenReturn(StoreResult.notFound("foo"));


        assertThat(attestationDefinitionService.linkAttestation("foo", "id"))
                .isFailed()
                .satisfies(f -> assertThat(f.getReason()).isEqualTo(ServiceFailure.Reason.NOT_FOUND));

        verify(attestationDefinitionStore).resolveDefinition(anyString());
        verify(participantStore).findById(anyString());
        verifyNoMoreInteractions(participantStore, attestationDefinitionStore);
    }

    @Test
    void linkAttestation_attestationNotFound_expectFailure() {
        when(attestationDefinitionStore.resolveDefinition(anyString()))
                .thenReturn(null);

        assertThat(attestationDefinitionService.linkAttestation("foo", "id"))
                .isFailed()
                .satisfies(f -> assertThat(f.getReason()).isEqualTo(ServiceFailure.Reason.NOT_FOUND));

        verify(attestationDefinitionStore).resolveDefinition(anyString());
        verifyNoMoreInteractions(participantStore, attestationDefinitionStore);
    }

    @Test
    void unlinkAttestation_whenLinked() {
        when(participantStore.findById(anyString()))
                .thenReturn(StoreResult.success(new Participant("foo", "did:web:bar", "foo bar", List.of("att1"))));

        when(participantStore.update(any())).thenReturn(StoreResult.success());

        assertThat(attestationDefinitionService.unlinkAttestation("att1", "foo"))
                .isSucceeded()
                .isEqualTo(true);

        verify(participantStore).findById(anyString());
        verify(participantStore).update(any());
        verifyNoMoreInteractions(attestationDefinitionStore, participantStore);
    }

    @Test
    void unlinkAttestation_whenNotLinked() {
        when(participantStore.findById(anyString()))
                .thenReturn(StoreResult.success(new Participant("foo", "did:web:bar", "foo bar", List.of("att42"))));

        assertThat(attestationDefinitionService.unlinkAttestation("att1", "foo"))
                .isSucceeded()
                .isEqualTo(false);

        verify(participantStore).findById(anyString());
        verifyNoMoreInteractions(attestationDefinitionStore, participantStore);
    }

    @Test
    void unlinkAttestation_whenParticipantNotFound_expectFailure() {
        when(participantStore.findById(anyString()))
                .thenReturn(StoreResult.notFound("foo"));


        assertThat(attestationDefinitionService.unlinkAttestation("foo", "id"))
                .isFailed()
                .satisfies(f -> assertThat(f.getReason()).isEqualTo(ServiceFailure.Reason.NOT_FOUND));

        verify(participantStore).findById(anyString());
        verifyNoMoreInteractions(participantStore, attestationDefinitionStore);
    }

    @Test
    void getAttestationsForParticipant() {
        when(participantStore.findById(anyString()))
                .thenReturn(StoreResult.success(new Participant("participant-id", "did:web:foo", "foo bar", List.of("1", "2"))));

        when(attestationDefinitionStore.resolveDefinition(anyString()))
                .thenReturn(new AttestationDefinition("1", "type", Map.of()), new AttestationDefinition("2", "type", Map.of()));

        assertThat(attestationDefinitionService.getAttestationsForParticipant("participant-id"))
                .isSucceeded()
                .satisfies(defs -> assertThat(defs).hasSize(2));
    }

    @Test
    void getAttestationsForParticipant_someNotFound() {
        when(participantStore.findById(anyString()))
                .thenReturn(StoreResult.success(new Participant("participant-id", "did:web:foo", "foo bar", List.of("1", "2", "3"))));

        when(attestationDefinitionStore.resolveDefinition(anyString()))
                .thenReturn(
                        new AttestationDefinition("1", "type", Map.of()),
                        new AttestationDefinition("2", "type", Map.of()),
                        null);

        assertThat(attestationDefinitionService.getAttestationsForParticipant("participant-id"))
                .isSucceeded()
                .satisfies(defs -> assertThat(defs).hasSize(2));
    }

    @Test
    void getAttestationsForParticipant_noResult() {
        when(participantStore.findById(anyString()))
                .thenReturn(StoreResult.success(new Participant("participant-id", "did:web:foo", "foo bar", List.of("1", "2"))));

        when(attestationDefinitionStore.resolveDefinition(anyString()))
                .thenReturn(null);

        assertThat(attestationDefinitionService.getAttestationsForParticipant("participant-id"))
                .isSucceeded()
                .satisfies(defs -> assertThat(defs).isEmpty());
    }

    @Test
    void getAttestationsForParticipant_notFound_expectError() {
        when(participantStore.findById(anyString()))
                .thenReturn(StoreResult.notFound("foo"));

        assertThat(attestationDefinitionService.getAttestationsForParticipant("participant-id"))
                .isFailed()
                .detail().isEqualTo("foo");

        verifyNoInteractions(attestationDefinitionStore);
    }

    @Test
    void queryAttestations() {
        when(attestationDefinitionStore.query(any()))
                .thenReturn(StoreResult.success(List.of(new AttestationDefinition("id", "type", Map.of()))));

        assertThat(attestationDefinitionService.queryAttestations(QuerySpec.max()))
                .isSucceeded();

        verify(attestationDefinitionStore).query(any());
        verifyNoMoreInteractions(attestationDefinitionStore);
    }
}