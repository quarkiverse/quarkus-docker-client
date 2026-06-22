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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectExecResponse;
import com.github.dockerjava.api.model.Frame;

/**
 * Mirrors docker-java's ExecCreateCmdImplIT, ExecStartCmdIT, InspectExecCmdIT
 * and ResizeExecCmdIT. Each docker command is exposed through its own REST
 * endpoint.
 */
@Path("/docker-exec")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class ExecResource {

    private static final int TTY_HEIGHT = 30;
    private static final int TTY_WIDTH = 120;

    @Inject
    DockerClient dockerClient;

    // ExecCreateCmdImplIT#execCreateTest
    @POST
    @Path("/{containerId}/create")
    @Produces(MediaType.TEXT_PLAIN)
    public Response execCreate(@PathParam("containerId") String containerId,
            @QueryParam("cmd") String cmd,
            @QueryParam("tty") boolean tty) {
        ExecCreateCmdResponse response = dockerClient.execCreateCmd(containerId)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withTty(tty)
                .withCmd(cmd.split(","))
                .exec();
        return Response.ok(response.getId()).build();
    }

    // ExecStartCmdIT#execStart : run the exec to completion
    @POST
    @Path("/{execId}/start")
    public Response execStart(@PathParam("execId") String execId) throws InterruptedException {
        dockerClient.execStartCmd(execId)
                .withDetach(false)
                .exec(new ResultCallback.Adapter<>())
                .awaitCompletion();
        return Response.noContent().build();
    }

    // InspectExecCmdIT#inspectExec
    @GET
    @Path("/{execId}/inspect")
    public Response inspectExec(@PathParam("execId") String execId) {
        InspectExecResponse response = dockerClient.inspectExecCmd(execId).exec();
        return Response.ok(response).build();
    }

    // ResizeExecCmdIT#resizeExecInstanceTtyTest : run the full resize scenario and
    // report whether the exec completed once its tty was resized to the target size.
    @POST
    @Path("/{containerId}/resize-scenario")
    public Response resizeScenario(@PathParam("containerId") String containerId) throws InterruptedException {
        ExecCreateCmdResponse exec = dockerClient.execCreateCmd(containerId)
                .withTty(true)
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd("sh", "-c",
                        String.format("until stty size | grep '%d %d'; do : ; done", TTY_HEIGHT, TTY_WIDTH))
                .exec();

        ResultCallback.Adapter<Frame> callback = dockerClient.execStartCmd(exec.getId())
                .exec(new ResultCallback.Adapter<>());
        callback.awaitStarted();

        dockerClient.resizeExecCmd(exec.getId()).withSize(TTY_HEIGHT, TTY_WIDTH).exec();

        boolean completed = callback.awaitCompletion(10, TimeUnit.SECONDS);

        Map<String, Boolean> result = Collections.singletonMap("completed", completed);
        return Response.ok(result).build();
    }
}
