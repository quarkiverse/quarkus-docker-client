package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.BUSYBOX;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.inspectImage;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeImage;
import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class TagImageCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    @Test
    public void tagImage() {
        String tag = String.valueOf(Math.abs(System.nanoTime()));
        String tagged = "docker-java/busybox:" + tag;
        try {
            given()
                    .when()
                    .queryParam("image", BUSYBOX)
                    .queryParam("repository", "docker-java/busybox")
                    .queryParam("tag", tag)
                    .post("/docker-image/tag")
                    .then()
                    .statusCode(204);

            inspectImage(tagged).statusCode(200);
        } finally {
            removeImage(tagged);
        }
    }

    @Test
    public void tagNonExistingImage() {
        given()
                .when()
                .queryParam("image", "non-existing")
                .queryParam("repository", "docker-java/busybox")
                .queryParam("tag", "1")
                .post("/docker-image/tag")
                .then()
                .statusCode(404);
    }
}
