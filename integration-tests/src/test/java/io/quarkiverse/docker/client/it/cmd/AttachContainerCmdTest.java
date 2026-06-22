package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AttachContainerCmdTest {

    @Test
    public void attachContainer() {
        ensureBusybox();
        given()
                .when()
                .queryParam("snippet", "hello world")
                .post("/docker-container/attach-scenario")
                .then()
                .statusCode(200)
                .body(containsString("hello world"));
    }
}
