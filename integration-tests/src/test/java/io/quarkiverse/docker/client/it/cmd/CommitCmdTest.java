package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.createAndStartContainer;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.inspectImage;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeContainer;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeImage;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class CommitCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    @Test
    public void commit() {
        String containerId = createAndStartContainer("touch,/test");
        String imageId = null;
        try {
            String busyboxId = inspectImage("busybox").statusCode(200).extract().path("Id");

            imageId = given()
                    .when()
                    .post("/docker-image/commit/" + containerId)
                    .then()
                    .statusCode(200)
                    .extract()
                    .asString();

            inspectImage(imageId).statusCode(200).body("Parent", equalTo(busyboxId));
        } finally {
            removeContainer(containerId);
            if (imageId != null) {
                removeImage(imageId);
            }
        }
    }

    @Test
    public void commitWithLabels() {
        String containerId = createAndStartContainer("touch,/test");
        String imageId = null;
        try {
            given().post("/docker-container/" + containerId + "/wait").then().statusCode(200);

            imageId = given()
                    .when()
                    .post("/docker-image/commit/" + containerId + "/labels")
                    .then()
                    .statusCode(200)
                    .extract()
                    .asString();

            inspectImage(imageId)
                    .statusCode(200)
                    .body("Config.Labels.label1", equalTo("abc"))
                    .body("Config.Labels.label2", equalTo("123"));
        } finally {
            removeContainer(containerId);
            if (imageId != null) {
                removeImage(imageId);
            }
        }
    }

    @Test
    public void commitNonExistingContainer() {
        given()
                .when()
                .post("/docker-image/commit/non-existent")
                .then()
                .statusCode(404);
    }
}
