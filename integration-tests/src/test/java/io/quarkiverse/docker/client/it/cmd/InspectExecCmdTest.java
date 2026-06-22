package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.createAndStartContainer;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeContainer;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class InspectExecCmdTest {

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
    public void inspectExec() {
        String containerId = createAndStartContainer("sleep,9999");
        try {
            String check1 = execCreate(containerId, "test,-e,/marker");
            given().post("/docker-exec/" + check1 + "/start").then().statusCode(204);
            given()
                    .when()
                    .get("/docker-exec/" + check1 + "/inspect")
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("Running", equalTo(false))
                    .body("ExitCode", equalTo(1));

            String touch = execCreate(containerId, "touch,/marker");
            given().post("/docker-exec/" + touch + "/start").then().statusCode(204);
            given()
                    .when()
                    .get("/docker-exec/" + touch + "/inspect")
                    .then()
                    .statusCode(200)
                    .body("Running", equalTo(false))
                    .body("ExitCode", equalTo(0));

            String check2 = execCreate(containerId, "test,-e,/marker");
            given().post("/docker-exec/" + check2 + "/start").then().statusCode(204);
            given()
                    .when()
                    .get("/docker-exec/" + check2 + "/inspect")
                    .then()
                    .statusCode(200)
                    .body("ExitCode", equalTo(0));
        } finally {
            removeContainer(containerId);
        }
    }
}
