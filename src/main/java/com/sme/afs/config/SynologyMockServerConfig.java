package com.sme.afs.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("demo")
public class SynologyMockServerConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public WireMockServer mockSynologyServer() {
        WireMockServer server = new WireMockServer(
                WireMockConfiguration.wireMockConfig()
                        .port(5000)
                        .extensions(new ResponseTemplateTransformer(false)) // Enable the response template transformer
        );

        // Add stub for failed authentication
        server.stubFor(WireMock.get(WireMock.urlPathEqualTo("/webapi/auth.cgi"))
            .withQueryParam("api", WireMock.equalTo("SYNO.API.Auth"))
            .withQueryParam("version", WireMock.equalTo("2"))
            .withQueryParam("method", WireMock.equalTo("login"))
            .willReturn(WireMock.aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"success\":false,\"error\":{\"code\":400}}")
            ));

        // Configure stubs for authentication endpoint
        server.stubFor(WireMock.get(WireMock.urlPathEqualTo("/webapi/auth.cgi"))
                .withQueryParam("api", WireMock.equalTo("SYNO.API.Auth"))
                .withQueryParam("version", WireMock.equalTo("2"))
                .withQueryParam("method", WireMock.equalTo("login"))
                .withQueryParam("account", WireMock.matching("(admin[123])"))
                .withQueryParam("passwd", WireMock.equalTo("password123"))
                .withQueryParam("otp_code", WireMock.equalTo("333666"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withTransformers("response-template")
                        .withBody("{\"success\":true,\"data\":{\"sid\":\"mock-session-"
                                + System.currentTimeMillis() + "-"
                                + "{{request.query.account}}\"}}")
                ));

        return server;
    }
}