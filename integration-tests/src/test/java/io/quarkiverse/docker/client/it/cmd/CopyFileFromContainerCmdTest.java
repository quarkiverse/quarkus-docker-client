package io.quarkiverse.docker.client.it.cmd;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class CopyFileFromContainerCmdTest {

    @Test
    public void copyFileFromNonExistingContainer() {
        given().queryParam("resource", "/test").get("/docker-container/non-existing/copy-file-from").then()
                .statusCode(404);
    }
}
