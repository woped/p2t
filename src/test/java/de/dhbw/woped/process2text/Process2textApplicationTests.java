package de.dhbw.woped.process2text;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.EnableWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import de.dhbw.woped.process2text.service.P2TLLMService;

@SpringBootTest
@EnableWireMock
class Process2textApplicationTests {

  @Value("${wiremock.server.baseUrl}")
  private String wiremockBaseUrl;

  @Autowired private P2TLLMService p2tllmService;

  @Test
  void contextLoads() {}

  @Test
  void testGetGeminiModels() {
    stubFor(
        get(urlPathEqualTo("/v1beta/models"))
            .withHeader("X-Goog-Api-Key", equalTo("test-api-key"))
            .willReturn(okJson("{\"models\":[{\"name\":\"mod1\"},{\"name\":\"mod2\"}]}")));

    System.setProperty("gemini.api.baseUrl", wiremockBaseUrl);
    System.setProperty("gemini.api.url", wiremockBaseUrl + "/v1beta/models");

    List<String> models = p2tllmService.getGeminiModels("test-api-key");
    assertNotNull(models);
    assertEquals(2, models.size());
    assertTrue(models.contains("mod1"));
    assertTrue(models.contains("mod2"));
  }

  @Test
  void testGetGptModels() {
    stubFor(
        get(urlPathEqualTo("/v1/models"))
            .withHeader("Authorization", equalTo("Bearer test-api-key"))
            .willReturn(okJson("{\"data\":[{\"id\":\"gpt-4\"},{\"id\":\"gpt-3.5-turbo\"}]}")));

    System.setProperty("openai.api.url", wiremockBaseUrl + "/v1/models");

    List<String> models = p2tllmService.getGptModels("test-api-key");
    assertNotNull(models);
    assertEquals(2, models.size());
    assertTrue(models.contains("gpt-4"));
    assertTrue(models.contains("gpt-3.5-turbo"));
  }
}
