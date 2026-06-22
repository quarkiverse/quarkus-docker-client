package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.createAndStartContainer;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class RemoveContainerCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    @Test
    public void removeContainer() {
        String id = createAndStartContainer("true");
        given().post("/docker-container/" + id + "/wait").then().statusCode(200);

        given().delete("/docker-container/" + id).then().statusCode(204);

        given()
                .when()
                .get("/docker-container")
                .then()
                .statusCode(200)
                .body("find { it.Id == '" + id + "' }", nullValue());
    }

    @Test
    public void removeNonExistingContainer() {
        given().delete("/docker-container/non-existing").then().statusCode(404);
    }
}
