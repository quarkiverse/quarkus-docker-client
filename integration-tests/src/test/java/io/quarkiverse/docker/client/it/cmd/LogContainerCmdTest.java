package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.createAndStartContainer;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeContainer;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class LogContainerCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    @Test
    public void logContainer() {
        String id = createAndStartContainer("/bin/echo,hello world");
        try {
            given().post("/docker-container/" + id + "/wait").then().statusCode(200);
            given()
                    .when()
                    .get("/docker-container/" + id + "/logs")
                    .then()
                    .statusCode(200)
                    .body(containsString("hello world"));
        } finally {
            removeContainer(id);
        }
    }
}
