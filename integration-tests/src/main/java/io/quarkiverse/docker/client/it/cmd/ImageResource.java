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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Image;

/**
 * Mirrors docker-java's ListImagesCmdIT, PullImageCmdIT, RemoveImageCmdIT,
 * TagImageCmdIT, CommitCmdIT, SaveImageCmdIT, SaveImagesCmdIT, LoadImageCmdIT
 * and BuildImageCmdIT. Each docker command is exposed through its own REST
 * endpoint.
 */
@Path("/docker-image")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class ImageResource {

    @Inject
    DockerClient dockerClient;

    // ListImagesCmdIT#listImages
    @GET
    public Response listImages() {
        List<Image> images = dockerClient.listImagesCmd().withShowAll(true).exec();
        return Response.ok(images).build();
    }

    // PullImageCmdIT#testPullImage
    @POST
    @Path("/pull")
    public Response pullImage(@QueryParam("image") String image) throws InterruptedException {
        dockerClient.pullImageCmd(image).start().awaitCompletion();
        return Response.noContent().build();
    }

    // Test-setup helper: pull only when the image is not already cached locally.
    // Used by the tests to guarantee an image is present without paying a registry
    // round-trip (and possible timeout) on every run.
    @POST
    @Path("/ensure")
    public Response ensureImage(@QueryParam("image") String image) throws InterruptedException {
        try {
            dockerClient.inspectImageCmd(image).exec();
        } catch (NotFoundException notFound) {
            dockerClient.pullImageCmd(image).start().awaitCompletion();
        }
        return Response.noContent().build();
    }

    // helper used by CommitCmdIT / PullImageCmdIT / BuildImageCmdIT (inspectImageCmd).
    @GET
    @Path("/inspect")
    public Response inspectImage(@QueryParam("name") String name) {
        try {
            InspectImageResponse response = dockerClient.inspectImageCmd(name).exec();
            return Response.ok(response).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // RemoveImageCmdIT#removeImage / removeNonExistingImage
    @DELETE
    public Response removeImage(@QueryParam("name") String name, @QueryParam("force") boolean force) {
        try {
            dockerClient.removeImageCmd(name).withForce(force).exec();
            return Response.noContent().build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // TagImageCmdIT#tagImage / tagNonExistingImage
    @POST
    @Path("/tag")
    public Response tagImage(@QueryParam("image") String image,
            @QueryParam("repository") String repository,
            @QueryParam("tag") String tag) {
        try {
            dockerClient.tagImageCmd(image, repository, tag).exec();
            return Response.noContent().build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // CommitCmdIT#commit
    @POST
    @Path("/commit/{containerId}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response commit(@PathParam("containerId") String containerId) {
        try {
            String imageId = dockerClient.commitCmd(containerId).exec();
            return Response.ok(imageId).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    // CommitCmdIT#commitWithLabels
    @POST
    @Path("/commit/{containerId}/labels")
    @Produces(MediaType.TEXT_PLAIN)
    public Response commitWithLabels(@PathParam("containerId") String containerId) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("label1", "abc");
        labels.put("label2", "123");
        String imageId = dockerClient.commitCmd(containerId).withLabels(labels).exec();
        return Response.ok(imageId).build();
    }

    // SaveImageCmdIT#saveImage
    @GET
    @Path("/save")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response saveImage(@QueryParam("name") String name, @QueryParam("tag") String tag) {
        final InputStream in = (tag == null || tag.isEmpty())
                ? dockerClient.saveImageCmd(name).exec()
                : dockerClient.saveImageCmd(name).withTag(tag).exec();
        StreamingOutput out = output -> {
            try (InputStream stream = in) {
                stream.transferTo(output);
            }
        };
        return Response.ok(out).build();
    }

    // SaveImagesCmdIT#saveNoImages / saveImagesWithNameAndTag
    @GET
    @Path("/save-images")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response saveImages(@QueryParam("repository") String repository, @QueryParam("tag") String tag) {
        final InputStream in = (repository == null || repository.isEmpty())
                ? dockerClient.saveImagesCmd().exec()
                : dockerClient.saveImagesCmd().withImage(repository, tag).exec();
        StreamingOutput out = output -> {
            try (InputStream stream = in) {
                stream.transferTo(output);
            }
        };
        return Response.ok(out).build();
    }

    // LoadImageCmdIT#loadImageFromTar : save an existing image to a tar and load it back.
    @POST
    @Path("/load")
    public Response loadImage(@QueryParam("name") String name) throws Exception {
        byte[] tar;
        try (InputStream saved = dockerClient.saveImageCmd(name).exec();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            saved.transferTo(buffer);
            tar = buffer.toByteArray();
        }
        try (InputStream upload = new ByteArrayInputStream(tar)) {
            dockerClient.loadImageCmd(upload).exec();
        }
        return Response.noContent().build();
    }

    // BuildImageCmdIT#labels : build a minimal image from a generated Dockerfile.
    @POST
    @Path("/build")
    @Produces(MediaType.TEXT_PLAIN)
    public Response buildImage(@QueryParam("from") String from) throws Exception {
        String baseImage = (from == null || from.isEmpty()) ? "busybox:latest" : from;
        File baseDir = Files.createTempDirectory("docker-java-build").toFile();
        try {
            File dockerfile = new File(baseDir, "Dockerfile");
            Files.write(dockerfile.toPath(), ("FROM " + baseImage + "\n").getBytes(StandardCharsets.UTF_8));

            String imageId = dockerClient.buildImageCmd(baseDir)
                    .withNoCache(true)
                    .withLabels(Collections.singletonMap("test", "abc"))
                    .start()
                    .awaitImageId();
            return Response.ok(imageId).build();
        } finally {
            new File(baseDir, "Dockerfile").delete();
            baseDir.delete();
        }
    }
}
