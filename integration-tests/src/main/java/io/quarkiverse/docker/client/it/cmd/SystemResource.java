/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

/**
 * Mirrors docker-java's InfoCmdIT, VersionCmdIT, PingCmdIT and EventsCmdIT.
 * Each docker command is exposed through its own REST endpoint.
 */
@Path("/docker-system")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class SystemResource {

    private static final String EVENTS_IMAGE = "busybox:latest";

    @Inject
    DockerClient dockerClient;

    // InfoCmdIT#infoTest
    @GET
    @Path("/info")
    public Response info() {
        Info info = dockerClient.infoCmd().exec();
        return Response.ok(info).build();
    }

    // VersionCmdIT#version
    @GET
    @Path("/version")
    public Response version() {
        Version version = dockerClient.versionCmd().exec();
        return Response.ok(version).build();
    }

    // PingCmdIT#ping
    @GET
    @Path("/ping")
    public Response ping() {
        dockerClient.pingCmd().exec();
        return Response.noContent().build();
    }

    // EventsCmdIT#testEventStreamTimeBound : generate events inside a time window
    // and collect them via a since/until bound events stream.
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
        // Only pull when the image is not already cached locally (avoids hitting
        // the registry on every run).
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
