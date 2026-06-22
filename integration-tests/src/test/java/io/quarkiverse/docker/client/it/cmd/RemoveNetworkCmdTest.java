package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.createNetwork;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class RemoveNetworkCmdTest {

    @Test
    public void removeNetwork() {
        String id = createNetwork("test-network-" + System.nanoTime());

        given()
                .when()
                .delete("/docker-network/" + id)
                .then()
                .statusCode(204);

        given()
                .when()
                .get("/docker-network")
                .then()
                .statusCode(200)
                .body("find { it.Id == '" + id + "' }", nullValue());
    }

    @Test
    public void removeNonExistingNetwork() {
        given()
                .when()
                .delete("/docker-network/non-existing")
                .then()
                .statusCode(404);
    }
}
