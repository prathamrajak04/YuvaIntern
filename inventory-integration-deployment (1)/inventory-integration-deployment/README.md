# Inventory Manager: Integration, Deployment and Documentation

[![CI](https://github.com/prathamrajak04/inventory-integration-deployment/actions/workflows/ci.yml/badge.svg)](https://github.com/prathamrajak04/inventory-integration-deployment/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-17%2B-orange)
![Dependencies](https://img.shields.io/badge/runtime%20dependencies-0-brightgreen)
![License](https://img.shields.io/badge/license-MIT-blue)

A command-line inventory application in plain Java 17: add, list, find, search, update and delete
items, get a low-stock report and the total stock value. Data is saved to a CSV file so it survives
restarts. It ships as a single self-contained JAR with launcher scripts, a release bundle, a
simulated deployment check and a full developer guide.

## Quick start

```bash
bash scripts/package.sh            # builds dist/inventory-app-1.0.0.zip (Maven or plain JDK)
unzip dist/inventory-app-1.0.0.zip -d /opt
/opt/inventory-app-1.0.0/bin/run.sh      # Windows: bin\run.bat
```

Requires Java 17 or newer. No other libraries are needed at runtime.

```
=== Inventory Manager ===
 1) Add item          6) Update price
 2) List all items    7) Rename item
 3) Find by id        8) Delete item
 4) Search by name    9) Low-stock report
 5) Update quantity  10) Total inventory value
 0) Exit
```

## Architecture at a glance

```
app.Main            wires everything together, parses flags, returns exit codes
cli.ConsoleMenu     user interface (only talks to the service)
service             business rules and audit logging
repository          ItemRepository interface -> InMemory / CSV file implementations
model, util, exception, config
```

Layers only depend downward; `scripts/check-layers.sh` enforces it and runs in CI.
Details, a worked request walk-through and design reasons are in the
[Developer Guide](docs/DEVELOPER_GUIDE.md) (also provided as `docs/Week5_Developer_Guide.docx`).

## Features

- Full CRUD with validation (blank names, negative quantities, NaN/infinite prices rejected)
- Durable CSV storage: atomic file replacement, write-before-memory-update, precise corruption errors
- Layered configuration: bundled defaults < config file < environment < JVM property < command line
- Logging to a file with the JDK's built-in logger (no logging library)
- Exit codes for operations: `0` normal, `1` startup failure, `2` bad command line
- Executable JAR, Linux/macOS and Windows launchers, SHA-256 checksum, optional Dockerfile

## Build, test, deploy

| Task | Command |
|------|---------|
| Check layer rules | `bash scripts/check-layers.sh` |
| Run all tests + coverage | `mvn test` (report: `target/site/jacoco/index.html`) |
| Build the JAR | `bash scripts/build.sh` (`--no-maven` for JDK-only machines) |
| Make the release bundle | `bash scripts/package.sh` |
| Acceptance-test the bundle | `bash scripts/deploy-simulate.sh` |
| Container (optional) | `docker build -t inventory-app:1.0.0 .` |

## Command line

```
--config=PATH   external properties file
--data=PATH     CSV data file (highest priority)
--version, -v   print version
--help, -h      print usage
```

## Verification status

- 122 unit and integration tests and 17 deployment acceptance checks were run on the final code.
  The test run used a small stand-in for the JUnit annotations because Maven Central was not
  reachable where the project was authored; run `mvn test` for the official JUnit and JaCoCo report.
- `bin/run.bat` and the `Dockerfile` have not been executed (Linux-only authoring environment).
  Run `bin\run.bat --version` / build the image as the first acceptance step on those platforms.

## Project layout

```
src/main/java/com/inventory/   application code
src/main/resources/            bundled default settings
src/test/java/com/inventory/   tests (unit, component, end-to-end)
deploy/                        launcher scripts, sample config and README that go into the bundle
scripts/                       build, package, deploy-simulate, check-layers
docs/                          developer guide (md + docx)
Dockerfile, pom.xml, .github/workflows/ci.yml
```

## License

MIT
