package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.inspectImage;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeImage;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class BuildImageCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    @Test
    public void buildImage() {
        String imageId = null;
        try {
            imageId = given()
                    .when()
                    .post("/docker-image/build")
                    .then()
                    .statusCode(200)
                    .extract()
                    .asString();

            inspectImage(imageId).statusCode(200).body("Config.Labels.test", equalTo("abc"));
        } finally {
            if (imageId != null) {
                removeImage(imageId);
            }
        }
    }
}
