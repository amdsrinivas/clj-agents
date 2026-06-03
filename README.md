# clj-agents

A Clojure project scaffolded with `deps.edn` and modern Clojure tooling.

## Prerequisites

- [Clojure CLI tools](https://clojure.org/guides/install_clojure) (1.11+)
- Java 11 or later

## Project Structure

```
clj-agents/
├── deps.edn          # Dependency and alias configuration
├── build.clj         # Build script using tools.build
├── env.example       # Example environment configuration
├── README.md         # This file
├── src/
│   └── clj_agents/
│       └── core.clj  # Main source code
└── test/
    └── clj_agents/
        └── core_test.clj  # Tests
```

## Configuration

Copy `env.example` to `.env` in the project root and fill in the required values:

```bash
cp env.example .env
```

The `.env` file is loaded at runtime to configure LLM API connection settings. The available variables are:

| Variable | Description |
|---|---|
| `LLM_API_BASE_URL` | Base URL of the LLM API |
| `LLM_API_ENDPOINT` | API endpoint path |
| `LLM_API_KEY` | API key for the LLM provider |
| `LLM_MODEL` | Model identifier to use |
| `ENABLE_DEBUG` | Set to `true` to print LLM request/response payloads to the console |

> **Note:** `.env` is gitignored — you must create it manually on each clone.

## Usage

### Run the application

```bash
clojure -M:run
```

The application accepts an optional working directory argument. If omitted, it defaults to the current directory:

```bash
clojure -M:run /path/to/target/project
```

This sets the workspace root that file system tools (e.g. `list_directory`, `read_file`, `file_info`) operate within.

#### Debug logging

Set `ENABLE_DEBUG=true` in `.env` to print verbose LLM request and response payloads to the console. This is useful for inspecting API calls during development.

### Start a REPL

**In Cursive:** Configure the REPL run configuration to use the `:dev` alias, then run it.

Terminal REPL:

```bash
clojure -M:dev
```

### Build a standalone JAR

```bash
clojure -T:build clean
clojure -T:build jar
```

The JAR will be created at `target/clj-agents-0.1.0-standalone.jar`. Run it with:

```bash
java -jar target/clj-agents-0.1.0-standalone.jar
```

You can also pass a working directory to the JAR:

```bash
java -jar target/clj-agents-0.1.0-standalone.jar /path/to/target/project
```

## Editor Support

### IntelliJ IDEA (Cursive)

1. Import the project as a `deps.edn` project
2. Go to **Run > Edit Configurations > Clojure REPL > Local**
3. Select **Use deps.edn** and check the `:dev` alias
4. Run the configuration

## Adding Dependencies

Add dependencies to the `:deps` section in `deps.edn`:

```clojure
:deps {org.clojure/clojure {:mvn/version "1.12.0"}
       ;; Add your dependencies here
       clj-http/clj-http {:mvn/version "3.13.0"}}
```
