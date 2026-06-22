package io.quarkiverse.docker.client.it.cmd;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class ListNetworksCmdTest {

    @Test
    public void listNetworks() {
        given()
                .when()
                .get("/docker-network")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("find { it.Name == 'bridge' }.Scope", equalTo("local"))
                .body("find { it.Name == 'bridge' }.Driver", equalTo("bridge"))
                .body("find { it.Name == 'bridge' }.IPAM.Driver", equalTo("default"));
    }
}
