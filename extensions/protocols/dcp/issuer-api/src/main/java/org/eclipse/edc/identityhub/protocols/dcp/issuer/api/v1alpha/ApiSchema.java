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

package org.eclipse.edc.identityhub.protocols.dcp.issuer.api.v1alpha;

import io.swagger.v3.oas.annotations.media.Schema;

public interface ApiSchema {
    @Schema(name = "ApiErrorDetail", example = ApiErrorDetailSchema.API_ERROR_EXAMPLE)
    record ApiErrorDetailSchema(
            String message,
            String type,
            String path,
            String invalidValue
    ) {
        public static final String API_ERROR_EXAMPLE = """
                {
                    "message": "error message",
                    "type": "ErrorType",
                    "path": "object.error.path",
                    "invalidValue": "this value is not valid"
                }
                """;
    }
}