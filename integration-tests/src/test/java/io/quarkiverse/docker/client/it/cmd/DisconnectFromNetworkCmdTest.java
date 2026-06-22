package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.createAndStartContainer;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.createNetwork;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeContainer;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeNetwork;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class DisconnectFromNetworkCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    @Test
    public void disconnectFromNetwork() {
        String containerId = createAndStartContainer("sleep,9999");
        String networkId = createNetwork("disconnectNetwork-" + System.nanoTime());
        try {
            given().post("/docker-network/" + networkId + "/connect/" + containerId).then().statusCode(204);
            given().get("/docker-network/" + networkId).then().body("Containers", hasKey(containerId));

            given()
                    .when()
                    .post("/docker-network/" + networkId + "/disconnect/" + containerId)
                    .then()
                    .statusCode(204);

            given()
                    .when()
                    .get("/docker-network/" + networkId)
                    .then()
                    .statusCode(200)
                    .body("Containers", not(hasKey(containerId)));
        } finally {
            removeContainer(containerId);
            removeNetwork(networkId);
        }
    }
}
