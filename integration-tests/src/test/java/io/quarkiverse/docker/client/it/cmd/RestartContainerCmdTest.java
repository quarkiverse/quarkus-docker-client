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
public class RestartContainerCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    @Test
    public void restartContainer() {
        String id = createAndStartContainer("sleep,9999");
        try {
            String startedAt1 = given().get("/docker-container/" + id + "/inspect")
                    .then().statusCode(200).extract().path("State.StartedAt");

            given().queryParam("timeout", 2).post("/docker-container/" + id + "/restart").then().statusCode(204);

            given()
                    .when()
                    .get("/docker-container/" + id + "/inspect")
                    .then()
                    .statusCode(200)
                    .body("State.Running", equalTo(true))
                    .body("State.StartedAt", not(equalTo(startedAt1)));
        } finally {
            removeContainer(id);
        }
    }

    @Test
    public void restartNonExistingContainer() {
        given().post("/docker-container/non-existing/restart").then().statusCode(404);
    }
}
