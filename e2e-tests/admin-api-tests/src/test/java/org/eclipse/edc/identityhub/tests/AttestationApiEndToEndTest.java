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

package org.eclipse.edc.identityhub.tests;

import io.restassured.http.Header;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerServiceEndToEndExtension;
import org.eclipse.edc.identityhub.tests.fixtures.issuerservice.IssuerServiceEndToEndTestContext;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionValidatorRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.issuerservice.spi.participant.model.Participant;
import org.eclipse.edc.issuerservice.spi.participant.store.ParticipantStore;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.query.SortOrder;
import org.eclipse.edc.sql.testfixtures.PostgresqlEndToEndExtension;
import org.eclipse.edc.validator.spi.ValidationResult;
import org.eclipse.edc.validator.spi.Violation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.junit.assertions.AbstractResultAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SuppressWarnings("JUnitMalformedDeclaration")
public class AttestationApiEndToEndTest {
    abstract static class Tests {

        private static String token = "";

        @BeforeAll
        static void setup(IssuerServiceEndToEndTestContext context) {
            token = context.createSuperUser();
            var registry = context.getRuntime().getService(AttestationDefinitionValidatorRegistry.class);
            registry.registerValidator("test-type", def -> ValidationResult.success());
            registry.registerValidator("test-failure-type", def -> ValidationResult.failure(Violation.violation("test", null)));
        }

        @AfterEach
        void teardown(AttestationDefinitionStore store, ParticipantStore participantStore) {
            store.query(QuerySpec.max()).getContent()
                    .forEach(att -> store.deleteById(att.id()));

            participantStore.query(QuerySpec.max()).getContent()
                    .forEach(participant -> participantStore.deleteById(participant.participantId()));
        }

        @Test
        void createAttestationDefinition(IssuerServiceEndToEndTestContext context, AttestationDefinitionStore store) {
            context.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .body(new AttestationDefinition("test-id", "test-type", Map.of("foo", "bar")))
                    .post("/v1alpha/attestations")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(201);

            assertThat(store.resolveDefinition("test-id")).isNotNull();
        }

        @Test
        void createAttestationDefinition_shouldReturn400_whenValidationFails(IssuerServiceEndToEndTestContext context, AttestationDefinitionStore store) {
            context.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .body(new AttestationDefinition("test-id", "test-failure-type", Map.of("foo", "bar")))
                    .post("/v1alpha/attestations")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);

        }

        @Test
        void getForParticipant(IssuerServiceEndToEndTestContext context, AttestationDefinitionStore store, ParticipantStore participantStore) {
            var att1 = new AttestationDefinition("att1", "test-type", Map.of("bar", "baz"));
            var att2 = new AttestationDefinition("att2", "test-type-1", Map.of("bar", "baz"));
            var p = new Participant("foobar", "did:web:foobar", "Foo Bar", List.of("att1", "att2"));
            var r = store.create(att1).compose(v -> store.create(att2)).compose(participant -> participantStore.create(p));
            assertThat(r).isSucceeded();

            context.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .get("/v1alpha/attestations?participantId=foobar")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", equalTo(2))
                    .body("[0].id", equalTo("att1"))
                    .body("[1].id", equalTo("att2"));
        }

        @Test
        void linkAttestation_expect201(IssuerServiceEndToEndTestContext context, AttestationDefinitionStore store, ParticipantStore participantStore) {
            var r = store.create(new AttestationDefinition("att1", "test-type", Map.of("bar", "baz")))
                    .compose(participant -> participantStore.create(new Participant("foobar", "did:web:foobar", "Foo Bar")));
            assertThat(r).isSucceeded();

            context.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .post("/v1alpha/attestations/att1/link?participantId=foobar")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(201);

            assertThat(participantStore.findById("foobar")).isSucceeded()
                    .satisfies(participant -> assertThat(participant.attestations()).containsExactly("att1"));
        }

        @Test
        void linkAttestation_alreadyLinked_expect204(IssuerServiceEndToEndTestContext context, AttestationDefinitionStore store, ParticipantStore participantStore) {
            var r = store.create(new AttestationDefinition("att1", "test-type", Map.of("bar", "baz")))
                    .compose(participant -> participantStore.create(new Participant("foobar", "did:web:foobar", "Foo Bar", singletonList("att1"))));
            assertThat(r).isSucceeded();

            context.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .post("/v1alpha/attestations/att1/link?participantId=foobar")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(204);

            assertThat(participantStore.findById("foobar")).isSucceeded()
                    .satisfies(participant -> assertThat(participant.attestations()).containsExactly("att1"));
        }

        @Test
        void queryAttestations(IssuerServiceEndToEndTestContext context, AttestationDefinitionStore store, ParticipantStore participantStore) {
            var p1 = new Participant("p1", "did:web:foobar", "Foo Bar", singletonList("att1"));
            var p2 = new Participant("p2", "did:web:barbaz", "Bar Baz", List.of("att1", "att2"));

            var r = participantStore.create(p1).compose(participant -> participantStore.create(p2));
            assertThat(r).isSucceeded();
            store.create(new AttestationDefinition("att1", "test-type", Map.of("key1", "val1")));
            store.create(new AttestationDefinition("att2", "test-type-1", Map.of("key2", "val2")));

            //query by attestation type
            context.getAdminEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(new Header("x-api-key", token))
                    .body(QuerySpec.Builder.newInstance()
                            .sortField("id")
                            .sortOrder(SortOrder.ASC)
                            .filter(new Criterion("attestationType", "=", "test-type"))
                            .build())
                    .post("/v1alpha/attestations/query")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(200)
                    .body("size()", equalTo(1))
                    .body("[0].id", equalTo("att1"));

        }
    }

    @Nested
    @EndToEndTest
    @ExtendWith(IssuerServiceEndToEndExtension.InMemory.class)
    class InMemory extends Tests {
    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {

        @Order(0)
        @RegisterExtension
        static final PostgresqlEndToEndExtension POSTGRESQL_EXTENSION = new PostgresqlEndToEndExtension();

        private static final String ISSUER = "issuer";

        @Order(1)
        @RegisterExtension
        static final BeforeAllCallback POSTGRES_CONTAINER_STARTER = context -> {
            POSTGRESQL_EXTENSION.createDatabase(ISSUER);
        };

        @Order(2)
        @RegisterExtension
        static final IssuerServiceEndToEndExtension ISSUER_SERVICE = IssuerServiceEndToEndExtension.Postgres
                .withConfig(cfg -> POSTGRESQL_EXTENSION.configFor(ISSUER));
    }
}
