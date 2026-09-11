# OpsFactor Community

[Português](README.pt-BR.md)

OpsFactor Community is the source-available backend for demand and supply chain
planning. It is an independently buildable Java 21 / Spring Boot application
and does not depend on OpsFactor Enterprise sources or artifacts.

This repository is released under the [Sustainable Use License 1.0](LICENSE.md).
Practical licensing examples are available in the
[Community licensing FAQ](https://docs.opsfactor.com/documentation/community/licensing-faq/).

## Version

Use the Git tag and release manifest to identify the exact source of a
distribution. Backend and frontend are versioned independently; the Maven
version alone does not identify an installer. Use the installer, documentation,
and import templates from the same release.

## Start here

- **Use the application:** follow the [Community installation guide](https://docs.opsfactor.com/documentation/community/tutorial/01-installation/).
- **Load a reproducible scenario:** follow the [Community tutorial](https://docs.opsfactor.com/documentation/community/tutorial/), including its downloadable files and import order.
- **Develop the backend:** use the build and configuration steps below. The backend does not serve the separate frontend.

## Planning and production data

- Statistical demand planning and a material/location Planning Book support collaboration through **Direct Demand = Baseline + Demand Adjustment**.
- Heuristic supply planning uses the supply network, inventory, production resources, availability, routings, bills of materials, and production versions.
- A routing owns its **base quantity and unit**; its operations specify **duration and time unit**. A production version links the routing and bill of materials. Download templates from the running version before preparing an import: column order matters.
- **Production Plan Volume** and **Production Plan Occupation** expose planned production and resource use. The default SNP unit must be compatible with the scenario's units.

Production master data includes single-output and multiple-output models.
The tutorial validates the single-output flow. Multiple-output Production Plan
persistence still has a known key/conflict limitation; do not assume that
multi-output master-data support means the complete planning flow is validated.
See the [production data tutorial](https://docs.opsfactor.com/documentation/community/tutorial/11-production-data/).

## Architecture

- `com.opsfactor.community.bootstrap`: application entry point.
- `com.opsfactor.community.web`: Community API boundary.
- `com.opsfactor.community.capability.<domain>`: domain services, entities,
  repositories, projections, DTOs, and routines.
- `com.opsfactor.community.platform`: shared runtime, calendar, in-memory BI,
  integrations, and utilities.
- `com.opsfactor.community.platform.rinstance`: statistical-model access through R.
- `com.opsfactor.community.platform.scheduler`: task execution and history.
- `com.opsfactor.community.platform.security`: Community authentication.

The Community edition includes statistical demand planning, heuristic supply
planning, a material/location Planning Book, and the minimum operational data
upload contracts. Enterprise-only features remain outside this repository.

## Build

Use Java 21 and Maven from this repository's root. Compile and run the standard
test suite while producing the executable backend:

```powershell
mvn clean package
```

To run only the standard test suite:

```powershell
mvn test
```

The PostgreSQL migration integration gate is separate: `*IT` is not included
automatically in the standard suite. Provide a PostgreSQL binaries directory
containing `initdb` and `pg_ctl`; the test creates and stops its own disposable
cluster and must not target an existing database:

```powershell
mvn test "-Dtest=ProductionSchemaCompatibilityPostgreSqlIT" "-Dopsfactor.test.postgresql.bin=C:/path/to/postgresql/bin"
```

## Local configuration

Create a dedicated PostgreSQL database and role first. The default profiles
are `prd,database-postgresql`; the connection reads these environment variables:

- `OPSFACTOR_DATASOURCE_HOST` (default: `localhost`)
- `OPSFACTOR_DATASOURCE_PORT` (default: `5432`)
- `OPSFACTOR_DATASOURCE_DATABASE` (default: `opsfactor`)
- `OPSFACTOR_DATASOURCE_USERNAME` (default: `opsfactor`)
- `OPSFACTOR_DATASOURCE_PASSWORD` (default: empty)

Do not commit credentials, private hosts, or customer-specific configuration.

After setting those variables in your shell, start the backend in the foreground:

```powershell
java -jar target/opsfactor-community-exec.jar
```

The source default port is `5000`; readiness is available at
`http://localhost:5000/health-status`. Installer-managed ports may differ.
Statistical forecasting also needs R with the `forecast` package available to
RCaller. The Windows distribution packages its runtime dependencies; a source
build does not install them. Back up an existing database before upgrading.

## Frontend

The Community frontend lives in the separate
[`opsfactor-community-front`](https://github.com/OpsFactor/opsfactor-community-front) repository. The
backend does not version the frontend `dist`; a release pipeline builds it
separately and packages the resulting artifact in the final distribution.
