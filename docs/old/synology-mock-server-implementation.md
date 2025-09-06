## Overview
This document provides a detailed guide for implementing the Synology DSM authentication mock server using the embedded WireMock approach in your Java-Spring-Boot application.

## Dependencies
Add the following dependency to your `pom.xml` file:

```xml
<dependency>
    <groupId>com.github.tomakehurst</groupId>
    <artifactId>wiremock-jre8-standalone</artifactId>
    <version>2.35.0</version>
    <scope>test</scope>
</dependency>
```

## Configuration
Create a new Spring configuration class to set up the embedded WireMock server:

```java
@Configuration
@Profile("demo")
public class SynologyMockServerConfig {
    
    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer mockSynologyServer() {
        WireMockServer server = new WireMockServer(wireMockConfig().port(5000));
        
        // Configure stubs for authentication endpoint
        server.stubFor(get(urlPathEqualTo("/webapi/auth.cgi"))
            .withQueryParam("api", equalTo("SYNO.API.Auth"))
            .withQueryParam("version", equalTo("2"))
            .withQueryParam("method", equalTo("login"))
            .withQueryParam("account", matching("(admin|internal[12]|external[12])"))
            .withQueryParam("passwd", equalTo("password123"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"success\":true,\"data\":{\"sid\":\"mock-session-{{now}}-{{request.query.account}}\"}}")
            ));
            
        // Add more stubs for error cases...
        
        return server;
    }
}
```

This configuration class sets up the WireMock server to listen on port 5000 and stubs the `/webapi/auth.cgi` endpoint with the appropriate response for successful and error cases.

## Usage
To use the mock server, you'll need to activate the "demo" Spring profile in your application. This can be done in various ways, such as:

1. Setting the `spring.profiles.active` system property:
   ```
   java -Dspring.profiles.active=demo -jar your-app.jar
   ```
2. Adding the following to your `application.properties` or `application.yml` file:
   ```yaml
   spring:
     profiles:
       active: demo
   ```

Once the mock server is running, your application can make requests to `http://localhost:5000/webapi/auth.cgi` and it will receive the appropriate responses based on the configured stubs.

## Testing
You can use the mock server in your integration tests to validate the behavior of your application's authentication flow. Here's an example:

```java
@SpringBootTest
@ActiveProfiles("demo")
public class AuthenticationTest {
    
    @Autowired
    private WebApplicationContext webApplicationContext;
    
    @Test
    public void testValidLogin() {
        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        mockMvc.perform(get("/webapi/auth.cgi")
            .param("api", "SYNO.API.Auth")
            .param("version", "2")
            .param("method", "login")
            .param("account", "admin")
            .param("passwd", "password123")
            .param("session", "FileStation")
            .param("format", "json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.sid").isString());
    }
}
```

## Best Practices
- Externalize mock user credentials and other configuration settings to a separate file (e.g., `application.yml`)
- Implement proper logging and error handling
- Add health checks and metrics/monitoring
- Ensure proper CORS support
- Validate all incoming requests
- Handle timeouts and other edge cases

## Conclusion
By following this guide, you should be able to set up the Synology DSM authentication mock server using the embedded WireMock approach in your Java-Spring-Boot application. This will provide a reliable and configurable mock server for development and testing purposes.
