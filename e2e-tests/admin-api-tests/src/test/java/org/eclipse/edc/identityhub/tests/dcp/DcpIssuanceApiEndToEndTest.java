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

package org.eclipse.edc.identityhub.tests.dcp;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import org.eclipse.edc.iam.did.spi.resolution.DidPublicKeyResolver;
import org.eclipse.edc.identityhub.tests.fixtures.IssuerServiceEndToEndExtension;
import org.eclipse.edc.identityhub.tests.fixtures.IssuerServiceEndToEndTestContext;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationDefinitionStore;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSource;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactory;
import org.eclipse.edc.issuerservice.spi.issuance.attestation.AttestationSourceFactoryRegistry;
import org.eclipse.edc.issuerservice.spi.issuance.credentialdefinition.CredentialDefinitionService;
import org.eclipse.edc.issuerservice.spi.issuance.model.AttestationDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.model.CredentialRuleDefinition;
import org.eclipse.edc.issuerservice.spi.issuance.process.store.IssuanceProcessStore;
import org.eclipse.edc.issuerservice.spi.participant.ParticipantService;
import org.eclipse.edc.issuerservice.spi.participant.model.Participant;
import org.eclipse.edc.junit.annotations.EndToEndTest;
import org.eclipse.edc.junit.annotations.PostgresqlIntegrationTest;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static io.restassured.http.ContentType.JSON;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.JwtCreationUtil.generateJwt;
import static org.eclipse.edc.identityhub.verifiablecredentials.testfixtures.VerifiableCredentialTestUtil.generateEcKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DcpIssuanceApiEndToEndTest {

    protected static final DidPublicKeyResolver DID_PUBLIC_KEY_RESOLVER = mock();


    abstract static class Tests {

        public static final String ISSUER_DID = "did:web:issuer";
        public static final String PARTICIPANT_DID = "did:web:participant";
        public static final String DID_WEB_PARTICIPANT_KEY_1 = "did:web:participant#key1";
        public static final ECKey PARTICIPANT_KEY = generateEcKey(DID_WEB_PARTICIPANT_KEY_1);
        protected static final AttestationSourceFactory ATTESTATION_SOURCE_FACTORY = mock();
        private static final String VALID_CREDENTIAL_REQUEST_MESSAGE = """
                {
                  "@context": [
                     "https://w3id.org/dspace-dcp/v1.0/dcp.jsonld"
                  ],
                  "@type": "CredentialRequestMessage",
                  "credentials":[
                    {
                        "credentialType": "MembershipCredential",
                        "format": "vcdm11_jwt"
                    }
                  ]
                }
                """;
        private static final String FAULTY_CREDENTIAL_REQUEST_MESSAGE = """
                {
                  "@context": [
                     "https://w3id.org/dspace-dcp/v1.0/dcp.jsonld"
                  ],
                  "@type": "CredentialRequestMessage"
                }
                """;

        @BeforeAll
        static void beforeAll(IssuerServiceEndToEndTestContext context) {
            var pipelineFactory = context.getRuntime().getService(AttestationSourceFactoryRegistry.class);
            pipelineFactory.registerFactory("Attestation", ATTESTATION_SOURCE_FACTORY);
        }

        @AfterEach
        void teardown(ParticipantService participantService, CredentialDefinitionService credentialDefinitionService) {
            participantService.queryParticipants(QuerySpec.max()).getContent()
                    .forEach(p -> participantService.deleteParticipant(p.participantId()).getContent());

            credentialDefinitionService.queryCredentialDefinitions(QuerySpec.max()).getContent()
                    .forEach(c -> credentialDefinitionService.deleteCredentialDefinition(c.getId()).getContent());
        }

        @Test
        void requestCredential(IssuerServiceEndToEndTestContext context, ParticipantService participantService,
                               CredentialDefinitionService credentialDefinitionService,
                               AttestationDefinitionStore attestationDefinitionStore,
                               IssuanceProcessStore issuanceProcessStore) throws JOSEException {

            participantService.createParticipant(new Participant(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));

            var attestationDefinition = new AttestationDefinition("attestation-id", "Attestation", Map.of());
            attestationDefinitionStore.create(attestationDefinition);

            Map<String, Object> credentialRuleConfiguration = Map.of(
                    "claim", "onboarding.signedDocuments",
                    "operator", "eq",
                    "value", true);

            var credentialDefinition = CredentialDefinition.Builder.newInstance()
                    .id("credential-id")
                    .credentialType("MembershipCredential")
                    .jsonSchemaUrl("https://example.com/schema")
                    .jsonSchema("{}")
                    .attestation("attestation-id")
                    .rule(new CredentialRuleDefinition("expression", credentialRuleConfiguration))
                    .build();

            credentialDefinitionService.createCredentialDefinition(credentialDefinition);

            var token = generateSiToken();

            Map<String, Object> claims = Map.of("onboarding", Map.of("signedDocuments", true));

            var attestationSource = mock(AttestationSource.class);

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(DID_WEB_PARTICIPANT_KEY_1))).thenReturn(Result.success(PARTICIPANT_KEY.toPublicKey()));
            when(ATTESTATION_SOURCE_FACTORY.createSource(eq(attestationDefinition))).thenReturn(attestationSource);
            when(attestationSource.execute(any())).thenReturn(Result.success(claims));

            var location = context.getDcpIssuanceEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                    .post("/v1alpha/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(201)
                    .extract()
                    .header("Location");

            assertThat(location).contains("/v1alpha/requests/");

            var processId = location.substring(location.lastIndexOf('/') + 1);
            var issuanceProcess = issuanceProcessStore.findById(processId);

            assertThat(issuanceProcess).isNotNull()
                    .satisfies(process -> {
                        assertThat(process.getParticipantId()).isEqualTo(PARTICIPANT_DID);
                        assertThat(process.getCredentialDefinitions()).containsExactly("credential-id");
                        assertThat(process.getClaims()).containsAllEntriesOf(claims);
                    });

        }

        @Test
        void requestCredential_validationError_shouldReturn400(IssuerServiceEndToEndTestContext context) {
            var token = generateSiToken();

            context.getDcpIssuanceEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .body(FAULTY_CREDENTIAL_REQUEST_MESSAGE)
                    .post("/v1alpha/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);

        }

        @Test
        void requestCredential_tokenNotPresent_shouldReturn401(IssuerServiceEndToEndTestContext context) {
            context.getDcpIssuanceEndpoint().baseRequest()
                    .contentType(JSON)
                    .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                    .post("/v1alpha/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

        }

        @Test
        void requestCredential_participantNotFound_shouldReturn401(IssuerServiceEndToEndTestContext context) {
            var token = generateSiToken();

            context.getDcpIssuanceEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                    .post("/v1alpha/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

        }

        @Test
        void requestCredential_tokenVerificationFails_shouldReturn401(IssuerServiceEndToEndTestContext context, ParticipantService participantService) throws JOSEException {

            participantService.createParticipant(new Participant(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));

            var spoofedKey = new ECKeyGenerator(Curve.P_256).keyID(DID_WEB_PARTICIPANT_KEY_1).generate();

            var token = generateSiToken();

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(DID_WEB_PARTICIPANT_KEY_1))).thenReturn(Result.success(spoofedKey.toPublicKey()));

            context.getDcpIssuanceEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                    .post("/v1alpha/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

        }

        @Test
        void requestCredential_wrongTokenAudience_shouldReturn401(IssuerServiceEndToEndTestContext context, ParticipantService participantService) throws JOSEException {

            participantService.createParticipant(new Participant(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));

            var token = generateSiToken("wrong-audience");

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(DID_WEB_PARTICIPANT_KEY_1))).thenReturn(Result.success(PARTICIPANT_KEY.toPublicKey()));

            context.getDcpIssuanceEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                    .post("/v1alpha/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

        }

        @Test
        void requestCredential_definitionNotFound_shouldReturn400(IssuerServiceEndToEndTestContext context, ParticipantService participantService) throws JOSEException {

            participantService.createParticipant(new Participant(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));

            var token = generateSiToken();

            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(DID_WEB_PARTICIPANT_KEY_1))).thenReturn(Result.success(PARTICIPANT_KEY.toPublicKey()));

            context.getDcpIssuanceEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                    .post("/v1alpha/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);

        }

        @Test
        void requestCredential_attestationsNotFulfilled_shouldReturn403(IssuerServiceEndToEndTestContext context,
                                                                        ParticipantService participantService,
                                                                        AttestationDefinitionStore attestationDefinitionStore,
                                                                        CredentialDefinitionService credentialDefinitionService) throws JOSEException {

            participantService.createParticipant(new Participant(PARTICIPANT_DID, PARTICIPANT_DID, "Participant"));
            var attestationDefinition = new AttestationDefinition("attestation-id", "Attestation", Map.of());
            attestationDefinitionStore.create(attestationDefinition);

            Map<String, Object> credentialRuleConfiguration = Map.of(
                    "claim", "onboarding.signedDocuments",
                    "operator", "eq",
                    "value", true);


            var credentialDefinition = CredentialDefinition.Builder.newInstance()
                    .id("credential-id")
                    .credentialType("MembershipCredential")
                    .jsonSchemaUrl("https://example.com/schema")
                    .jsonSchema("{}")
                    .attestation("attestation-id")
                    .rule(new CredentialRuleDefinition("expression", credentialRuleConfiguration))
                    .build();

            credentialDefinitionService.createCredentialDefinition(credentialDefinition);
            var token = generateSiToken();

            Map<String, Object> claims = Map.of("onboarding", Map.of("signedDocuments", false));

            var attestationSource = mock(AttestationSource.class);
            when(DID_PUBLIC_KEY_RESOLVER.resolveKey(eq(DID_WEB_PARTICIPANT_KEY_1))).thenReturn(Result.success(PARTICIPANT_KEY.toPublicKey()));
            when(ATTESTATION_SOURCE_FACTORY.createSource(eq(attestationDefinition))).thenReturn(attestationSource);
            when(attestationSource.execute(any())).thenReturn(Result.success(claims));

            context.getDcpIssuanceEndpoint().baseRequest()
                    .contentType(JSON)
                    .header(AUTHORIZATION, token)
                    .body(VALID_CREDENTIAL_REQUEST_MESSAGE)
                    .post("/v1alpha/credentials")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);

        }

        private String generateSiToken() {
            return generateSiToken(ISSUER_DID);
        }

        private String generateSiToken(String audience) {
            return generateJwt(audience, PARTICIPANT_DID, PARTICIPANT_DID, Map.of(), PARTICIPANT_KEY);
        }
    }


    @Nested
    @EndToEndTest
    @Order(1)
    class InMemory extends Tests {


        @RegisterExtension
        static IssuerServiceEndToEndExtension runtime;

        static {
            runtime = new IssuerServiceEndToEndExtension.InMemory();
            runtime.registerServiceMock(DidPublicKeyResolver.class, DID_PUBLIC_KEY_RESOLVER);
        }

    }

    @Nested
    @PostgresqlIntegrationTest
    class Postgres extends Tests {
        
        @RegisterExtension
        static IssuerServiceEndToEndExtension runtime;

        static {
            runtime = new IssuerServiceEndToEndExtension.Postgres();
            runtime.registerServiceMock(DidPublicKeyResolver.class, DID_PUBLIC_KEY_RESOLVER);
        }
    }
}
