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

@Path("/docker-volume")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class VolumeResource {

    @Inject
    DockerClient dockerClient;

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

    @GET
    public Response listVolumes() {
        ListVolumesResponse response = dockerClient.listVolumesCmd().exec();
        return Response.ok(response).build();
    }

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
