package de.dhbw.woped.process2text.controller;

import de.dhbw.woped.process2text.model.process.OpenAiApiDTO;
import de.dhbw.woped.process2text.service.P2TLLMService;
import de.dhbw.woped.process2text.service.P2TService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
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

  /**
   * Endpoint to translate a process model into human-readable text.
   *
   * @param body The process model in plain text format.
   * @return The translated text.
   */
  @PostMapping(value = "/generateText", consumes = "text/plain", produces = "text/plain")
  protected String generateText(@RequestBody String body) {
    if (logger.isDebugEnabled()) {
      logger.debug("Received body: " + body.replaceAll("[\n\r\t]", "_"));
    }
    String response = p2tService.generateText(body);
    logger.debug("Response: " + response);
    return response;
  }

  /**
   * Endpoint to translate a process model into human-readable text using OpenAI's Large Language
   * Model.
   *
   * @param body The process model in plain text format.
   * @param apiKey The API key for OpenAI.
   * @param prompt The prompt to guide the translation.
   * @param gptModel The GPT model to be used for translation.
   * @return The translated text.
   */
  @PostMapping(value = "/generateTextLLM", consumes = "text/plain", produces = "text/plain")
  protected String generateTextLLM(
      @RequestBody String body,
      @RequestParam(required = false) String apiKey,
      @RequestParam(required = true) String prompt,
      @RequestParam(required = true) String gptModel,
      @RequestParam(required = true) String provider,
      @RequestParam(required = false, defaultValue = "false") boolean useRag) {
    logger.debug(
        "Received request with apiKey: {}, prompt: {}, gptModel: {}, provider: {}, body: {}",
        apiKey,
        prompt,
        gptModel,
        provider,
        body.replaceAll("[\n\r\t]", "_"));

    OpenAiApiDTO openAiApiDTO;
    if (provider.equalsIgnoreCase("lmStudio")) {

      openAiApiDTO = new OpenAiApiDTO(null, gptModel, prompt, provider, useRag);
    } else {
      openAiApiDTO = new OpenAiApiDTO(apiKey, gptModel, prompt, provider, useRag);
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
  public List<String> getGptModels(
      @RequestParam(required = false) String apiKey,
      @RequestParam(required = true) String provider) {
    switch (provider) {
      case "gemini":
        return llmService.getGeminiModels(apiKey);
      case "openai":
        return llmService.getGptModels(apiKey);
      case "lmstudio":
        return llmService.getLmStudioModels();
      default:
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Invalid provider. Supported providers are: 'gemini', 'openAi' and 'lmStudio'.");
    }
  }
}
