/*
 *  Copyright (c) 2023 Metaform Systems, Inc.
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

package org.eclipse.edc.identityhub.did.store.sql;

import org.eclipse.edc.identithub.spi.did.store.DidResourceStore;
import org.eclipse.edc.identityhub.did.store.sql.schema.postgres.PostgresDialectStatements;
import org.eclipse.edc.runtime.metamodel.annotation.Extension;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Provider;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.sql.QueryExecutor;
import org.eclipse.edc.transaction.datasource.spi.DataSourceRegistry;
import org.eclipse.edc.transaction.spi.TransactionContext;

import static org.eclipse.edc.identityhub.did.store.sql.SqlDidResourceStoreExtension.NAME;

@Extension(value = NAME)
public class SqlDidResourceStoreExtension implements ServiceExtension {
    public static final String NAME = "DID Resource SQL Store Extension";

    @Setting(value = "Datasource name for the DidResource database", defaultValue = DataSourceRegistry.DEFAULT_DATASOURCE)
    public static final String DATASOURCE_SETTING_NAME = "edc.datasource.didresource.name";
    @Inject
    private DataSourceRegistry dataSourceRegistry;
    @Inject
    private TransactionContext transactionContext;
    @Inject
    private TypeManager typemanager;
    @Inject
    private QueryExecutor queryExecutor;
    @Inject(required = false)
    private DidResourceStatements statements;


    @Provider
    public DidResourceStore createSqlStore(ServiceExtensionContext context) {
        return new SqlDidResourceStore(dataSourceRegistry, getDataSourceName(context), transactionContext, typemanager.getMapper(),
                queryExecutor, getStatementImpl());
    }

    private DidResourceStatements getStatementImpl() {
        return statements != null ? statements : new PostgresDialectStatements();
    }

    private String getDataSourceName(ServiceExtensionContext context) {
        return context.getConfig().getString(DATASOURCE_SETTING_NAME, DataSourceRegistry.DEFAULT_DATASOURCE);
    }
}
