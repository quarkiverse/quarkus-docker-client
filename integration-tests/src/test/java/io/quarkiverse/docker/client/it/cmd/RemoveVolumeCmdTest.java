package io.quarkiverse.docker.client.it.cmd;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class RemoveVolumeCmdTest {

    private static final String VOLUME_NAME = "volume1";

    @AfterEach
    public void cleanup() {
        CmdTestSupport.removeVolume(VOLUME_NAME);
    }

    @Test
    public void removeVolume() {
        given().post("/docker-volume/" + VOLUME_NAME).then().statusCode(200);

        given()
                .when()
                .delete("/docker-volume/" + VOLUME_NAME)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/docker-volume/" + VOLUME_NAME)
                .then()
                .statusCode(404);
    }
}
