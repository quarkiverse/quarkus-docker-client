package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.inspectImage;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeImage;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class PullImageCmdTest {

    @Test
    public void pullImage() {
        String image = "alpine:3.17";
        removeImage(image);

        given()
                .when()
                .queryParam("image", image)
                .post("/docker-image/pull")
                .then()
                .statusCode(204);

        inspectImage(image)
                .statusCode(200)
                .body("Id", not(emptyOrNullString()));
    }
}
