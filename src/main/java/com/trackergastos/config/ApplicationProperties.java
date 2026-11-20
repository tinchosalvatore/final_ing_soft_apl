package com.trackergastos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Tracker Gastos.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link tech.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private final Liquibase liquibase = new Liquibase();

    private final Registro registro = new Registro();

    // jhipster-needle-application-properties-property

    public Liquibase getLiquibase() {
        return liquibase;
    }

    public Registro getRegistro() {
        return registro;
    }

    // jhipster-needle-application-properties-property-getter

    public static class Liquibase {

        private Boolean asyncStart = true;

        public Boolean getAsyncStart() {
            return asyncStart;
        }

        public void setAsyncStart(Boolean asyncStart) {
            this.asyncStart = asyncStart;
        }
    }

    /**
     * Registro público de cuentas (spec 001, regla 16). {@code false} en prod real: las cuentas las crea un ADMIN.
     * Lo lee {@code UserService} con {@code @Value} (ArchUnit no deja que service dependa de config).
     */
    public static class Registro {

        private boolean habilitado = true;

        public boolean isHabilitado() {
            return habilitado;
        }

        public void setHabilitado(boolean habilitado) {
            this.habilitado = habilitado;
        }
    }

    // jhipster-needle-application-properties-property-class
}
