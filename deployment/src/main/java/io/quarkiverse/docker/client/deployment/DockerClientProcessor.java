package io.quarkiverse.docker.client.deployment;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import org.jboss.jandex.DotName;

import com.github.dockerjava.api.DockerClient;

import io.quarkiverse.docker.client.runtime.DockerClientRecorder;
import io.quarkiverse.docker.client.runtime.NamedDockerClient;
import io.quarkiverse.docker.client.runtime.config.DockerRuntimeConfig;
import io.quarkus.arc.BeanDestroyer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedPackageBuildItem;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

class DockerClientProcessor {

    private static final String FEATURE = "docker-client";
    private static final DotName NAMED_DOCKER_CLIENT = DotName.createSimple(NamedDockerClient.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    /**
     * Extracts and processes named Docker clients from the application archives. This includes both explicitly named clients
     * and the default client.
     */
    @BuildStep
    void extractNamedDockerClients(
            ApplicationArchivesBuildItem beanArchiveIndex,
            BuildProducer<DockerClientNamesBuildItem> dockerClientNames) {
        Set<String> namedDockerClients = new HashSet<>(collectNamedDockerClients(beanArchiveIndex));
        namedDockerClients.add(DockerRuntimeConfig.DEFAULT_CLIENT_NAME);
        dockerClientNames.produce(new DockerClientNamesBuildItem(namedDockerClients));
    }

    /**
     * Registers the NamedDockerClient qualifier for dependency injection.
     */
    @BuildStep
    List<AdditionalBeanBuildItem> registerDockerClientName() {
        return Collections.singletonList(
                AdditionalBeanBuildItem.builder()
                        .addBeanClass(NamedDockerClient.class)
                        .build());
    }

    /**
     * Collects all named Docker clients from the application archives.
     *
     * @param beanArchiveIndex The application archives to scan
     * @return Set of Docker client names found in the archives
     */
    private Set<String> collectNamedDockerClients(ApplicationArchivesBuildItem beanArchiveIndex) {
        return beanArchiveIndex.getAllApplicationArchives().stream()
                .map(ApplicationArchive::getIndex)
                .flatMap(archive -> archive.getAnnotations(NAMED_DOCKER_CLIENT).stream())
                .map(annotation -> annotation.value().asString())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Sets up Docker clients at runtime initialization. Creates synthetic beans for both the default client and named
     * clients.
     *
     * @param recorder The Docker client recorder
     * @param clientNames The collected Docker client names
     * @param syntheticBean Producer for synthetic beans
     */
    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    public void setup(
            DockerClientRecorder recorder,
            DockerClientNamesBuildItem clientNames,
            BuildProducer<SyntheticBeanBuildItem> syntheticBean) {

        recorder.initialize(clientNames.getDockerClientNames());

        // Create default Docker client bean
        syntheticBean.produce(createDefaultDockerClientBean(recorder));

        // Create named Docker client beans
        produceNamedDockerClientBeans(syntheticBean, clientNames.getDockerClientNames(), recorder);
    }

    /**
     * Creates the default Docker client synthetic bean configuration.
     *
     * @param recorder The Docker client recorder
     * @return SyntheticBeanBuildItem for the default Docker client
     */
    private SyntheticBeanBuildItem createDefaultDockerClientBean(DockerClientRecorder recorder) {
        return SyntheticBeanBuildItem.configure(DockerClient.class)
                .unremovable()
                .types(DockerClient.class)
                .supplier(recorder.createDockerClientBean())
                .scope(Singleton.class)
                .setRuntimeInit()
                .destroyer(BeanDestroyer.CloseableDestroyer.class)
                .done();
    }

    /**
     * Produces synthetic beans for all named Docker clients.
     *
     * @param syntheticBean Producer for synthetic beans
     * @param clientNames Set of client names to create beans for
     * @param recorder The Docker client recorder
     */
    private void produceNamedDockerClientBeans(
            BuildProducer<SyntheticBeanBuildItem> syntheticBean,
            Set<String> clientNames,
            DockerClientRecorder recorder) {
        clientNames.stream()
                .map(clientName -> syntheticNamedDockerClientBeanFor(clientName, recorder))
                .forEach(syntheticBean::produce);
    }

    /**
     * Creates a synthetic bean configuration for a named Docker client.
     *
     * @param clientName Name of the Docker client
     * @param recorder The Docker client recorder
     * @return SyntheticBeanBuildItem for the named Docker client
     */
    private SyntheticBeanBuildItem syntheticNamedDockerClientBeanFor(String clientName, DockerClientRecorder recorder) {
        return SyntheticBeanBuildItem.configure(DockerClient.class)
                .unremovable()
                .types(DockerClient.class)
                .supplier(recorder.createNamedDockerClientBean(clientName))
                .scope(Singleton.class)
                .addQualifier()
                .annotation(NamedDockerClient.class)
                .addValue("value", clientName)
                .done()
                .setRuntimeInit()
                .destroyer(BeanDestroyer.CloseableDestroyer.class)
                .done();
    }

    /**
     * Adds health check support for Docker clients.
     *
     * @param config The Docker runtime configuration
     * @return HealthBuildItem for Docker client health checks
     */
    @BuildStep
    HealthBuildItem addHealthCheck(DockerRuntimeConfig config) {
        return new HealthBuildItem(
                "io.quarkiverse.docker.client.runtime.health.DockerClientHealthCheck",
                config.enableHealthCheck().orElse(false));
    }

    @BuildStep
    void dockerJavaApiIndex(BuildProducer<IndexDependencyBuildItem> indexDependencyProducer) {
        indexDependencyProducer.produce(Set.of(
                new IndexDependencyBuildItem(
                        "com.github.docker-java", "docker-java-api"),
                new IndexDependencyBuildItem(
                        "com.github.docker-java", "docker-java-core")));
    }

    @BuildStep
    void registerReflection(
            CombinedIndexBuildItem index,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {

        Set<String> classes = new HashSet<>();

        index.getIndex().getKnownClasses().stream()
                .map(ci -> ci.name().toString())
                .filter(name -> name.startsWith("com.github.dockerjava.api") || name.startsWith("com.github.dockerjava.core"))
                .forEach(classes::add);

        reflectiveClasses.produce(
                ReflectiveClassBuildItem.builder(classes.toArray(String[]::new))
                        .constructors()
                        .methods()
                        .fields()
                        .build());

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder("com.github.dockerjava.core.DockerConfigFile")
                .constructors()
                .methods()
                .fields()
                .build());
    }

    @BuildStep
    public void runtimeInitialized(
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClassProducer,
            BuildProducer<RuntimeInitializedPackageBuildItem> runtimeInitializedPackageProducer) {
        runtimeInitializedClassProducer
                .produce(new RuntimeInitializedClassBuildItem(
                        "com.github.dockerjava.transport.NamedPipeSocket$Kernel32"));
        runtimeInitializedPackageProducer
                .produce(new RuntimeInitializedPackageBuildItem(
                        "org.apache.hc.client5.http.impl.auth"));
    }
}
