# Container build for the Inventory Manager.
#
# 1) Build the bundle first:      bash scripts/package.sh
# 2) Build the image:             docker build -t inventory-app:1.0.0 .
# 3) Run interactively, keeping the data in a named volume so it survives container removal:
#       docker run -it --rm -v inventory-data:/opt/inventory/data inventory-app:1.0.0
#
# NOTE: this Dockerfile was written for the project but could not be built in the authoring
# environment (no Docker daemon available there). Treat the first build as part of acceptance.
FROM eclipse-temurin:17-jre

# Run as a normal user, not root.
RUN useradd --system --create-home --home-dir /opt/inventory inventory
WORKDIR /opt/inventory

COPY dist/inventory-app-1.0.0/ /opt/inventory/
RUN chown -R inventory:inventory /opt/inventory && chmod +x /opt/inventory/bin/run.sh
USER inventory

VOLUME ["/opt/inventory/data"]
ENTRYPOINT ["/opt/inventory/bin/run.sh"]
