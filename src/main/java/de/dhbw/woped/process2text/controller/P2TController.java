package de.dhbw.woped.process2text.controller;

import de.dhbw.woped.process2text.model.process.OpenAiApiDTO;
import de.dhbw.woped.process2text.service.P2TLLMService;
import de.dhbw.woped.process2text.service.P2TService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Controller class to handle HTTP requests related to process-to-text translation. Provides
 * endpoints to translate process models into human-readable text.
 */
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@Slf4j
public class P2TController {

  private static final Logger logger = LoggerFactory.getLogger(P2TController.class);

  @Autowired private P2TService p2tService;
  @Autowired private P2TLLMService llmService;

  @Autowired
  @Qualifier("httpRequestsTotal")
  private Counter httpRequestsTotal;

  @Autowired
  @Qualifier("httpRequestDuration")
  private Timer httpRequestDuration;

  /**
   * Endpoint to translate a process model into human-readable text.
   *
   * @param body The process model in plain text format.
   * @return The translated text.
   */
  @ApiOperation(value = "Translate a process model into human readable text.")
  @PostMapping(value = "/generateText", consumes = "text/plain", produces = "text/plain")
  protected String generateText(@RequestBody String body) {
    httpRequestsTotal.increment();
    return httpRequestDuration.record(
        () -> {
          if (logger.isDebugEnabled()) {
            logger.debug("Received body: " + body.replaceAll("[\n\r\t]", "_"));
          }
          String response = p2tService.generateText(body);
          logger.debug("Response: " + response);
          return response;
        });
  }

  /**
   * Endpoint to translate a process model into human-readable text using OpenAI's Large Language
   * Model.
   *
   * @param body The process model in plain text format.
   * @param apiKey The API key for OpenAI.
   * @param prompt The prompt to guide the translation.
   * @param gptModel The GPT model to be used for translation.
   * @param provider The provider to use (e.g., "openAi", "lmStudio").
   * @param useRag Whether to use RAG (Retrieval-Augmented Generation) to enrich the prompt.
   * @return The translated text.
   */
  @ApiOperation(
      value =
          "Translate a process model into human readable text using one of OpenAIs Large Language"
              + " Models")
  @PostMapping(value = "/generateTextLLM", consumes = "text/plain", produces = "text/plain")
  protected String generateTextLLM(
      @RequestBody String body,
      @RequestParam(required = false) String apiKey,
      @RequestParam(required = true) String prompt,
      @RequestParam(required = true) String gptModel,
      @RequestParam(required = true) String provider,
      @RequestParam(required = true) boolean useRag) {
    logger.debug(
        "Received request with apiKey: {}, prompt: {}, gptModel: {}, provider: {}, useRag: {}, body: {}",
        apiKey,
        prompt,
        gptModel,
        provider,
        useRag,
        body.replaceAll("[\n\r\t]", "_"));

    String enrichedPrompt = prompt;

    if (useRag) {
      try {
        RestTemplate restTemplate = new RestTemplate();
        // JSON body for the RAG service
        org.json.JSONObject requestJson = new org.json.JSONObject();
        requestJson.put("prompt", prompt);
        requestJson.put("diagram", body);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestJson.toString(), headers);

        // POST to the RAG service
        String ragServiceUrl = System.getProperty("rag.service.url", "http://localhost:5000");
        ResponseEntity<String> ragResponse =
            restTemplate.postForEntity(ragServiceUrl + "/rag/enrich", entity, String.class);

        // Expected: {"enriched_prompt": "..."}
        org.json.JSONObject responseJson = new org.json.JSONObject(ragResponse.getBody());
        enrichedPrompt = responseJson.getString("enriched_prompt");
        logger.info("RAG service enriched prompt successfully. Original length: {}, Enriched length: {}", 
                   prompt.length(), enrichedPrompt.length());
        logger.debug("Enriched prompt: {}", enrichedPrompt);
      } catch (Exception e) {
        logger.error("Error calling RAG service, falling back to original prompt", e);
      }
    }

    OpenAiApiDTO openAiApiDTO;
    if (provider.equalsIgnoreCase("lmStudio")) {

      openAiApiDTO = new OpenAiApiDTO(null, gptModel, enrichedPrompt, provider, useRag);
    } else {
      openAiApiDTO = new OpenAiApiDTO(apiKey, gptModel, enrichedPrompt, provider, useRag);
    }

    try {
      String response = llmService.callLLM(body, openAiApiDTO);
      logger.debug("LLM Response: " + response);
      return response;
    } catch (ResponseStatusException e) {
      logger.error("Error processing LLM request", e);
      throw e;
    }
  }

  /**
   * Endpoint to retrieve the list of available GPT models.
   *
   * @param apiKey The API key for OpenAI.
   * @return A list of model names as strings.
   */
  @GetMapping("/gptModels")
  public List<String> getGptModels(@RequestParam(required = true) String apiKey) {
    httpRequestsTotal.increment();
    return httpRequestDuration.record(() -> llmService.getGptModels(apiKey));
  }
}
