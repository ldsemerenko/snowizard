package com.smoketurner.snowizard.application.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.server.JSONP;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codahale.metrics.annotation.Timed;
import com.smoketurner.snowizard.api.Id;
import com.smoketurner.snowizard.api.protos.SnowizardProtos.SnowizardResponse;
import com.smoketurner.snowizard.application.exceptions.SnowizardException;
import com.smoketurner.snowizard.core.IdWorker;
import com.smoketurner.snowizard.exceptions.InvalidSystemClock;
import com.smoketurner.snowizard.exceptions.InvalidUserAgentError;
import io.dropwizard.jersey.caching.CacheControl;
import io.dropwizard.jersey.errors.ErrorMessage;
import io.dropwizard.jersey.params.IntParam;
import io.dropwizard.jersey.protobuf.ProtocolBufferMediaType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/")
@Api(value = "ids")
public class IdResource {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(IdResource.class);
    private final IdWorker worker;

    /**
     * Constructor
     *
     * @param worker
     *            ID worker
     */
    public IdResource(final IdWorker worker) {
        this.worker = Objects.requireNonNull(worker);
    }

    /**
     * Get a new ID and handle any thrown exceptions
     *
     * @param agent
     *            User Agent
     * @return generated ID
     * @throws SnowizardException
     */
    public long getId(final String agent) {
        try {
            return worker.getId(agent);
        } catch (final InvalidUserAgentError e) {
            LOGGER.error("Invalid user agent ({})", agent);
            throw new SnowizardException(Response.Status.BAD_REQUEST,
                    "Invalid User-Agent header", e);
        } catch (final InvalidSystemClock e) {
            LOGGER.error("Invalid system clock", e);
            throw new SnowizardException(Response.Status.INTERNAL_SERVER_ERROR,
                    e.getMessage(), e);
        }
    }

    /**
     * Get a new ID as plain text
     *
     * @param agent
     *            User Agent
     * @return generated ID
     */
    @GET
    @Timed
    @Produces(MediaType.TEXT_PLAIN)
    @CacheControl(mustRevalidate = true, noCache = true, noStore = true)
    @ApiOperation(value = "Get ID", notes = "Return a single ID",
                  response = String.class)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid user-agent",
                         response = ErrorMessage.class),
            @ApiResponse(code = 500, message = "Unable to generate ID",
                         response = ErrorMessage.class) })
    public String getIdAsString(
            @ApiParam(value = "user-agent",
                      required = true) @HeaderParam(HttpHeaders.USER_AGENT) final @NotEmpty String agent) {
        return String.valueOf(getId(agent));
    }

    /**
     * Get a new ID as JSON
     *
     * @param agent
     *            User Agent
     * @return generated ID
     */
    @GET
    @Timed
    @JSONP(callback = "callback", queryParam = "callback")
    @Produces({ MediaType.APPLICATION_JSON, "application/javascript" })
    @CacheControl(mustRevalidate = true, noCache = true, noStore = true)
    @ApiOperation(value = "Get ID", notes = "Return a single ID",
                  response = Id.class)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid user-agent",
                         response = ErrorMessage.class),
            @ApiResponse(code = 500, message = "Unable to generate ID",
                         response = ErrorMessage.class) })
    public Id getIdAsJSON(
            @ApiParam(value = "user-agent",
                      required = true) @HeaderParam(HttpHeaders.USER_AGENT) final @NotEmpty String agent) {
        return new Id(getId(agent));
    }

    /**
     * Get one or more IDs as a Google Protocol Buffer response
     *
     * @param agent
     *            User Agent
     * @param count
     *            Number of IDs to return
     * @return generated IDs
     */
    @GET
    @Timed
    @Produces(ProtocolBufferMediaType.APPLICATION_PROTOBUF)
    @CacheControl(mustRevalidate = true, noCache = true, noStore = true)
    @ApiOperation(value = "Get ID", notes = "Return a single ID",
                  response = SnowizardResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid user-agent",
                         response = ErrorMessage.class),
            @ApiResponse(code = 500, message = "Unable to generate ID",
                         response = ErrorMessage.class) })
    public SnowizardResponse getIdAsProtobuf(
            @ApiParam(value = "user-agent",
                      required = true) @HeaderParam(HttpHeaders.USER_AGENT) final @NotEmpty String agent,
            @QueryParam("count") @DefaultValue("1") final IntParam count) {

        final List<Long> ids = new ArrayList<>();
        if (count != null) {
            for (int i = 0; i < count.get(); i++) {
                ids.add(getId(agent));
            }
        } else {
            ids.add(getId(agent));
        }
        return SnowizardResponse.newBuilder().addAllId(ids).build();
    }
}
