package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.BUSYBOX;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeContainer;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ResizeContainerCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    @Test
    public void resizeContainer() {
        String id = given()
                .when()
                .queryParam("image", BUSYBOX)
                .queryParam("cmd", "sh,-c,until stty size | grep '30 120'; do : ; done")
                .queryParam("tty", true)
                .queryParam("stdinOpen", true)
                .queryParam("user", "root")
                .post("/docker-container/create")
                .then()
                .statusCode(200)
                .extract()
                .asString();
        try {
            given().post("/docker-container/" + id + "/start").then().statusCode(204);
            given().queryParam("height", 30).queryParam("width", 120).post("/docker-container/" + id + "/resize").then()
                    .statusCode(204);
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
}
