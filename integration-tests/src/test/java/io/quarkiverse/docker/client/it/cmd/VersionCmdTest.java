package io.quarkiverse.docker.client.it.cmd;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
public class VersionCmdTest {

    @Test
    public void version() {
        given()
                .when()
                .get("/docker-system/version")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("Version", not(emptyOrNullString()))
                .body("GoVersion", not(emptyOrNullString()))
                .body("Version.split('\\\\.').size()", greaterThanOrEqualTo(3));
    }
}
