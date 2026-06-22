package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.createAndStartContainer;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeContainer;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class ListContainersCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    @Test
    public void listContainers() {
        String id = createAndStartContainer("sleep,9999");
        try {
            given()
                    .when()
                    .get("/docker-container")
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("find { it.Id == '" + id + "' }", notNullValue())
                    .body("find { it.Id == '" + id + "' }.Image", startsWith("busybox"));
        } finally {
            removeContainer(id);
        }
    }
}
