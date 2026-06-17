package io.quarkiverse.docker.client.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@Testcontainers
public class DockerClientResourceTest {

    private static final String TEST_IMAGE = "nginx:alpine";
    private static String containerId;

    @Container
    public static GenericContainer<?> testContainer = new GenericContainer<>(TEST_IMAGE).withExposedPorts(80);

    @Test
    @Order(1)
    public void testGetDockerInfo() {
        given()
                .when()
                .get("/docker-client/info")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("Containers", notNullValue())
                .body("Images", notNullValue());
    }

    @Test
    @Order(2)
    public void testListContainers() {
        given()
                .when()
                .get("/docker-client/containers")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", greaterThanOrEqualTo(0));
    }

    @Test
    @Order(3)
    public void testCreateContainer() {
        containerId = given()
                .when()
                .post("/docker-client/containers/" + TEST_IMAGE)
                .then()
                .statusCode(200)
                .extract()
                .asString();

        // Verify container exists in list
        given()
                .when()
                .get("/docker-client/containers")
                .then()
                .statusCode(200)
                .body("find { it.Id == '" + containerId + "' }", notNullValue());
    }

    @Test
    @Order(4)
    public void testRemoveContainer() {
        given()
                .when()
                .delete("/docker-client/containers/" + containerId)
                .then()
                .statusCode(204);

        // Verify container no longer exists
        given()
                .when()
                .get("/docker-client/containers")
                .then()
                .statusCode(200)
                .body("find { it.id == '" + containerId + "' }", nullValue());
    }

    @AfterAll
    public static void cleanup() {
        if (containerId != null) {
            try {
                given().delete("/docker-client/containers/" + containerId);
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }
}
