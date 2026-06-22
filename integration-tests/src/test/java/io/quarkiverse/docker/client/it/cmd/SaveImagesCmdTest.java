package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class SaveImagesCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    @Test
    public void saveImages() {
        byte[] tar = given()
                .when()
                .queryParam("repository", "busybox")
                .queryParam("tag", "latest")
                .get("/docker-image/save-images")
                .then()
                .statusCode(200)
                .extract()
                .asByteArray();
        Assertions.assertTrue(tar.length > 0);
    }
}
