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

@Path("/docker-network")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class NetworkResource {

    @Inject
    DockerClient dockerClient;

    @POST
    @Path("/{name}")
    public Response createNetwork(@PathParam("name") String name) {
        CreateNetworkResponse response = dockerClient.createNetworkCmd().withName(name).exec();
        return Response.ok(response).build();
    }

    @POST
    @Path("/{name}/ipam")
    public Response createNetworkWithIpam(@PathParam("name") String name, @QueryParam("subnet") String subnet) {
        Network.Ipam ipam = new Network.Ipam().withConfig(new Network.Ipam.Config().withSubnet(subnet));
        CreateNetworkResponse response = dockerClient.createNetworkCmd().withName(name).withIpam(ipam).exec();
        return Response.ok(response).build();
    }

    @POST
    @Path("/{name}/label")
    public Response createNetworkWithLabel(@PathParam("name") String name) {
        CreateNetworkResponse response = dockerClient.createNetworkCmd()
                .withName(name)
                .withLabels(Collections.singletonMap("com.example.usage", "test"))
                .exec();
        return Response.ok(response).build();
    }

    @POST
    @Path("/{name}/attachable")
    public Response createAttachableNetwork(@PathParam("name") String name) {
        CreateNetworkResponse response = dockerClient.createNetworkCmd()
                .withName(name)
                .withAttachable(true)
                .exec();
        return Response.ok(response).build();
    }

    @GET
    public Response listNetworks() {
        List<Network> networks = dockerClient.listNetworksCmd().exec();
        return Response.ok(networks).build();
    }

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

    @POST
    @Path("/{networkId}/connect/{containerId}")
    public Response connectToNetwork(@PathParam("networkId") String networkId,
            @PathParam("containerId") String containerId) {
        dockerClient.connectToNetworkCmd().withNetworkId(networkId).withContainerId(containerId).exec();
        return Response.noContent().build();
    }

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
