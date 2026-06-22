package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeNetwork;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class CreateNetworkCmdTest {

    @Test
    public void createNetwork() {
        String id = CmdTestSupport.createNetwork("createNetwork-" + System.nanoTime());
        try {
            given()
                    .when()
                    .get("/docker-network/" + id)
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("Driver", equalTo("bridge"));
        } finally {
            removeNetwork(id);
        }
    }

    @Test
    public void createNetworkWithIpam() {
        String name = "networkIpam-" + System.nanoTime();
        String subnet = "10.67.79.0/24";
        String id = given()
                .when()
                .queryParam("subnet", subnet)
                .post("/docker-network/" + name + "/ipam")
                .then()
                .statusCode(200)
                .extract()
                .path("Id");
        try {
            given()
                    .when()
                    .get("/docker-network/" + id)
                    .then()
                    .statusCode(200)
                    .body("Driver", equalTo("bridge"))
                    .body("IPAM.Config[0].Subnet", equalTo(subnet));
        } finally {
            removeNetwork(id);
        }
    }

    @Test
    public void createNetworkWithLabel() {
        String name = "createNetworkWithLabel-" + System.nanoTime();
        String id = given()
                .when()
                .post("/docker-network/" + name + "/label")
                .then()
                .statusCode(200)
                .extract()
                .path("Id");
        try {
            given()
                    .when()
                    .get("/docker-network/" + id)
                    .then()
                    .statusCode(200)
                    .body("Labels.'com.example.usage'", equalTo("test"));
        } finally {
            removeNetwork(id);
        }
    }

    @Test
    public void createAttachableNetwork() {
        String name = "createAttachableNetwork-" + System.nanoTime();
        String id = given()
                .when()
                .post("/docker-network/" + name + "/attachable")
                .then()
                .statusCode(200)
                .extract()
                .path("Id");
        try {
            given()
                    .when()
                    .get("/docker-network/" + id)
                    .then()
                    .statusCode(200)
                    .body("Attachable", equalTo(true));
        } finally {
            removeNetwork(id);
        }
    }
}
