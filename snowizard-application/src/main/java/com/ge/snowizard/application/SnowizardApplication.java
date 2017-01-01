package com.ge.snowizard.application;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.ge.snowizard.application.config.SnowizardConfiguration;
import com.ge.snowizard.application.exceptions.SnowizardExceptionMapper;
import com.ge.snowizard.application.health.EmptyHealthCheck;
import com.ge.snowizard.application.resources.IdResource;
import com.ge.snowizard.application.resources.PingResource;
import com.ge.snowizard.application.resources.VersionResource;
import com.ge.snowizard.core.IdWorker;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jersey.protobuf.ProtobufBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.federecio.dropwizard.swagger.SwaggerBundle;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

public class SnowizardApplication extends Application<SnowizardConfiguration> {

    public static void main(final String[] args) throws Exception {
        new SnowizardApplication().run(args);
    }

    @Override
    public String getName() {
        return "snowizard";
    }

    @Override
    public void initialize(final Bootstrap<SnowizardConfiguration> bootstrap) {
        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(),
                new EnvironmentVariableSubstitutor(false)));

        bootstrap.addBundle(new SwaggerBundle<SnowizardConfiguration>() {
            @Override
            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(
                    final SnowizardConfiguration configuration) {
                return configuration.getSwagger();
            }
        });

        bootstrap.addBundle(new ProtobufBundle());
    }

    @Override
    public void run(final SnowizardConfiguration config,
            final Environment environment) throws Exception {

        environment.jersey().register(SnowizardExceptionMapper.class);

        final IdWorker worker = IdWorker
                .builder(config.getWorkerId(), config.getDatacenterId())
                .withMetricRegistry(environment.metrics())
                .withValidateUserAgent(config.validateUserAgent()).build();

        environment.metrics().register(
                MetricRegistry.name(SnowizardApplication.class, "worker_id"),
                (Gauge<Integer>) config::getWorkerId);

        environment.metrics().register(
                MetricRegistry.name(SnowizardApplication.class,
                        "datacenter_id"),
                (Gauge<Integer>) config::getDatacenterId);

        // health check
        environment.healthChecks().register("empty", new EmptyHealthCheck());

        // resources
        environment.jersey().register(new IdResource(worker));
        environment.jersey().register(new PingResource());
        environment.jersey().register(new VersionResource());
    }
}
