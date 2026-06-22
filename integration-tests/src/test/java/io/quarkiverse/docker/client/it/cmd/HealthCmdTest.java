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
public class HealthCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    @Test
    public void healthContainer() {
        String id = given()
                .when()
                .queryParam("image", BUSYBOX)
                .queryParam("healthcheck", true)
                .post("/docker-container/create")
                .then()
                .statusCode(200)
                .extract()
                .asString();
        try {
            given().post("/docker-container/" + id + "/start").then().statusCode(204);
            given()
                    .when()
                    .get("/docker-container/" + id + "/health")
                    .then()
                    .statusCode(200)
                    .body(equalTo("healthy"));
        } finally {
            removeContainer(id);
        }
    }
}
