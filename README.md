# Bigtable JDBC

[![ci](https://github.com/GoogleCloudPlatform/java-bigtable-jdbc/actions/workflows/ci.yaml/badge.svg)](https://github.com/GoogleCloudPlatform/java-bigtable-jdbc/actions/workflows/ci.yaml)

`bigtable-jdbc` is a [JDBC Driver](https://docs.oracle.com/javase/8/docs/technotes/guides/jdbc/) for [Google Cloud Bigtable](https://cloud.google.com/bigtable?hl=en).

## Getting Started

Add the driver to your project dependencies. It is recommended to use the [shaded jar](#shaded-artifact) to avoid dependency conflicts.

```xml
<dependency>
  <groupId>com.google.cloud</groupId>
  <artifactId>google-cloud-bigtable-jdbc</artifactId>
  <version>0.1.1-SNAPSHOT</version>
  <classifier>shaded</classifier>
</dependency>
```

### Usage Example

```java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class Main {
  public static void main(String[] args) throws Exception {
    String projectId = "your-project-id";
    String instanceId = "your-instance-id";
    
    // Format: jdbc:bigtable:/projects/{projectId}/instances/{instanceId}
    String url = String.format("jdbc:bigtable:/projects/%s/instances/%s", projectId, instanceId);

    try (Connection connection = DriverManager.getConnection(url);
         Statement statement = connection.createStatement()) {
         
      try (ResultSet resultSet = statement.executeQuery("SELECT * FROM my_table LIMIT 10")) {
        while (resultSet.next()) {
          System.out.println(resultSet.getString(1));
        }
      }
    }
  }
}
```

## Connection URL Format

The driver uses the following URL format:
`jdbc:bigtable:/projects/{projectId}/instances/{instanceId}[?property=value[&...]]`

### Connection Parameters

| Property | Description | Default |
| :--- | :--- | :--- |
| `app_profile_id` | The Bigtable [App Profile ID](https://cloud.google.com/bigtable/docs/app-profiles) to use. | `default` |
| `universe_domain` | The universe domain for the Bigtable service. | `googleapis.com` |
| `credential_file_path` | Local path to a service account JSON key file. | - |
| `credential_json` | The full JSON content of a service account key. | - |

## Authentication

The driver supports several ways to provide Google Cloud credentials:

1.  **Application Default Credentials (ADC):** If no explicit credentials are provided, the driver will use ADC.
2.  **Service Account JSON String:** Use the `credential_json` property.
3.  **Service Account Key File:** Use the `credential_file_path` property.

*Note: If multiple methods are provided, `credential_json` takes highest precedence, followed by `credential_file_path`, then ADC.*

## Supported SQL

The driver currently supports a subset of SQL via the [Bigtable SQL API](https://cloud.google.com/bigtable/docs/reference-sql).

*   **SELECT statements**: Querying data from tables.
*   **Parameterized Queries**: Using `?` placeholders in `PreparedStatement`.
*   **ReadOnly**: The connection is strictly read-only. `executeUpdate` and other modification operations are not supported.

## Shaded Artifact

To prevent dependency conflicts with libraries like gRPC, Guava, or Protobuf, a shaded version of the jar is provided. All transitive dependencies are relocated to the `com.google.cloud.bigtable.jdbc.shaded` package.

To use the shaded jar in Maven, add the `<classifier>shaded</classifier>` tag to your dependency.

## Running the Samples

### Prerequisites

* Java 17 or higher
* Maven 3.9.9 or higher
* A Google Cloud project with the Bigtable API enabled
* A Bigtable instance and table

### Steps

1. **Clone the repository:**
    ```bash
    git clone https://github.com/GoogleCloudPlatform/java-bigtable-jdbc.git
    cd java-bigtable-jdbc
    ```

2. **Build the project:**
    ```bash
    mvn clean install -DskipTests
    ```

3. **Run the samples:**
    Navigate to the samples directory and follow the instructions in the [samples/snippets README](samples/snippets/README.md) (if available) or run:
    ```bash
    cd samples/snippets
    mvn exec:java -Dexec.mainClass="com.google.cloud.bigtable.jdbc.samples.JdbcExample" -Dexec.args="[PROJECT_ID] [INSTANCE_ID] [TABLE_NAME] [ROW_KEY]"
    ```
