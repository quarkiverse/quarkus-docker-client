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
public class StartContainerCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    @Test
    public void startContainer() {
        String id = createAndStartContainer("top");
        try {
            given()
                    .when()
                    .get("/docker-container/" + id + "/inspect")
                    .then()
                    .statusCode(200)
                    .body("State.Running", equalTo(true));
        } finally {
            removeContainer(id);
        }
    }

    @Test
    public void startNonExistingContainer() {
        given().post("/docker-container/non-existing/start").then().statusCode(404);
    }
}
