package io.quarkiverse.docker.client.it.cmd;

import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.createAndStartContainer;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.ensureBusybox;
import static io.quarkiverse.docker.client.it.cmd.CmdTestSupport.removeContainer;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class UpdateContainerCmdTest {

    @BeforeEach
    public void setUp() {
        ensureBusybox();
    }

    @Test
    public void updateContainer() {
        String id = createAndStartContainer("sleep,9999");
        try {
            given().post("/docker-container/" + id + "/update").then().statusCode(204);
            given()
                    .when()
                    .get("/docker-container/" + id + "/inspect")
                    .then()
                    .statusCode(200)
                    .body("HostConfig.CpuShares", equalTo(512))
                    .body("HostConfig.CpuPeriod", equalTo(100000))
                    .body("HostConfig.CpuQuota", equalTo(50000))
                    .body("HostConfig.CpusetMems", equalTo("0"));
        } finally {
            removeContainer(id);
        }
    }
}
