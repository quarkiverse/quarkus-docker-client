package io.quarkiverse.docker.client.it.cmd;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;

import io.restassured.response.ValidatableResponse;

/**
 * Shared helpers for the per-command resource tests. These wrap the common
 * docker-container / docker-image setup calls so each command test class stays
 * focused on the single command it verifies.
 */
final class CmdTestSupport {

    static final String BUSYBOX = "busybox:latest";

    private CmdTestSupport() {
    }

    static void ensureBusybox() {
        given().queryParam("image", BUSYBOX).post("/docker-image/ensure").then().statusCode(204);
    }

    static String createContainer(String cmd) {
        return given()
                .queryParam("image", BUSYBOX)
                .queryParam("cmd", cmd)
                .post("/docker-container/create")
                .then()
                .statusCode(200)
                .extract()
                .asString();
    }

    static String createAndStartContainer(String cmd) {
        String id = createContainer(cmd);
        startContainer(id);
        return id;
    }

    static void startContainer(String id) {
        given().post("/docker-container/" + id + "/start").then().statusCode(204);
    }

    static void removeContainer(String id) {
        try {
            given().queryParam("force", true).delete("/docker-container/" + id);
        } catch (Exception ignored) {
        }
    }

    static void removeImage(String name) {
        try {
            given().queryParam("name", name).queryParam("force", true).delete("/docker-image");
        } catch (Exception ignored) {
        }
    }

    static void removeNetwork(String id) {
        try {
            given().delete("/docker-network/" + id);
        } catch (Exception ignored) {
        }
    }

    static void removeVolume(String name) {
        try {
            given().delete("/docker-volume/" + name);
        } catch (Exception ignored) {
        }
    }

    static String createNetwork(String name) {
        return given()
                .post("/docker-network/" + name)
                .then()
                .statusCode(200)
                .body("Id", not(emptyOrNullString()))
                .extract()
                .path("Id");
    }

    static ValidatableResponse inspectImage(String name) {
        return given().queryParam("name", name).when().get("/docker-image/inspect").then();
    }
}
