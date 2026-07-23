package com.premtsd.linkedin.platform.datasource;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * HA Postgres read/write routing, applied to any service just by having this
 * starter on the classpath. Configure via:
 *
 *   app.datasource.routing.enabled   (default true; false = plain Boot datasource)
 *   app.datasource.writer.*          Hikari props for the primary  (HAProxy write port)
 *   app.datasource.reader.*          Hikari props for the replicas (HAProxy read port)
 *
 * Read-only transactions go to the reader; everything else — and every read
 * after a write within the same request (see ReadYourWritesFilter) — goes to
 * the writer. Runs before Boot's DataSourceAutoConfiguration so it backs off
 * when this one applies; disable per service with the property above.
 */
@AutoConfiguration(before = DataSourceAutoConfiguration.class)
@ConditionalOnClass(HikariDataSource.class)
@ConditionalOnProperty(name = "app.datasource.routing.enabled", havingValue = "true", matchIfMissing = true)
public class RoutingDataSourceAutoConfiguration {

    @Bean
    @ConfigurationProperties("app.datasource.writer")
    public HikariDataSource writerDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setPoolName("writer-pool");
        return dataSource;
    }

    @Bean
    @ConfigurationProperties("app.datasource.reader")
    public HikariDataSource readerDataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setPoolName("reader-pool");
        dataSource.setReadOnly(true);
        return dataSource;
    }

    // No @ConditionalOnMissingBean(DataSource) here: it would see this
    // configuration's own writer/reader Hikari beans and back off. The
    // opt-out is the app.datasource.routing.enabled property.
    // Pools are injected as parameters — @AutoConfiguration does not proxy
    // bean methods, so calling writerDataSource() directly would create an
    // unmanaged (and unbound) second instance.
    @Bean
    @Primary
    public DataSource dataSource(@Qualifier("writerDataSource") HikariDataSource writerDataSource,
                                 @Qualifier("readerDataSource") HikariDataSource readerDataSource) {
        ReadWriteRoutingDataSource routingDataSource = new ReadWriteRoutingDataSource();
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceType.WRITER, writerDataSource);
        targetDataSources.put(DataSourceType.READER, readerDataSource);
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(writerDataSource);
        routingDataSource.afterPropertiesSet();
        // Lazy proxy: the physical connection (and the WRITER/READER decision)
        // is deferred until the first statement, by which point the
        // transaction's readOnly flag is already set by Spring.
        return new LazyConnectionDataSourceProxy(routingDataSource);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public ReadYourWritesFilter readYourWritesFilter() {
        return new ReadYourWritesFilter();
    }
}
