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
import java.util.List;

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

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateNetworkResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Network;

/**
 * Mirrors docker-java's CreateNetworkCmdIT, InspectNetworkCmdIT,
 * ListNetworksCmdIT, RemoveNetworkCmdIT, ConnectToNetworkCmdIT and
 * DisconnectFromNetworkCmdIT. Each docker command is exposed through its own
 * REST endpoint.
 */
@Path("/docker-network")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class NetworkResource {

    @Inject
    DockerClient dockerClient;

    // CreateNetworkCmdIT#createNetwork
    @POST
    @Path("/{name}")
    public Response createNetwork(@PathParam("name") String name) {
        CreateNetworkResponse response = dockerClient.createNetworkCmd().withName(name).exec();
        return Response.ok(response).build();
    }

    // CreateNetworkCmdIT#createNetworkWithIpamConfig
    @POST
    @Path("/{name}/ipam")
    public Response createNetworkWithIpam(@PathParam("name") String name, @QueryParam("subnet") String subnet) {
        Network.Ipam ipam = new Network.Ipam().withConfig(new Network.Ipam.Config().withSubnet(subnet));
        CreateNetworkResponse response = dockerClient.createNetworkCmd().withName(name).withIpam(ipam).exec();
        return Response.ok(response).build();
    }

    // CreateNetworkCmdIT#createNetworkWithLabel
    @POST
    @Path("/{name}/label")
    public Response createNetworkWithLabel(@PathParam("name") String name) {
        CreateNetworkResponse response = dockerClient.createNetworkCmd()
                .withName(name)
                .withLabels(Collections.singletonMap("com.example.usage", "test"))
                .exec();
        return Response.ok(response).build();
    }

    // CreateNetworkCmdIT#createAttachableNetwork
    @POST
    @Path("/{name}/attachable")
    public Response createAttachableNetwork(@PathParam("name") String name) {
        CreateNetworkResponse response = dockerClient.createNetworkCmd()
                .withName(name)
                .withAttachable(true)
                .exec();
        return Response.ok(response).build();
    }

    // ListNetworksCmdIT#listNetworks
    @GET
    public Response listNetworks() {
        List<Network> networks = dockerClient.listNetworksCmd().exec();
        return Response.ok(networks).build();
    }

    // InspectNetworkCmdIT#inspectNetwork
    @GET
    @Path("/{networkId}")
    public Response inspectNetwork(@PathParam("networkId") String networkId) {
        try {
            Network network = dockerClient.inspectNetworkCmd().withNetworkId(networkId).exec();
            return Response.ok(network).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // RemoveNetworkCmdIT#removeNetwork / removeNonExistingContainer
    @DELETE
    @Path("/{networkId}")
    public Response removeNetwork(@PathParam("networkId") String networkId) {
        try {
            dockerClient.removeNetworkCmd(networkId).exec();
            return Response.noContent().build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // ConnectToNetworkCmdIT#connectToNetwork
    @POST
    @Path("/{networkId}/connect/{containerId}")
    public Response connectToNetwork(@PathParam("networkId") String networkId,
            @PathParam("containerId") String containerId) {
        dockerClient.connectToNetworkCmd().withNetworkId(networkId).withContainerId(containerId).exec();
        return Response.noContent().build();
    }

    // DisconnectFromNetworkCmdIT#disconnectFromNetwork
    @POST
    @Path("/{networkId}/disconnect/{containerId}")
    public Response disconnectFromNetwork(@PathParam("networkId") String networkId,
            @PathParam("containerId") String containerId,
            @QueryParam("force") boolean force) {
        dockerClient.disconnectFromNetworkCmd()
                .withNetworkId(networkId)
                .withContainerId(containerId)
                .withForce(force)
                .exec();
        return Response.noContent().build();
    }
}
