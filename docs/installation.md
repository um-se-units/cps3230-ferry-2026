# Installing and running the Fliegu Ferries booking service

This guide takes you from an empty machine to a green build and a running
application. You need it in week 1; the same setup carries you through the
whole unit.

## 1. What to install

| Tool | Version | Why |
|---|---|---|
| JDK | 21 | the application and the tests are Java 21 |
| Apache Maven | 3.9 or later | `mvn verify` builds and tests everything |
| Git | any recent | cloning and committing your work |
| Google Chrome | current | the browser tests in weeks 8 and 9 |

Any JDK 21 will do, whoever built it. If `java -version` already shows
version 21, leave out the JDK line below and install the rest.

### macOS

Install [Homebrew](https://brew.sh) if you do not have it, then in Terminal:

```bash
brew install --cask temurin@21
brew install maven git
brew install --cask google-chrome
```

Add this line to `~/.zshrc` so that Maven finds the right JDK, then open a
new Terminal window:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

### Windows

In PowerShell:

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
winget install Apache.Maven
winget install Git.Git
winget install Google.Chrome
```

Open a new PowerShell window afterwards so that the updated `PATH` is used.
If `winget install Apache.Maven` is not available on your machine, download
the binary zip from <https://maven.apache.org/download.cgi>, unzip it to
`C:\Tools\apache-maven` and add `C:\Tools\apache-maven\bin` to your `PATH`
(Settings, "Edit environment variables for your account").

### Check

```bash
java -version      # openjdk version "21..."
mvn -version       # Apache Maven 3.9.x ... Java version: 21
git --version
```

If `mvn -version` shows a Java version other than 21, `JAVA_HOME` points at
another JDK; fix that before going on.

## 2. Clone the repository

```bash
git clone https://github.com/um-se-units/cps3230-ferry-2026
cd cps3230-ferry-2026
```

## 3. Build and test

```bash
mvn verify
```

The first run downloads the dependencies. A successful build ends with
`BUILD SUCCESS` and a test summary such as:

```text
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
```

It also writes the coverage report to `target/site/jacoco/index.html` (open
it in a browser) and the runnable application to `target/ferry.jar`.

On Windows PowerShell, quote any Maven argument that contains `=`, for
example `mvn verify "-Dweek=03"`.

## 4. Run the application

```bash
java -jar target/ferry.jar
```

Open <http://localhost:8080>. The application uses an in-memory database
with a fortnight of sailings between Ċirkewwa and Mġarr already loaded; every
restart starts from that clean state. Stop it with `Ctrl+C`.

## 5. Common problems

| Symptom | Cause and fix |
|---|---|
| `mvn: command not found` or `'mvn' is not recognized` | Maven's `bin` folder is not on `PATH`; open a new terminal window after installing, or add it by hand |
| `Fatal error compiling: error: release version 21 not supported` | Maven is running an older JDK; set `JAVA_HOME` to JDK 21 |
| `Address already in use` when running the jar | something else is on port 8080; stop it, or find it with `lsof -i :8080` (macOS) or `netstat -ano \| findstr :8080` (Windows) |
| `Could not transfer artifact` during the first `mvn verify` | no network, or a proxy; on a proxy configure `~/.m2/settings.xml` as described at <https://maven.apache.org/guides/mini/guide-proxies.html> |
| Browser tests cannot start Chrome | Chrome is not installed, or Selenium Manager could not download the driver; install Chrome and retry with a network connection |

## 6. Working in an IDE

Both IntelliJ IDEA and VS Code (with the Extension Pack for Java) open the
project from its `pom.xml` and pick up the same JDK and dependencies. Keep
`mvn verify` as the reference: what passes there is what counts.
