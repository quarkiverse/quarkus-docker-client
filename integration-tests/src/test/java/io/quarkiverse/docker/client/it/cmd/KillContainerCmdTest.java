package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.createAndStartContainer;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeContainer;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class KillContainerCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    @Test
    public void killContainer() {
        String id = createAndStartContainer("sleep,9999");
        try {
            given().post("/docker-container/" + id + "/kill").then().statusCode(204);
            given()
                    .when()
                    .get("/docker-container/" + id + "/inspect")
                    .then()
                    .statusCode(200)
                    .body("State.Running", equalTo(false))
                    .body("State.ExitCode", not(equalTo(0)));
        } finally {
            removeContainer(id);
        }
    }

    @Test
    public void killNonExistingContainer() {
        given().post("/docker-container/non-existing/kill").then().statusCode(404);
    }
}
