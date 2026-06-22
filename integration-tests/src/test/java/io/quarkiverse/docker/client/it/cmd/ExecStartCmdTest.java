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
public class ExecStartCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    private String execCreate(String containerId, String cmd) {
        return given()
                .when()
                .queryParam("cmd", cmd)
                .post("/docker-exec/" + containerId + "/create")
                .then()
                .statusCode(200)
                .extract()
                .asString();
    }

    @Test
    public void execStart() {
        String containerId = createAndStartContainer("sleep,9999");
        try {
            String touchExec = execCreate(containerId, "touch,/execStartTest.log");
            given().post("/docker-exec/" + touchExec + "/start").then().statusCode(204);

            String checkExec = execCreate(containerId, "test,-e,/execStartTest.log");
            given().post("/docker-exec/" + checkExec + "/start").then().statusCode(204);
            given()
                    .when()
                    .get("/docker-exec/" + checkExec + "/inspect")
                    .then()
                    .statusCode(200)
                    .body("ExitCode", equalTo(0));
        } finally {
            removeContainer(containerId);
        }
    }
}
