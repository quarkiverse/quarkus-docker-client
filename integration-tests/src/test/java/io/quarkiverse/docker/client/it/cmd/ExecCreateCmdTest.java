package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.createAndStartContainer;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeContainer;
import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ExecCreateCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    @Test
    public void execCreate() {
        String containerId = createAndStartContainer("sleep,9999");
        try {
            String execId = given()
                    .when()
                    .queryParam("cmd", "touch,file.log")
                    .post("/docker-exec/" + containerId + "/create")
                    .then()
                    .statusCode(200)
                    .extract()
                    .asString();
            Assertions.assertFalse(execId.isEmpty());
        } finally {
            removeContainer(containerId);
        }
    }
}
