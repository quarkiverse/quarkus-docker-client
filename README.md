# Quarkus Docker Client

[![Version](https://img.shields.io/maven-central/v/io.quarkiverse.docker/quarkus-docker-client?logo=apache-maven&style=flat-square)](https://central.sonatype.com/artifact/io.quarkiverse.docker/quarkus-docker-client-parent)

This extension provides a Quarkus integration with the [Docker Java Client](https://github.com/docker-java/docker-java)
library.

## Usage

Add the following dependency to your project:

```xml

<dependency>
    <groupId>io.quarkiverse.docker</groupId>
    <artifactId>quarkus-docker-client</artifactId>
    <version>0.0.5</version>
</dependency>
```

> [!CAUTION]
> This extension currently doesn't support GraalVM native image compilation.