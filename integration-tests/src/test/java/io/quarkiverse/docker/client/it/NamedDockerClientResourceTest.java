package io.quarkiverse.docker.client.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NamedDockerClientResourceTest {

    private static final String TEST_IMAGE = "nginx:alpine";
    private final Map<String, String> containerIds = new HashMap<>();
    private static final String[] CLIENT_NAMES = { "default", "client1", "client2" };

    @Container
    public static GenericContainer<?> testContainer = new GenericContainer<>(TEST_IMAGE).withExposedPorts(80);

    @Test
    @Order(1)
    void testListContainersAllClients() {
        for (String clientName : CLIENT_NAMES) {
            given()
                    .when()
                    .get("/named-docker-client/{clientName}/containers", clientName)
                    .then()
                    .statusCode(200)
                    .body("size()", greaterThanOrEqualTo(0));
        }
    }

    @Test
    @Order(2)
    void testCreateContainerAllClients() {
        for (String clientName : CLIENT_NAMES) {
            String containerId = given()
                    .when()
                    .post("/named-docker-client/{clientName}/containers/{imageName}", clientName, TEST_IMAGE)
                    .then()
                    .statusCode(200)
                    .extract()
                    .asString();

            containerIds.put(clientName, containerId);

            // Verify container exists
            given()
                    .when()
                    .get("/named-docker-client/{clientName}/containers", clientName)
                    .then()
                    .statusCode(200)
                    .body("find { it.Id == '" + containerId + "' }", notNullValue());
        }
    }

    @Test
    @Order(3)
    void testStopContainerAllClients() {
        for (String clientName : CLIENT_NAMES) {
            String containerId = containerIds.get(clientName);

            given()
                    .when()
                    .post("/named-docker-client/{clientName}/containers/{containerId}/stop", clientName, containerId)
                    .then()
                    .statusCode(200);

            // Wait for container to stop
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // Ignore
            }

            // Verify container is stopped
            given()
                    .when()
                    .get("/named-docker-client/{clientName}/containers", clientName)
                    .then()
                    .statusCode(200)
                    .body("find { it.Id == '" + containerId + "' }.State", equalTo("exited"));
        }
    }

    @Test
    @Order(4)
    void testRemoveContainerAllClients() {
        for (String clientName : CLIENT_NAMES) {
            String containerId = containerIds.get(clientName);

            given()
                    .when()
                    .delete("/named-docker-client/{clientName}/containers/{containerId}", clientName, containerId)
                    .then()
                    .statusCode(204);

            given()
                    .when()
                    .get("/named-docker-client/{clientName}/containers", clientName)
                    .then()
                    .statusCode(200)
                    .body("find { it.Id == '" + containerId + "' }", nullValue());
        }
    }

    @AfterAll
    void cleanup() {
        // Cleanup any remaining containers
        for (Map.Entry<String, String> entry : containerIds.entrySet()) {
            try {
                given().delete("/named-docker-client/{clientName}/containers/{containerId}", entry.getKey(), entry.getValue());
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }
}
