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

package org.eclipse.edc.identityhub.tests.fixtures.common;

import static java.lang.String.format;

public abstract class CommonRuntimeConfiguration {

    protected Endpoint didEndpoint;


    public Endpoint getDidEndpoint() {
        return didEndpoint;
    }


    public String didFor(String participantContextId) {
        var didLocation = format("%s%%3A%s", didEndpoint.getUrl().getHost(), didEndpoint.getUrl().getPort());
        return format("did:web:%s:%s", didLocation, participantContextId);
    }

}
