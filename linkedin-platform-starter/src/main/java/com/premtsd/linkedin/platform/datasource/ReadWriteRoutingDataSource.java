package com.premtsd.linkedin.platform.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Routes read-only transactions (e.g. Spring Data JPA finder methods, which
 * are readOnly by default) to the replica pool (haproxy:5433) and everything
 * else to the primary (haproxy:5432).
 *
 * Read-your-writes: once a write transaction runs on this thread/request,
 * subsequent read-only transactions fall back to the WRITER until the request
 * ends. Cross-request consistency is covered at the DB level by Patroni's
 * synchronous replication with synchronous_commit=remote_apply.
 */
public class ReadWriteRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        boolean readOnlyTx = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        if (readOnlyTx) {
            return DataSourceContextHolder.hasWritten() ? DataSourceType.WRITER : DataSourceType.READER;
        }
        DataSourceContextHolder.markWrite();
        return DataSourceType.WRITER;
    }
}
