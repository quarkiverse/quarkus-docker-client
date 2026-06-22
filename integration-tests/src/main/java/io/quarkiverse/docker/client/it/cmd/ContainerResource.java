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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.ChangeLog;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HealthCheck;
import com.github.dockerjava.api.model.Statistics;

/**
 * Mirrors docker-java's container command integration tests:
 * CreateContainerCmdIT, StartContainerCmdIT, StopContainerCmdIT,
 * KillContainerCmdIT, RestartContainerCmdImplIT, PauseCmdIT, UnpauseCmdIT,
 * RenameContainerCmdIT, WaitContainerCmdIT, RemoveContainerCmdImplIT,
 * ListContainersCmdIT, InspectContainerCmdIT, ContainerDiffCmdIT,
 * ResizeContainerCmdIT, UpdateContainerCmdIT, LogContainerCmdIT, StatsCmdIT,
 * HealthCmdIT, AttachContainerCmdIT, CopyArchiveToContainerCmdIT,
 * CopyArchiveFromContainerCmdIT and CopyFileFromContainerCmdIT. Each docker
 * command is exposed through its own REST endpoint.
 */
@Path("/docker-container")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class ContainerResource {

    @Inject
    DockerClient dockerClient;

    // CreateContainerCmdIT#createContainer
    @POST
    @Path("/create")
    @Produces(MediaType.TEXT_PLAIN)
    public Response create(@QueryParam("image") String image,
            @QueryParam("cmd") String cmd,
            @QueryParam("name") String name,
            @QueryParam("tty") boolean tty,
            @QueryParam("stdinOpen") boolean stdinOpen,
            @QueryParam("user") String user,
            @QueryParam("env") List<String> env,
            @QueryParam("label") List<String> labels,
            @QueryParam("healthcheck") boolean healthcheck) throws InterruptedException {
        try {
            CreateContainerCmd createCmd = dockerClient.createContainerCmd(image);
            if (cmd != null && !cmd.isEmpty()) {
                createCmd.withCmd(cmd.split(","));
            }
            if (name != null && !name.isEmpty()) {
                createCmd.withName(name);
            }
            if (tty) {
                createCmd.withTty(true);
            }
            if (stdinOpen) {
                createCmd.withStdinOpen(true);
            }
            if (user != null && !user.isEmpty()) {
                createCmd.withUser(user);
            }
            if (env != null && !env.isEmpty()) {
                createCmd.withEnv(env);
            }
            if (labels != null && !labels.isEmpty()) {
                Map<String, String> labelMap = new HashMap<>();
                for (String label : labels) {
                    int idx = label.indexOf('=');
                    if (idx > 0) {
                        labelMap.put(label.substring(0, idx), label.substring(idx + 1));
                    }
                }
                createCmd.withLabels(labelMap);
            }
            if (healthcheck) {
                createCmd.withCmd("nc", "-l", "-p", "8080")
                        .withHealthcheck(new HealthCheck()
                                .withTest(Arrays.asList("CMD", "sh", "-c", "netstat -ltn | grep 8080"))
                                .withInterval(TimeUnit.SECONDS.toNanos(1))
                                .withTimeout(TimeUnit.MINUTES.toNanos(1))
                                .withStartPeriod(TimeUnit.SECONDS.toNanos(30))
                                .withRetries(10));
            }
            String id = createCmd.exec().getId();
            return Response.ok(id).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // StartContainerCmdIT#startContainer
    @POST
    @Path("/{id}/start")
    public Response start(@PathParam("id") String id) {
        try {
            dockerClient.startContainerCmd(id).exec();
            return Response.noContent().build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // StopContainerCmdIT#testStopContainer
    @POST
    @Path("/{id}/stop")
    public Response stop(@PathParam("id") String id, @QueryParam("timeout") Integer timeout) {
        try {
            dockerClient.stopContainerCmd(id).withTimeout(timeout == null ? 10 : timeout).exec();
            return Response.noContent().build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // KillContainerCmdIT#killContainer
    @POST
    @Path("/{id}/kill")
    public Response kill(@PathParam("id") String id) {
        try {
            dockerClient.killContainerCmd(id).exec();
            return Response.noContent().build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // RestartContainerCmdImplIT#restartContainer
    @POST
    @Path("/{id}/restart")
    public Response restart(@PathParam("id") String id, @QueryParam("timeout") Integer timeout) {
        try {
            dockerClient.restartContainerCmd(id).withtTimeout(timeout == null ? 10 : timeout).exec();
            return Response.noContent().build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // PauseCmdIT#pauseRunningContainer
    @POST
    @Path("/{id}/pause")
    public Response pause(@PathParam("id") String id) {
        try {
            dockerClient.pauseContainerCmd(id).exec();
            return Response.noContent().build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // UnpauseCmdIT#unpausePausedContainer
    @POST
    @Path("/{id}/unpause")
    public Response unpause(@PathParam("id") String id) {
        try {
            dockerClient.unpauseContainerCmd(id).exec();
            return Response.noContent().build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // RenameContainerCmdIT#renameContainer
    @POST
    @Path("/{id}/rename")
    public Response rename(@PathParam("id") String id, @QueryParam("name") String name) {
        try {
            dockerClient.renameContainerCmd(id).withName(name).exec();
            return Response.noContent().build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // WaitContainerCmdIT#testWaitContainer
    @POST
    @Path("/{id}/wait")
    @Produces(MediaType.TEXT_PLAIN)
    public Response waitContainer(@PathParam("id") String id) {
        try {
            int exitCode = dockerClient.waitContainerCmd(id).start().awaitStatusCode();
            return Response.ok(String.valueOf(exitCode)).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // RemoveContainerCmdImplIT#removeContainer
    @DELETE
    @Path("/{id}")
    public Response remove(@PathParam("id") String id, @QueryParam("force") boolean force) {
        try {
            dockerClient.removeContainerCmd(id).withForce(force).exec();
            return Response.noContent().build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // ListContainersCmdIT#testListContainers
    @GET
    public Response list(@QueryParam("label") List<String> labels) {
        var cmd = dockerClient.listContainersCmd().withShowAll(true);
        if (labels != null && !labels.isEmpty()) {
            cmd.withLabelFilter(labels);
        }
        List<Container> containers = cmd.exec();
        return Response.ok(containers).build();
    }

    // InspectContainerCmdIT#inspectContainer
    @GET
    @Path("/{id}/inspect")
    public Response inspect(@PathParam("id") String id, @QueryParam("size") boolean size) {
        try {
            InspectContainerResponse response = dockerClient.inspectContainerCmd(id).withSize(size).exec();
            return Response.ok(response).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // ContainerDiffCmdIT#testContainerDiff
    @GET
    @Path("/{id}/diff")
    public Response diff(@PathParam("id") String id) {
        try {
            List<ChangeLog> diff = dockerClient.containerDiffCmd(id).exec();
            return Response.ok(diff).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // ResizeContainerCmdIT#resizeContainerTtyTest
    @POST
    @Path("/{id}/resize")
    public Response resize(@PathParam("id") String id,
            @QueryParam("height") int height,
            @QueryParam("width") int width) {
        try {
            dockerClient.resizeContainerCmd(id).withSize(height, width).exec();
            return Response.noContent().build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // UpdateContainerCmdIT#updateContainer
    @POST
    @Path("/{id}/update")
    public Response update(@PathParam("id") String id) {
        try {
            dockerClient.updateContainerCmd(id)
                    .withCpuShares(512)
                    .withCpuPeriod(100000L)
                    .withCpuQuota(50000L)
                    .withCpusetMems("0")
                    .exec();
            return Response.noContent().build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // LogContainerCmdIT#asyncMultipleLogContainer : collect container logs as text
    @GET
    @Path("/{id}/logs")
    @Produces(MediaType.TEXT_PLAIN)
    public Response logs(@PathParam("id") String id) throws InterruptedException {
        final StringBuilder log = new StringBuilder();
        dockerClient.logContainerCmd(id)
                .withStdOut(true)
                .withStdErr(true)
                .withTailAll()
                .exec(new ResultCallback.Adapter<Frame>() {
                    @Override
                    public void onNext(Frame frame) {
                        log.append(new String(frame.getPayload(), StandardCharsets.UTF_8));
                    }
                })
                .awaitCompletion(10, TimeUnit.SECONDS);
        return Response.ok(log.toString()).build();
    }

    // StatsCmdIT#testStatsNoStreaming : return a single stats sample
    @GET
    @Path("/{id}/stats")
    public Response stats(@PathParam("id") String id) throws InterruptedException, IOException {
        final List<Statistics> collected = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);
        try (ResultCallback.Adapter<Statistics> callback = dockerClient.statsCmd(id)
                .withNoStream(true)
                .exec(new ResultCallback.Adapter<Statistics>() {
                    @Override
                    public void onNext(Statistics stats) {
                        if (stats != null) {
                            collected.add(stats);
                        }
                        latch.countDown();
                    }
                })) {
            latch.await(10, TimeUnit.SECONDS);
        }
        if (collected.isEmpty()) {
            return Response.noContent().build();
        }
        return Response.ok(collected.get(0)).build();
    }

    // HealthCmdIT#healthiness : poll until the container becomes healthy
    @GET
    @Path("/{id}/health")
    @Produces(MediaType.TEXT_PLAIN)
    public Response health(@PathParam("id") String id) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(60);
        String status = null;
        while (System.currentTimeMillis() < deadline) {
            InspectContainerResponse inspect = dockerClient.inspectContainerCmd(id).exec();
            if (inspect.getState().getHealth() != null) {
                status = inspect.getState().getHealth().getStatus();
                if ("healthy".equals(status)) {
                    break;
                }
            }
            Thread.sleep(1000);
        }
        return Response.ok(status).build();
    }

    // CopyArchiveToContainerCmdIT#copyStreamToContainer : write a file then copy it in
    @POST
    @Path("/{id}/copy-to")
    public Response copyTo(@PathParam("id") String id,
            @QueryParam("remotePath") String remotePath,
            @QueryParam("fileName") String fileName,
            @QueryParam("content") String content) throws Exception {
        File dir = Files.createTempDirectory("docker-java-copy").toFile();
        File file = new File(dir, fileName);
        Files.write(file.toPath(), (content == null ? "" : content).getBytes(StandardCharsets.UTF_8));
        try {
            dockerClient.copyArchiveToContainerCmd(id)
                    .withRemotePath(remotePath == null ? "/" : remotePath)
                    .withHostResource(file.getAbsolutePath())
                    .exec();
            return Response.noContent().build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } finally {
            file.delete();
            dir.delete();
        }
    }

    // CopyArchiveFromContainerCmdIT#copyFromContainer : export a path as a tar stream
    @GET
    @Path("/{id}/copy-from")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response copyFrom(@PathParam("id") String id, @QueryParam("resource") String resource) {
        try {
            final InputStream in = dockerClient.copyArchiveFromContainerCmd(id, resource).exec();
            StreamingOutput out = output -> {
                try (InputStream stream = in) {
                    stream.transferTo(output);
                }
            };
            return Response.ok(out).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // CopyFileFromContainerCmdIT#copyFromNonExistingContainer (deprecated cmd)
    @GET
    @Path("/{id}/copy-file-from")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response copyFileFrom(@PathParam("id") String id, @QueryParam("resource") String resource) {
        try {
            final InputStream in = dockerClient.copyFileFromContainerCmd(id, resource).exec();
            StreamingOutput out = output -> {
                try (InputStream stream = in) {
                    stream.transferTo(output);
                }
            };
            return Response.ok(out).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // AttachContainerCmdIT#attachContainerWithoutTTY : attach to a container and
    // collect its output. The whole flow runs server-side because attaching needs
    // to be wired up before the container is started.
    @POST
    @Path("/attach-scenario")
    @Produces(MediaType.TEXT_PLAIN)
    public Response attachScenario(@QueryParam("snippet") String snippet) throws Exception {
        String text = (snippet == null || snippet.isEmpty()) ? "hello world" : snippet;
        CreateContainerResponse container = dockerClient.createContainerCmd("busybox:latest")
                .withCmd("echo", text)
                .withTty(false)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .exec();

        final StringBuilder collected = new StringBuilder();
        try {
            ResultCallback.Adapter<Frame> callback = dockerClient.attachContainerCmd(container.getId())
                    .withStdErr(true)
                    .withStdOut(true)
                    .withFollowStream(true)
                    .withLogs(true)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            collected.append(new String(frame.getPayload(), StandardCharsets.UTF_8));
                        }
                    });
            callback.awaitStarted(5, TimeUnit.SECONDS);
            dockerClient.startContainerCmd(container.getId()).exec();
            callback.awaitCompletion(30, TimeUnit.SECONDS);
            callback.close();
            return Response.ok(collected.toString()).build();
        } finally {
            try {
                dockerClient.removeContainerCmd(container.getId()).withForce(true).exec();
            } catch (Exception ignored) {
            }
        }
    }
}
