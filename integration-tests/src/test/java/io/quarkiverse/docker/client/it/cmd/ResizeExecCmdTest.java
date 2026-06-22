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
public class ResizeExecCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    @Test
    public void resizeExec() {
        String containerId = createAndStartContainer("sleep,9999");
        try {
            given()
                    .when()
                    .post("/docker-exec/" + containerId + "/resize-scenario")
                    .then()
                    .statusCode(200)
                    .body("completed", equalTo(true));
        } finally {
            removeContainer(containerId);
        }
    }
}
