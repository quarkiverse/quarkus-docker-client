package io.quarkiverse.docker.client.it.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.Version;

@Path("/docker-system")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class SystemResource {

    private static final String EVENTS_IMAGE = "busybox:latest";

    @Inject
    DockerClient dockerClient;

    @GET
    @Path("/info")
    public Response info() {
        Info info = dockerClient.infoCmd().exec();
        return Response.ok(info).build();
    }

    @GET
    @Path("/version")
    public Response version() {
        Version version = dockerClient.versionCmd().exec();
        return Response.ok(version).build();
    }

    @GET
    @Path("/ping")
    public Response ping() {
        dockerClient.pingCmd().exec();
        return Response.noContent().build();
    }

    @GET
    @Path("/events")
    public Response events() throws Exception {
        String startTime = epochSeconds();
        generateEvents();
        String endTime = epochSeconds();

        List<Event> events = new ArrayList<>();
        try (ResultCallback.Adapter<Event> callback = dockerClient.eventsCmd()
                .withSince(startTime)
                .withUntil(endTime)
                .exec(new ResultCallback.Adapter<Event>() {
                    @Override
                    public void onNext(Event event) {
                        events.add(event);
                    }
                })) {
            callback.awaitCompletion(30, TimeUnit.SECONDS);
        }
        return Response.ok(events).build();
    }

    private static String epochSeconds() {
        return String.valueOf(System.currentTimeMillis() / 1000);
    }

    private void generateEvents() throws InterruptedException {

        try {
            dockerClient.inspectImageCmd(EVENTS_IMAGE).exec();
        } catch (NotFoundException notFound) {
            dockerClient.pullImageCmd(EVENTS_IMAGE).start().awaitCompletion();
        }
        CreateContainerResponse container = dockerClient.createContainerCmd(EVENTS_IMAGE)
                .withCmd("sleep", "9999")
                .exec();
        dockerClient.startContainerCmd(container.getId()).exec();
        dockerClient.stopContainerCmd(container.getId()).withTimeout(1).exec();
        dockerClient.removeContainerCmd(container.getId()).withForce(true).exec();
    }
}
