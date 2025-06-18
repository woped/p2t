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
 * Service class to handle interaction with the OpenAI API. This service sends text to the API and
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
    }

    // Get Provider
    String provider = dto.getProvider();

    // Call provider
    String response = "";
    switch (provider) {
      case "openAi":
        response = createCallOpenAi(body, dto);
        break;
      case "gemini":
        response = createCallGemini(body, dto);
        break;
      case "llmStudio":
        response = createCallLlmStudio(body, dto);
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

    String url = System.getProperty("openai.api.url", "https://api.openai.com/v1");

    OpenAIClient client = OpenAIOkHttpClient.builder().apiKey(dto.getApiKey()).baseUrl(url).build();


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
  }

  /*
   * Creates the API Call for the Gemini API with the provided text and API details.
   *
   * @param body The text to be sent to the Gemini API.
   * @param openAiApiDTO Contains the API key, Gemini model, and prompt.
   * @return the api call for Gemini.
   */
  private String createCallGemini(String body, OpenAiApiDTO dto) {
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

    return response.text();
  }

  /*
   * Creates the API Call for the LlmStudio API with the provided text and API details.
   *
   * @param body The text to be sent to the LlmStudio API.
   * @param openAiApiDTO Contains the API key, GPT model, and prompt.
   * @return the api call for LlmStudio.
   */
  private String createCallLlmStudio(String body, OpenAiApiDTO dto) {

    return "";
  }

  /**
   * Retrieves the list of available GPT models from the OpenAI API.
   *
   * @param apiKey The API key for OpenAI.
   * @return A list of model names as strings.
   */
  public List<String> getGptModels(String apiKey) {
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
    }
  }

  /**
   * Retrieves the list of available Gemini models.
   *
   * @param apiKey The API key for Gemini.
   * @return A list of model names as strings.
   */
  public List<String> getGeminiModels(String apiKey) {
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
