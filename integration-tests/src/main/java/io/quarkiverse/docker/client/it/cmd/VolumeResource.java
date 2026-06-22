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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateVolumeResponse;
import com.github.dockerjava.api.command.InspectVolumeResponse;
import com.github.dockerjava.api.command.ListVolumesResponse;
import com.github.dockerjava.api.exception.NotFoundException;

/**
 * Mirrors docker-java's CreateVolumeCmdIT, InspectVolumeCmdIT, ListVolumesCmdIT
 * and RemoveVolumeCmdIT. Each docker command is exposed through its own REST
 * endpoint.
 */
@Path("/docker-volume")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class VolumeResource {

    @Inject
    DockerClient dockerClient;

    // CreateVolumeCmdIT#createVolume
    @POST
    @Path("/{name}")
    public Response createVolume(@PathParam("name") String name) {
        CreateVolumeResponse response = dockerClient.createVolumeCmd()
                .withName(name)
                .withDriver("local")
                .withLabels(Collections.singletonMap("is-timelord", "yes"))
                .exec();
        return Response.ok(response).build();
    }

    // InspectVolumeCmdIT#inspectVolume / inspectNonExistentVolume
    @GET
    @Path("/{name}")
    public Response inspectVolume(@PathParam("name") String name) {
        try {
            InspectVolumeResponse response = dockerClient.inspectVolumeCmd(name).exec();
            return Response.ok(response).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // ListVolumesCmdIT#listVolumes
    @GET
    public Response listVolumes() {
        ListVolumesResponse response = dockerClient.listVolumesCmd().exec();
        return Response.ok(response).build();
    }

    // RemoveVolumeCmdIT#removeVolume
    @DELETE
    @Path("/{name}")
    public Response removeVolume(@PathParam("name") String name) {
        try {
            dockerClient.removeVolumeCmd(name).exec();
            return Response.noContent().build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
