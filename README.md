# OpsFactor Community

OpsFactor Community is the source-available backend for demand and supply
chain planning. It is an independently buildable Spring Boot application and
never depends on sources or artifacts from OpsFactor Enterprise.

This repository is released under the [Sustainable Use License 1.0](LICENSE.md).
The license is source-available, not an OSI-approved open-source license.
Practical licensing examples are available in the
[Community licensing FAQ](https://docs.opsfactor.com/documentation/community/licensing-faq/).

## Version

The initial public baseline is **0.1.0**. Maven declares that release version
directly, and the exact released source is identified by the annotated Git tag
`v0.1.0`.

## Architecture

- `com.opsfactor.community.web`: Community web application and API boundary.
- `com.opsfactor.community.capability.<domain>`: domain services, entities,
  repositories, projections, DTOs, and routines.
- `com.opsfactor.community.platform`: shared runtime, calendar, in-memory BI,
  integrations, and utilities.
- `com.opsfactor.community.rinstance`: Community statistical-model access.
- `com.opsfactor.community.scheduler`: synchronous execution history.
- `com.opsfactor.community.security`: simple Community authentication.

The Community edition includes statistical demand planning, heuristic supply
planning, a material/location Planning Book, and the minimum operational data
upload contracts. Enterprise-only features remain outside this repository.

## Build

Use Java 21 and Maven. The following command uses the workspace Maven cache:

```powershell
mvn -q -f pom.xml process-resources compile -DskipTests "-Dmaven.repo.local=C:\Users\erick\IdeaProjects\.m2repo"
```

Run the complete test suite with:

```powershell
mvn -q -f pom.xml test "-Dmaven.repo.local=C:\Users\erick\IdeaProjects\.m2repo"
```

## Local configuration

The MariaDB profile reads its connection from environment variables:

- `OPSFACTOR_DATASOURCE_HOST` (default: `localhost`)
- `OPSFACTOR_DATASOURCE_PORT` (default: `3306`)
- `OPSFACTOR_DATASOURCE_DATABASE` (default: `opsfactor`)
- `OPSFACTOR_DATASOURCE_USERNAME` (default: `opsfactor`)
- `OPSFACTOR_DATASOURCE_PASSWORD` (default: empty)

Do not commit credentials, private hosts, or customer-specific configuration.

## Frontend

The Community frontend lives in the separate
[`opsfactor-community-front`](../opsfactor-community-front) repository. The
backend does not version the frontend `dist`; a release pipeline builds it
separately and packages the resulting artifact in the final distribution.
