package io.quarkiverse.docker.client.it.cmd;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class ListVolumesCmdTest {

    private static final String VOLUME_NAME = "volume1";

    @AfterEach
    public void cleanup() {
        CmdTestSupport.removeVolume(VOLUME_NAME);
    }

    @Test
    public void listVolumes() {
        given().post("/docker-volume/" + VOLUME_NAME).then().statusCode(200);

        given()
                .when()
                .get("/docker-volume")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("Volumes.size()", greaterThanOrEqualTo(1))
                .body("Volumes.Name", hasItem(VOLUME_NAME));
    }
}
