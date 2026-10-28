INVENTORY MANAGER - OPERATIONS QUICK START
==========================================

Requirements: Java 17 or newer (java -version).

Start (Linux/macOS):   bin/run.sh
Start (Windows):       bin\run.bat
Check the version:     bin/run.sh --version
Use another data file: bin/run.sh --data=/srv/inventory/stock.csv

Folders
  bin/    launcher scripts
  lib/    the application JAR (self-contained, no other libraries needed)
  conf/   application.properties - edit settings here
  data/   inventory.csv is created on first save - BACK THIS UP
  logs/   inventory.log

Backup:   copy data/inventory.csv while the application is stopped.
Upgrade:  stop the app, back up data/, unzip the new version next to the old one,
          copy data/ and conf/ across, start the new version.
Rollback: stop the app, start the previous version's folder (its data/ is untouched).

Exit codes: 0 normal, 1 startup failure (bad config or corrupt data file), 2 bad command line.
Full documentation: docs/DEVELOPER_GUIDE.md in the source repository.
