package de.dhbw.woped.process2text.service;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import de.dhbw.woped.process2text.controller.P2TController;
import de.dhbw.woped.process2text.model.process.OpenAiApiDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service class to handle interaction with the LLM APIs. This service sends text to the API and
 * retrieves the generated response.
 */
@Service
public class P2TLLMService {

  private static final Logger logger = LoggerFactory.getLogger(P2TController.class);

  /**
   * Calls the OpenAI API with the provided text and API details, and extracts the response content.
   *
   * @param body The text to be sent to the OpenAI API.
   * @param openAiApiDTO Contains the API key, GPT model, and prompt.
   * @return The content of the response from the OpenAI API.
   */
  public String callLLM(String body, OpenAiApiDTO dto) {
    // Use the Transformer API if the provided processmodell is a PNML to parse it
    // into an BPMN
    TransformerService transformerService = new TransformerService();
    if ("PNML".equals(transformerService.checkForBPMNorPNML(body))) {
      body = transformerService.transform("pnmltobpmn", body);
    } // Get Provider
    String provider = dto.getProvider(); // Use original provider name for display purposes
    String providerLowerCase = provider.toLowerCase(); // Use lowercase for switch comparison

    // Call provider
    String response = "";
    switch (providerLowerCase) {
      case "openai":
        response = createCallOpenAi(body, dto);
        break;
      case "gemini":
        response = createCallGemini(body, dto);
        break;
      case "lmstudio":
        response = createCallLmStudio(body, dto);
        break;
      default:
        throw new IllegalArgumentException("Unknown Provider: " + provider);
    }

    return response;
  }

  /*
   * Creates the API Call for the OpenAI API with the provided text and API details.
   *
   * @param body The text to be sent to the OpenAI API.
   * @param openAiApiDTO Contains the API key, GPT model, and prompt.
   * @return the api call for Open Ai.
   */
  private String createCallOpenAi(String body, OpenAiApiDTO dto) {

    if (dto.getApiKey() == null || dto.getApiKey().trim().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OpenAI API key is missing.");
    }
    if (dto.getGptModel() == null || dto.getGptModel().trim().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OpenAI GPT model is missing.");
    }

    String url = System.getProperty("openai.api.url", "https://api.openai.com/v1");

    try {
      OpenAIClient client =
          OpenAIOkHttpClient.builder().apiKey(dto.getApiKey()).baseUrl(url).build();

      ChatCompletionCreateParams createParams =
          ChatCompletionCreateParams.builder()
              .model(dto.getGptModel())
              .maxCompletionTokens(4096)
              .temperature(0.7)
              .addSystemMessage("You are a helpful assistant.")
              .addUserMessage(dto.getPrompt())
              .addUserMessage(body)
              .build();

      ChatCompletion chatCompletion = client.chat().completions().create(createParams);

      String response = chatCompletion.choices().get(0).message().content().get();

      logger.info("Raw OpenAI API response: {}", response);

      return response;

    } catch (Exception e) {
      logger.error("An unexpected error occurred during OpenAI API call: {}", e.getMessage(), e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error calling OpenAI API.", e);
    }
  }

  /*
   * Creates the API Call for the Gemini API with the provided text and API details.
   *
   * @param body The text to be sent to the Gemini API.
   * @param openAiApiDTO Contains the API key, Gemini model, and prompt.
   * @return the api call for Gemini.
   */
  private String createCallGemini(String body, OpenAiApiDTO dto) {

    if (dto.getApiKey() == null || dto.getApiKey().trim().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gemini API key is missing.");
    }
    if (dto.getGptModel() == null || dto.getGptModel().trim().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gemini model is missing.");
    }

    String url = System.getProperty("gemini.api.url", "https://generativelanguage.googleapis.com");

    try {

      Client.setDefaultBaseUrls(Optional.of(url), Optional.empty());

      Client client = Client.builder().apiKey(dto.getApiKey()).build();

      GenerateContentConfig config =
          GenerateContentConfig.builder().temperature((float) 0.7).maxOutputTokens(4096).build();

      List<Content> chatMessages = new ArrayList<>();
      chatMessages.add(
          Content.builder()
              .role("model")
              .parts(List.of(Part.fromText("You are a helpful assistant.")))
              .build());
      chatMessages.add(
          Content.builder()
              .role("user")
              .parts(List.of(Part.fromText(dto.getPrompt() + body)))
              .build());

      GenerateContentResponse response =
          client.models.generateContent(dto.getGptModel(), chatMessages, config);

      if (response == null || response.text() == null || response.text().isEmpty()) {
        logger.warn(
            "Gemini API response was empty or malformed for request with model: {}",
            dto.getGptModel());
        throw new ResponseStatusException(
            HttpStatus.NO_CONTENT, "Gemini API returned an empty or malformed response.");
      }

      return response.text();

    } catch (Exception e) {
      logger.error("An unexpected error occurred during Gemini API call: {}", e.getMessage(), e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error calling Gemini API.", e);
    }
  }

  /**
   * Creates the API call for the LM Studio API using the provided text and API details.
   *
   * @param body The text to be sent to the LM Studio API.
   * @param dto Contains the GPT model and prompt.
   * @return The generated response from the LM Studio model.
   */
  private String createCallLmStudio(String body, OpenAiApiDTO dto) {
    // Allow URL override for testing (e.g., WireMock)
    String apiUrl =
        System.getProperty("lmstudio.api.url", "http://localhost:1234/v1/chat/completions");
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Type", "application/json");

    // Create a more flexible message structure
    List<Map<String, String>> messages = new ArrayList<>();

    // Determine role for the first message based on the model, Mistral models use "assistant" as
    // the first role and others use "system"
    String firstRole = dto.getGptModel().toLowerCase().contains("mistral") ? "assistant" : "system";

    // Add a system/assistant message that works with the model
    messages.add(Map.of("role", firstRole, "content", "You are a helpful assistant."));

    // Combine prompt and body for the user message
    messages.add(Map.of("role", "user", "content", dto.getPrompt() + "\n\n" + body));

    // Create the request body according to the OpenAI-compatible format
    // Using LinkedHashMap instead of HashMap for more consistent order
    Map<String, Object> requestBody = new java.util.LinkedHashMap<>();
    requestBody.put("model", dto.getGptModel());
    requestBody.put("messages", messages);
    requestBody.put("max_tokens", 4096);
    requestBody.put("temperature", 0.7);
    requestBody.put("stream", false);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

    // Log the request for debugging
    logger.info("Sending request to LM Studio: {}", requestBody);

    try {
      // Send the request and get the raw response
      String response = restTemplate.postForObject(apiUrl, entity, String.class);
      logger.info("LM Studio raw response: {}", response);

      return extractContentFromLmStudioResponse(response);
    } catch (HttpClientErrorException e) {
      logger.error("Error calling LM Studio API: {}", e.getResponseBodyAsString());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "LM Studio API error", e);
    } catch (Exception e) {
      logger.error("Unexpected error when calling LM Studio API", e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "LM Studio error", e);
    }
  }

  /**
   * Parses the response from the LM Studio API to extract the content. This method is more flexible
   * to handle potential variations in the response format.
   *
   * @param response The raw JSON response from the LM Studio API.
   * @return The extracted content from the response.
   */
  private String extractContentFromLmStudioResponse(String response) {
    try {
      // Log the raw response for debugging
      logger.info("Extracting content from LM Studio response: {}", response);

      // Assuming the response is a JSON string, parse it
      JSONObject jsonResponse = new JSONObject(response);

      // Check if we have a choices array
      if (jsonResponse.has("choices") && jsonResponse.getJSONArray("choices").length() > 0) {
        JSONArray choices = jsonResponse.getJSONArray("choices");
        JSONObject firstChoice = choices.getJSONObject(0);

        // Handle different response formats
        if (firstChoice.has("message") && firstChoice.getJSONObject("message").has("content")) {
          // Standard OpenAI format
          String content = firstChoice.getJSONObject("message").getString("content");
          logger.info("Extracted content using standard OpenAI format: {}", content);
          return content;
        } else if (firstChoice.has("text")) {
          // Some LLM servers might use this format
          String content = firstChoice.getString("text");
          logger.info("Extracted content using text format: {}", content);
          return content;
        } else if (firstChoice.has("content")) {
          // Another possible format
          String content = firstChoice.getString("content");
          logger.info("Extracted content using content format: {}", content);
          return content;
        } else if (firstChoice.has("delta") && firstChoice.getJSONObject("delta").has("content")) {
          // Streaming format sometimes used
          String content = firstChoice.getJSONObject("delta").getString("content");
          logger.info("Extracted content using delta format: {}", content);
          return content;
        }
      }

      // As a last resort, check if the response itself has a "content" field
      if (jsonResponse.has("content")) {
        String content = jsonResponse.getString("content");
        logger.info("Extracted content from root content field: {}", content);
        return content;
      }

      // If JSON parsing fails, try to extract text content between quotes
      if (response.contains("\"content\":")) {
        int startIndex = response.indexOf("\"content\":") + "\"content\":".length();
        if (response.substring(startIndex).contains("\"")) {
          startIndex = response.indexOf("\"", startIndex) + 1;
          int endIndex = response.indexOf("\"", startIndex);
          if (endIndex > startIndex) {
            String content = response.substring(startIndex, endIndex);
            logger.info("Extracted content using string parsing: {}", content);
            return content;
          }
        }
      }

      // If we can't find the content in the expected format, log the response and throw an error
      logger.error("Unexpected response format from LM Studio: {}", response);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected response format from LM Studio");
    } catch (JSONException e) {
      logger.error("Error parsing LM Studio API response: {}", response, e);

      // If JSON parsing fails, just return the raw response
      if (response != null && !response.isEmpty()) {
        logger.warn("Returning raw response due to parsing error");
        return "Raw response: " + response;
      }

      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error parsing LM Studio API response", e);
    }
  }

  /**
   * Retrieves the list of available GPT models from the OpenAI API.
   *
   * @param apiKey The API key for OpenAI.
   * @return A list of model names as strings.
   */
  public List<String> getGptModels(String apiKey) {

    if (apiKey == null || apiKey.trim().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OpenAI API key is missing.");
    }

    String url = System.getProperty("openai.api.url", "https://api.openai.com/v1/models");
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + apiKey);
    HttpEntity<String> entity = new HttpEntity<>(headers);

    try {
      String response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
      JSONObject jsonResponse = new JSONObject(response);
      JSONArray models = jsonResponse.getJSONArray("data");
      List<String> modelNames = new ArrayList<>();
      for (int i = 0; i < models.length(); i++) {
        JSONObject model = models.getJSONObject(i);
        modelNames.add(model.getString("id"));
      }

      System.out.println("Verfügbare GPT Modelle: " + modelNames);
      return modelNames;
    } catch (HttpClientErrorException e) {
      logger.error("Error retrieving models from OpenAI API: {}", e.getResponseBodyAsString());
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "OpenAI API error: " + e.getResponseBodyAsString(), e);
    } catch (RestClientException e) {
      logger.error("Error retrieving models from OpenAI API", e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving models from OpenAI API", e);
    } catch (JSONException e) {
      logger.error("Error parsing OpenAI API response", e);
      throw new RuntimeException("Error parsing OpenAI API response", e);
    } catch (Exception e) {
      logger.error(
          "An unexpected error occurred while retrieving models from OpenAI API: {}",
          e.getMessage(),
          e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Unexpected error retrieving models from OpenAI API.",
          e);
    }
  }

  /**
   * Retrieves the list of available Gemini models.
   *
   * @param apiKey The API key for Gemini.
   * @return A list of model names as strings.
   */
  public List<String> getGeminiModels(String apiKey) {

    if (apiKey == null || apiKey.trim().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gemini API key is missing.");
    }

    String baseUrl =
        System.getProperty(
            "gemini.api.url", "https://generativelanguage.googleapis.com/v1beta/models");
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Goog-Api-Key", apiKey);
    HttpEntity<String> entity = new HttpEntity<>(headers);

    try {
      String response =
          restTemplate.exchange(baseUrl, HttpMethod.GET, entity, String.class).getBody();
      JSONObject jsonResponse = new JSONObject(response);
      JSONArray models = jsonResponse.getJSONArray("models");
      List<String> modelNames = new ArrayList<>();
      for (int i = 0; i < models.length(); i++) {
        JSONObject model = models.getJSONObject(i);
        modelNames.add(model.getString("name"));
      }

      System.out.println("Verfügbare Gemini Modelle: " + modelNames);
      return modelNames;
    } catch (HttpClientErrorException e) {
      logger.error("Error retrieving models from Gemini API: {}", e.getResponseBodyAsString());
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Gemini API error: " + e.getResponseBodyAsString(), e);
    } catch (RestClientException e) {
      logger.error("Error retrieving models from Gemini API", e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving models from Gemini API", e);
    } catch (JSONException e) {
      logger.error("Error parsing Gemini API response", e);
      throw new RuntimeException("Error parsing Gemini API response", e);
    } catch (Exception e) {
      logger.error(
          "An unexpected error occurred while retrieving models from Gemini API: {}",
          e.getMessage(),
          e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Unexpected error retrieving models from Gemini API.",
          e);
    }
  }

  /**
   * Retrieves the list of available models from the LM Studio API.
   *
   * @return A list of model names as strings.
   */
  public List<String> getLmStudioModels() {
    // Allow URL override for testing (e.g., WireMock)
    String baseUrl = System.getProperty("lmstudio.api.url", "http://localhost:1234");
    // Handle both "/v1/chat/completions" and base URLs
    if (baseUrl.endsWith("/v1/chat/completions")) {
      baseUrl = baseUrl.replace("/v1/chat/completions", "");
    }
    String url = baseUrl + "/v1/models";

    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    HttpEntity<String> entity = new HttpEntity<>(headers);
    List<String> models = new ArrayList<>();

    try {
      String response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
      JSONObject jsonResponse = new JSONObject(response);
      JSONArray modelsArray = jsonResponse.getJSONArray("data");

      // Convert JSONArray to List manually to avoid compatibility issues
      for (int i = 0; i < modelsArray.length(); i++) {
        JSONObject model = modelsArray.getJSONObject(i);
        models.add(model.getString("id"));
      }
    } catch (Exception e) {
      logger.warn(
          "Error retrieving models from LM Studio API standard endpoint, trying v0 endpoint", e);

      try {
        // Try alternative endpoint
        url = baseUrl + "/api/v0/models";
        String response =
            restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
        JSONObject jsonResponse = new JSONObject(response);
        JSONArray modelsArray = jsonResponse.getJSONArray("data");

        // Convert JSONArray to List manually to avoid compatibility issues
        for (int i = 0; i < modelsArray.length(); i++) {
          JSONObject model = modelsArray.getJSONObject(i);
          models.add(model.getString("id"));
        }
      } catch (Exception alternativeError) {
        logger.error("Error retrieving models from LM Studio API", alternativeError);
        // Add a placeholder model so the UI doesn't break
        models.add("Model loading failed - check LM Studio server");
      }
    }

    // If no models were found, add a placeholder
    if (models.isEmpty()) {
      models.add("No models found - check LM Studio server");
    }

    return models;
  }
}
