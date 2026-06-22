package io.quarkiverse.docker.client.it.cmd;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class RemoveImageCmdTest {

    @Test
    public void removeNonExistingImage() {
        given()
                .when()
                .queryParam("name", "non-existing")
                .delete("/docker-image")
                .then()
                .statusCode(404);
    }
}
