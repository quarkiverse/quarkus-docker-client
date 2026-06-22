package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class SaveImageCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    @Test
    public void saveImage() {
        byte[] tar = given()
                .when()
                .queryParam("name", "busybox")
                .get("/docker-image/save")
                .then()
                .statusCode(200)
                .extract()
                .asByteArray();
        Assertions.assertTrue(tar.length > 0);
    }
}
