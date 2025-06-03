package de.dhbw.woped.process2text;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

public class Test_getGeminiModels {
  private static final Logger logger = LoggerFactory.getLogger(Test_getGeminiModels.class);

  @Test
  public void testGetGeminiModels() {
    List<String> models = getGeminiModels("AIzaSyBYm0RnSVWvM2hMs55iuL-J2IXy2gcD6jg");
    System.out.println("Verfügbare Gemini Modelle: " + models);
  }

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
}