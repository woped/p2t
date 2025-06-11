package de.dhbw.woped.process2text.service;

import de.dhbw.woped.process2text.controller.P2TController;
import de.dhbw.woped.process2text.model.process.OpenAiApiDTO;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
  public String callLLM(String body, OpenAiApiDTO openAiApiDTO) {
    String apiUrl = "https://api.openai.com/v1/chat/completions";
    // Use the Transformer API if the provided processmodell is a PNML to parse it
    // into an BPMN
    TransformerService transformerService = new TransformerService();
    if (transformerService.checkForBPMNorPNML(body).equals("PNML")) {
      body = transformerService.transform("pnmltobpmn", body);
    }
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + openAiApiDTO.getApiKey());
    headers.setContentType(MediaType.APPLICATION_JSON);

    // Create the request body with the specified model, messages, max tokens, and
    // temperature
    Map<String, Object> requestBody = new HashMap<>();
    requestBody.put("model", openAiApiDTO.getGptModel());
    requestBody.put(
        "messages",
        List.of(
            Map.of("role", "system", "content", "You are a helpful assistant."),
            Map.of("role", "user", "content", openAiApiDTO.getPrompt()),
            Map.of("role", "user", "content", body)));
    requestBody.put("max_tokens", 4096);
    requestBody.put("temperature", 0.7);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

    try {
      // Send the request to the OpenAI API and get the response as a string
      String response = restTemplate.postForObject(apiUrl, entity, String.class);
      // Parse the response to extract the content
      return extractContentFromResponse(response);
    } catch (HttpClientErrorException e) {
      logger.error("Error calling OpenAI API: {}", e.getResponseBodyAsString());
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "OpenAI API error: " + e.getResponseBodyAsString(), e);
    } catch (RestClientException e) {
      logger.error("Error calling OpenAI API", e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error calling OpenAI API", e);
    }
  }

  public String callLLM2(String body, OpenAiApiDTO dto) {
    // Use the Transformer API if the provided processmodell is a PNML to parse it
    // into an BPMN
    TransformerService transformerService = new TransformerService();
    if ("PNML".equals(transformerService.checkForBPMNorPNML(body))) {
      body = transformerService.transform("pnmltobpmn", body);
    }

    // Get Provider
    String provider = ""; // = dto.getProvider();

    // Call provider
    String apiCallString = "";
    if (provider.equals("openai")) {
      apiCallString = createCallOpenAi(body, dto);
    } else if (provider.equals("gemini")) {
      apiCallString = createCallGemini(body, dto);
    } else if (provider.equals("llmStudio")) {
      apiCallString = createCallLmStudio(body, dto);
    }

    return callAPI(apiCallString);
  }

  private String callAPI(String apiCallString) {
    return "";
  }
  ;

  /*
   * Creates the API Call for the OpenAI API with the provided text and API details.
   *
   * @param body The text to be sent to the OpenAI API.
   * @param openAiApiDTO Contains the API key, GPT model, and prompt.
   * @return the api call for Open Ai.
   */
  private String createCallOpenAi(String body, OpenAiApiDTO dto) {
    //
    return "";
  }

  /*
   * Creates the API Call for the Gemini API with the provided text and API details.
   *
   * @param body The text to be sent to the Gemini API.
   * @param openAiApiDTO Contains the API key, Gemini model, and prompt.
   * @return the api call for Gemini.
   */
  private String createCallGemini(String body, OpenAiApiDTO dto) {

    return "";
  }

  /**
   * Creates the API call for the LM Studio API using the provided text and API details.
   *
   * @param body The text to be sent to the LM Studio API.
   * @param dto Contains the GPT model and prompt.
   * @return The generated response from the LM Studio model.
   */
  private String createCallLmStudio(String body, OpenAiApiDTO dto) {
    String apiUrl = "http://localhost:1234/v1/chat/completions"; // Default LM Studio API endpoint

    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Type", "application/json");

    // Determine role for the first message based on the model, Mistral models use "assistant" as
    // the first role
    // and others use "system"
    String firstRole = dto.getGptModel().toLowerCase().contains("mistral") ? "assistant" : "system";

    // Create the request body according to the OpenAI-compatible format
    Map<String, Object> requestBody =
        Map.of(
            "model",
            dto.getGptModel(),
            "messages",
            List.of(
                Map.of("role", firstRole, "content", "You are a helpful assistant."),
                Map.of("role", "user", "content", dto.getPrompt() + body)),
            "max_tokens",
            4096,
            "temperature",
            0.7);

    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

    try {
      // Send the request and parse the response
      String response = restTemplate.postForObject(apiUrl, entity, String.class);
      logger.info("LM Studio raw response: {}", response);

      return extractContentFromResponse(response);

    } catch (HttpClientErrorException e) {
      logger.error("Error calling LM Studio API: {}", e.getResponseBodyAsString());
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "LM Studio API error", e);
    } catch (Exception e) {
      logger.error("Unexpected error when calling LM Studio API", e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "LM Studio error", e);
    }
  }

  /**
   * Retrieves the list of available GPT models from the OpenAI API.
   *
   * @param apiKey The API key for OpenAI.
   * @return A list of model names as strings.
   */
  public List<String> getGptModels(String apiKey) {
    String url = "https://api.openai.com/v1/models";
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + apiKey);
    HttpEntity<String> entity = new HttpEntity<>(headers);

    try {
      String response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
      JSONObject jsonResponse = new JSONObject(response);
      JSONArray models = jsonResponse.getJSONArray("data");
      return models.toList().stream()
          .map(model -> ((Map<String, Object>) model).get("id").toString())
          .collect(Collectors.toList());
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
    }
  }

  /**
   * Retrieves the list of available Gemini models.
   *
   * @param apiKey The API key for Gemini.
   * @return A list of model names as strings.
   */
  public List<String> getGeminiModels(String apiKey) {
    String url = "https://generativelanguage.googleapis.com/v1beta/models";
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.set("X-Goog-Api-Key", apiKey);
    HttpEntity<String> entity = new HttpEntity<>(headers);

    try {
      String response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
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
    }
  }

  /**
   * Retrieves the list of available models from the LM Studio API.
   *
   * @return A list of model names as strings.
   */
  public List<String> getLmStudioModels() {
    String url = "http://localhost:1234/api/v0/models";
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    HttpEntity<String> entity = new HttpEntity<>(headers);

    try {
      String response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
      JSONObject jsonResponse = new JSONObject(response);
      JSONArray models = jsonResponse.getJSONArray("data");

      return models.toList().stream()
          .map(model -> ((Map<String, Object>) model).get("id").toString())
          .collect(Collectors.toList());
    } catch (HttpClientErrorException e) {
      logger.error("Error retrieving models from LM Studio API: {}", e.getResponseBodyAsString());
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "LM Studio API error: " + e.getResponseBodyAsString(), e);
    } catch (RestClientException e) {
      logger.error("Error retrieving models from LM Studio API", e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving models from LM Studio API", e);
    } catch (JSONException e) {
      logger.error("Error parsing LM Studio API response", e);
      throw new RuntimeException("Error parsing LM Studio API response", e);
    }
  }

  /**
   * Parses the response from the OpenAI API to extract the content.
   *
   * @param response The raw JSON response from the OpenAI API.
   * @return The extracted content from the response.
   */
  private String extractContentFromResponse(String response) {
    try {
      // Assuming the response is a JSON string, parse it
      JSONObject jsonResponse = new JSONObject(response);
      JSONArray choices = jsonResponse.getJSONArray("choices");
      if (choices.length() > 0) {
        // Get the first choice and extract the message content
        JSONObject firstChoice = choices.getJSONObject(0);
        JSONObject message = firstChoice.getJSONObject("message");
        return message.getString("content");
      } else {
        throw new ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR, "No choices found in the response");
      }
    } catch (JSONException e) {
      logger.error("Error parsing OpenAI API response", e);
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Error parsing OpenAI API response", e);
    }
  }
}
