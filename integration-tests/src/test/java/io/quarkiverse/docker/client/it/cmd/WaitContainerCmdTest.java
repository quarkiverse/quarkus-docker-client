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
public class WaitContainerCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    @Test
    public void waitContainer() {
        String id = createAndStartContainer("true");
        try {
            given()
                    .when()
                    .post("/docker-container/" + id + "/wait")
                    .then()
                    .statusCode(200)
                    .body(equalTo("0"));
        } finally {
            removeContainer(id);
        }
    }

    @Test
    public void waitNonExistingContainer() {
        given().post("/docker-container/non-existing/wait").then().statusCode(404);
    }
}
