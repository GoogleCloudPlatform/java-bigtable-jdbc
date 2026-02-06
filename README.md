# Bigtable JDBC

`bigtable-jdbc` is a [JDBC Driver](https://docs.oracle.com/javase/8/docs/technotes/guides/jdbc/) for [Google Bigtable](https://cloud.google.com/bigtable?hl=en).

You can create a JDBC connection easily for a variety of authentication types. For instance for an accessTokenProviderFQCN in connection URL:

```
package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {
  private static final String projectId = "test-project";
  private static final String instanceId = "test-instance";
  private static final String appProfileId = "test";
  private static final String endpoint = "test-endpoint";
  private static final String accessTokenProviderFQCN = "org.example.MyCustomImplementation";

  public static void main(String[] args) {
    try {
      // load the class so that it registers itself
      Class.forName("com.google.cloud.bigtable.jdbc.BigtableDriver");
      String url = String.format(
        "jdbc:bigtable:/projectId/%s/instances/%s?app_profile_id=%s?endpoint=%s&accessTokenProviderFQCN=%s",
        projectId, instanceId, appProfileId, endpoint, accessTokenProviderFQCN);
      Connection connection = DriverManager.getConnection(url);
      // perform SQL against Bigtable now!
    } catch (SQLException | ClassNotFoundException e) {
      e.printStackTrace();
    }
  }
}
```
## Running the Example

### Prerequisites

* Java 8 or higher
* Maven 3.2.5 or higher
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
    mvn clean package
    ```

3. **Run the example:**

    Replace the placeholder values with your actual project ID, instance ID, table name, and row key.

    ```bash
    mvn exec:java -Dexec.mainClass="com.google.cloud.bigtable.jdbc.example.JdbcExample" -Dexec.args="[PROJECT_ID] [INSTANCE_ID] [TABLE_NAME] [ROW_KEY]"
    ```

    Alternatively, to run the example with a service account key file:

    ```bash
    mvn exec:java -Dexec.mainClass="JdbcExampleBasicWithCreds" -Dexec.args="[PROJECT_ID] [INSTANCE_ID] [TABLE_ID] [CREDENTIAL_FILE_PATH]"
    ```

## Authentication

The driver supports several ways to provide Google Cloud credentials:

1.  **Application Default Credentials (ADC):** If no explicit credentials are provided, the driver will use ADC.
2.  **Service Account JSON String:** Use the `credential_json` property to provide the full JSON content of a service account key.
3.  **Service Account Key File:** Use the `credential_file_path` property to provide the path to a service account JSON key file.

**Note:** If both `credential_json` and `credential_file_path` are provided, `credential_json` takes precedence.
