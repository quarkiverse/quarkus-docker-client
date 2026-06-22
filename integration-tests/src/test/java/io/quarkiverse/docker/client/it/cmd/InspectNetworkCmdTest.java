package io.quarkiverse.docker.client.it.cmd;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class InspectNetworkCmdTest {

    @Test
    public void inspectNetwork() {
        given()
                .when()
                .get("/docker-network/bridge")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("Name", equalTo("bridge"))
                .body("Driver", equalTo("bridge"))
                .body("Scope", equalTo("local"));
    }
}
