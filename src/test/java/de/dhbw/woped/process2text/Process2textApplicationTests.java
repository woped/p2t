package de.dhbw.woped.process2text;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableWireMock({
    @ConfigureWireMock(name = "rag-service", port = 5000)
})
class Process2textApplicationTests {

  @LocalServerPort
  private int port;

  @Autowired
  private TestRestTemplate restTemplate;

  /**
   * Test: RAG service is called when useRAG=true
   */
  @Test
  void testRAGServiceCalledWhenEnabled() {
    // Mock RAG Service on port 5000
    stubFor(
        post(urlPathEqualTo("/rag/enrich"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"enriched_prompt\":\"Enhanced prompt with RAG context\"}")));

    // BPMN test content
    String bpmnBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<bpmn2:definitions xmlns:bpmn2=\"http://www.omg.org/spec/BPMN/20100524/MODEL\">\n"
        + "  <bpmn2:process id=\"test-process\" name=\"Test Process\" />\n"
        + "</bpmn2:definitions>";

    // Set system property for RAG service URL
    System.setProperty("rag.service.url", "http://localhost:5000");

    // Set HTTP headers
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);

    HttpEntity<String> request = new HttpEntity<>(bpmnBody, headers);

    // Call the real generateTextLLM method via HTTP with RAG enabled
    try {
      String url = "http://localhost:" + port + "/p2t/generateTextLLM"
          + "?apiKey=sk-testapikey"
          + "&prompt=Analyze this BPMN process"
          + "&gptModel=gpt-3.5-turbo"
          + "&provider=openAi"
          + "&useRag=true";

      String result = restTemplate.postForObject(url, request, String.class);

      // Check that a response is returned
      System.out.println("Response: " + result);
    } catch (Exception e) {
      // Ignore OpenAI errors, important is only that RAG was called
      System.out.println("Expected error (OpenAI mock): " + e.getMessage());
    }

    // Verify: RAG Service was called
    verify(postRequestedFor(urlPathEqualTo("/rag/enrich"))
        .withHeader("Content-Type", equalTo("application/json")));
    
    // Check that the RAG request contains the correct prompt and diagram
    verify(postRequestedFor(urlPathEqualTo("/rag/enrich"))
        .withRequestBody(containing("Analyze this BPMN process"))
        .withRequestBody(containing("test-process")));
  }

  /**
   * Test: RAG service is NOT called when useRAG=false
   */
  @Test
  void testRAGServiceNotCalledWhenDisabled() {
    // Mock RAG Service (but it won't be called)
    stubFor(
        post(urlPathEqualTo("/rag/enrich"))
            .withHeader("Content-Type", equalTo("application/json"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"enriched_prompt\":\"This should not be called\"}")));

    // BPMN test content
    String bpmnBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<bpmn2:definitions xmlns:bpmn2=\"http://www.omg.org/spec/BPMN/20100524/MODEL\">\n"
        + "  <bpmn2:process id=\"test-process\" name=\"Test Process\" />\n"
        + "</bpmn2:definitions>";

    // Set HTTP headers
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);

    HttpEntity<String> request = new HttpEntity<>(bpmnBody, headers);

    // Call the real generateTextLLM method via HTTP with RAG disabled
    try {
      String url = "http://localhost:" + port + "/p2t/generateTextLLM"
          + "?apiKey=sk-testapikey"
          + "&prompt=Analyze this BPMN process"
          + "&gptModel=gpt-3.5-turbo"
          + "&provider=openAi"
          + "&useRag=false";

      String result = restTemplate.postForObject(url, request, String.class);

      // Check that a response is returned
      System.out.println("Response: " + result);
    } catch (Exception e) {
      // Ignore OpenAI errors, important is only that RAG was NOT called
      System.out.println("Expected error (OpenAI mock): " + e.getMessage());
    }

    // Verify: RAG Service was NOT called
    verify(0, postRequestedFor(urlPathEqualTo("/rag/enrich")));
  }
}