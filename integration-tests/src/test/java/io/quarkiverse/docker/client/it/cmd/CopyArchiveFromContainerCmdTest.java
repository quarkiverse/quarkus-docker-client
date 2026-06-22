package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.createAndStartContainer;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeContainer;
import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class CopyArchiveFromContainerCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    @Test
    public void copyFromContainer() {
        String id = createAndStartContainer("sleep,9999");
        try {
            given()
                    .when()
                    .queryParam("remotePath", "/")
                    .queryParam("fileName", "copyFromContainer")
                    .queryParam("content", "hello")
                    .post("/docker-container/" + id + "/copy-to")
                    .then()
                    .statusCode(204);

            byte[] tar = given()
                    .when()
                    .queryParam("resource", "/copyFromContainer")
                    .get("/docker-container/" + id + "/copy-from")
                    .then()
                    .statusCode(200)
                    .extract()
                    .asByteArray();
            Assertions.assertTrue(tar.length > 0);
        } finally {
            removeContainer(id);
        }
    }

    @Test
    public void copyFromNonExistingContainer() {
        given().queryParam("resource", "/test").get("/docker-container/non-existing/copy-from").then().statusCode(404);
    }
}
