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

package org.eclipse.edc.identityhub;

import org.eclipse.edc.boot.system.injection.ObjectFactory;
import org.eclipse.edc.identityhub.accesstoken.rules.ClaimIsPresentRule;
import org.eclipse.edc.junit.extensions.DependencyInjectionExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.system.configuration.ConfigFactory;
import org.eclipse.edc.token.rules.ExpirationIssuedAtValidationRule;
import org.eclipse.edc.token.rules.NotBeforeValidationRule;
import org.eclipse.edc.token.spi.TokenValidationRulesRegistry;
import org.eclipse.edc.verifiablecredentials.jwt.rules.JtiValidationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.eclipse.edc.identityhub.DefaultServicesExtension.ACCESSTOKEN_JTI_VALIDATION_ACTIVATE;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(DependencyInjectionExtension.class)
class DefaultServicesExtensionTest {
    private final TokenValidationRulesRegistry registry = mock();

    @BeforeEach
    void setUp(ServiceExtensionContext context) {
        context.registerService(TokenValidationRulesRegistry.class, registry);
    }

    @Test
    void initialize_verifyTokenRules(DefaultServicesExtension extension, ServiceExtensionContext context) {
        extension.initialize(context);
        verify(registry).addRule(eq("dcp-si"), isA(ClaimIsPresentRule.class));
        verify(registry).addRule(eq("dcp-si"), isA(ExpirationIssuedAtValidationRule.class));
        verify(registry).addRule(eq("dcp-si"), isA(NotBeforeValidationRule.class));
        verify(registry).addRule(eq("dcp-access-token"), isA(ClaimIsPresentRule.class));
        verifyNoMoreInteractions(registry);
    }

    @Test
    void initialize_verifyTokenRules_withJtiRule(ServiceExtensionContext context, ObjectFactory factory) {
        when(context.getConfig()).thenReturn(ConfigFactory.fromMap(Map.of(ACCESSTOKEN_JTI_VALIDATION_ACTIVATE, Boolean.TRUE.toString())));


        factory.constructInstance(DefaultServicesExtension.class).initialize(context);
        verify(registry).addRule(eq("dcp-si"), isA(ClaimIsPresentRule.class));
        verify(registry).addRule(eq("dcp-si"), isA(ExpirationIssuedAtValidationRule.class));
        verify(registry).addRule(eq("dcp-si"), isA(NotBeforeValidationRule.class));
        verify(registry).addRule(eq("dcp-si"), isA(JtiValidationRule.class));
        verify(registry).addRule(eq("dcp-access-token"), isA(ClaimIsPresentRule.class));
        verify(registry).addRule(eq("dcp-access-token"), isA(JtiValidationRule.class));
        verifyNoMoreInteractions(registry);
    }
}