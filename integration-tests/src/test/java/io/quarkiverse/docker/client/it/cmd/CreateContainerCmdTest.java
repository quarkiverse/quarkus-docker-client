package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.BUSYBOX;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeContainer;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class CreateContainerCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    @Test
    public void createContainer() {
        String id = CmdTestSupport.createContainer("sleep,9999");
        try {
            Assertions.assertFalse(id.isEmpty());
            given()
                    .when()
                    .get("/docker-container/" + id + "/inspect")
                    .then()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("Id", equalTo(id))
                    .body("Config.Image", equalTo(BUSYBOX));
        } finally {
            remove(id);
        }
    }

    @Test
    public void createContainerWithNameEnvAndLabels() {
        String name = "createTest-" + System.nanoTime();
        String id = given()
                .when()
                .queryParam("image", BUSYBOX)
                .queryParam("cmd", "env")
                .queryParam("name", name)
                .queryParam("env", "FOO=bar")
                .queryParam("label", "com.example=test")
                .post("/docker-container/create")
                .then()
                .statusCode(200)
                .extract()
                .asString();
        try {
            given()
                    .when()
                    .get("/docker-container/" + id + "/inspect")
                    .then()
                    .statusCode(200)
                    .body("Name", equalTo("/" + name))
                    .body("Config.Env", hasItem("FOO=bar"))
                    .body("Config.Labels.'com.example'", equalTo("test"));
        } finally {
            remove(id);
        }
    }

    @Test
    public void createContainerNonExistingImage() {
        given()
                .when()
                .queryParam("image", "non-existing-image-xyz:latest")
                .post("/docker-container/create")
                .then()
                .statusCode(404);
    }

    private void remove(String id) {
        removeContainer(id);
    }
}
