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
public class ContainerDiffCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    @Test
    public void containerDiff() {
        String id = createAndStartContainer("touch,/test");
        try {
            given().post("/docker-container/" + id + "/wait").then().statusCode(200);
            given()
                    .when()
                    .get("/docker-container/" + id + "/diff")
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("find { it.Path == '/test' }.Kind", equalTo(1));
        } finally {
            removeContainer(id);
        }
    }

    @Test
    public void containerDiffNonExisting() {
        given().get("/docker-container/non-existing/diff").then().statusCode(404);
    }
}
