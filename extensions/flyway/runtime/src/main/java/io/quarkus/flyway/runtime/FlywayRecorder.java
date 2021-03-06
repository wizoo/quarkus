package io.quarkus.flyway.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.migration.JavaMigration;
import org.jboss.logging.Logger;

import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class FlywayRecorder {

    private static final Logger log = Logger.getLogger(FlywayRecorder.class);

    static final List<FlywayContainer> flywayContainers = new ArrayList<>(2);

    public void setApplicationMigrationFiles(Collection<String> migrationFiles) {
        log.debugv("Setting the following application migration files: {0}", migrationFiles);
        QuarkusPathLocationScanner.setApplicationMigrationFiles(migrationFiles);
    }

    public void setApplicationMigrationClasses(Collection<Class<? extends JavaMigration>> migrationClasses) {
        log.debugv("Setting the following application migration classes: {0}", migrationClasses);
        QuarkusPathLocationScanner.setApplicationMigrationClasses(migrationClasses);
    }

    public void setApplicationCallbackClasses(Map<String, Collection<Callback>> callbackClasses) {
        log.debugv("Setting application callbacks: {0} total", callbackClasses.values().size());
        QuarkusPathLocationScanner.setApplicationCallbackClasses(callbackClasses);
    }

    public Supplier<Flyway> flywaySupplier(String dataSourceName) {
        DataSource dataSource = DataSources.fromName(dataSourceName);
        FlywayContainerProducer flywayProducer = Arc.container().instance(FlywayContainerProducer.class).get();
        FlywayContainer flywayContainer = flywayProducer.createFlyway(dataSource, dataSourceName);
        flywayContainers.add(flywayContainer);
        return new Supplier<Flyway>() {
            @Override
            public Flyway get() {
                return flywayContainer.getFlyway();
            }
        };
    }

    public void doStartActions() {
        for (FlywayContainer flywayContainer : flywayContainers) {
            if (flywayContainer.isCleanAtStart()) {
                flywayContainer.getFlyway().clean();
            }
            if (flywayContainer.isMigrateAtStart()) {
                flywayContainer.getFlyway().migrate();
            }
        }
    }
}
