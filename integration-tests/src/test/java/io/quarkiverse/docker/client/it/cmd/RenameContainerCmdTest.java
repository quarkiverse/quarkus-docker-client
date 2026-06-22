package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.createAndStartContainer;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeContainer;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class RenameContainerCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    @Test
    public void renameContainer() {
        String id = createAndStartContainer("sleep,9999");
        try {
            String newName = "renamed-" + System.nanoTime();
            given().queryParam("name", newName).post("/docker-container/" + id + "/rename").then().statusCode(204);
            given()
                    .when()
                    .get("/docker-container/" + id + "/inspect")
                    .then()
                    .statusCode(200)
                    .body("Name", equalTo("/" + newName));
        } finally {
            removeContainer(id);
        }
    }

    @Test
    public void renameNonExistingContainer() {
        given().queryParam("name", "foo").post("/docker-container/non-existing/rename").then().statusCode(404);
    }
}
