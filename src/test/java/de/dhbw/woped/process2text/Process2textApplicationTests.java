package de.dhbw.woped.process2text;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dhbw.woped.process2text.model.process.OpenAiApiDTO;
import de.dhbw.woped.process2text.service.P2TLLMService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.EnableWireMock;

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

  @Test
  void testCallLLMOpenAI() {
    String body =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + //
            "<bpmn2:definitions xmlns:bpmn2=\"http://www.omg.org/spec/BPMN/20100524/MODEL\""
            + " xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\""
            + " xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\""
            + " xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" id=\"empty-definitions\""
            + " targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
            + //
            "  <bpmn2:collaboration id=\"Collaboration_id-12a40ec6-9fc5-4b12-aaa1-d8f8bc718b41\">\n"
            + //
            "    <bpmn2:participant id=\"id-54d8a5a1-c344-492c-bf9f-b29ad9662303\""
          + " name=\"Customer\" processRef=\"Process_id-54d8a5a1-c344-492c-bf9f-b29ad9662303\" />\n"
            + //
            "    <bpmn2:participant id=\"id-ba446fe3-fc5a-468a-879f-636efa8ae458\" name=\"Car Wash"
            + " Machine\" processRef=\"Process_id-ba446fe3-fc5a-468a-879f-636efa8ae458\" />\n"
            + //
            "  </bpmn2:collaboration>\n"
            + //
            "  <bpmn2:process id=\"Process_id-54d8a5a1-c344-492c-bf9f-b29ad9662303\""
            + " name=\"Customer\" />\n"
            + //
            "  <bpmn2:process id=\"Process_id-ba446fe3-fc5a-468a-879f-636efa8ae458\" name=\"Car"
            + " Wash Machine\" />\n"
            + //
            "</bpmn2:definitions>";
    OpenAiApiDTO dto =
        new OpenAiApiDTO(
            "sk-testapikey", "gpt-3.5-turbo", "Summarize the following text:", "openAi", false);

    stubFor(
        post(urlPathEqualTo("/chat/completions"))
            .withHeader("Authorization", equalTo("Bearer " + dto.getApiKey()))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"choices\":[{\"message\":{\"content\":\"This is a mocked OpenAI"
                            + " response.\"}}]}"))); // Direkte JSON-Antwort

    System.setProperty("openai.api.url", wiremockBaseUrl);

    String actualResponse = p2tllmService.callLLM(body, dto);

    assertNotNull(actualResponse);
    assertEquals("This is a mocked OpenAI response.", actualResponse);
  }

  @Test
  void testCallLLMGemini() {
    String body =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + //
            "            <bpmn2:definitions"
            + " xmlns:bpmn2=\"http://www.omg.org/spec/BPMN/20100524/MODEL\""
            + " xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\""
            + " xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\""
            + " xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" id=\"empty-definitions\""
            + " targetNamespace=\"http://bpmn.io/schema/bpmn\">\n"
            + //
            "              <bpmn2:collaboration"
            + " id=\"Collaboration_id-12a40ec6-9fc5-4b12-aaa1-d8f8bc718b41\">\n"
            + //
            "                <bpmn2:participant id=\"id-54d8a5a1-c344-492c-bf9f-b29ad9662303\""
          + " name=\"Customer\" processRef=\"Process_id-54d8a5a1-c344-492c-bf9f-b29ad9662303\" />\n"
            + //
            "                <bpmn2:participant id=\"id-ba446fe3-fc5a-468a-879f-636efa8ae458\""
            + " name=\"Car Wash Machine\""
            + " processRef=\"Process_id-ba446fe3-fc5a-468a-879f-636efa8ae458\" />\n"
            + //
            "              </bpmn2:collaboration>\n"
            + //
            "              <bpmn2:process id=\"Process_id-54d8a5a1-c344-492c-bf9f-b29ad9662303\""
            + " name=\"Customer\" />\n"
            + //
            "              <bpmn2:process id=\"Process_id-ba446fe3-fc5a-468a-879f-636efa8ae458\""
            + " name=\"Car Wash Machine\" />\n"
            + //
            "</bpmn2:definitions>";

    OpenAiApiDTO dto =
        new OpenAiApiDTO(
            "your-gemini-api-key",
            "gemini-1.5-flash",
            "Summarize the following text:",
            "gemini",
            true);

    System.setProperty("gemini.api.url", wiremockBaseUrl);

    stubFor(
        post(urlPathEqualTo("/v1beta/models/gemini-1.5-flash:generateContent"))
            .withHeader("X-Goog-Api-Key", equalTo(dto.getApiKey()))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"This is a mocked"
                            + " Gemini"
                            + " response.\"}],\"role\":\"model\"},\"finishReason\":\"STOP\",\"index\":0,\"safetyRatings\":[]}]}")));

    String actualResponse =
        p2tllmService.callLLM(body, dto); // callLLM should use the configured base URL

    assertNotNull(actualResponse);
    assertEquals("This is a mocked Gemini response.", actualResponse);
  }
}
