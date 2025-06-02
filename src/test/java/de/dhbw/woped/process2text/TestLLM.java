package de.dhbw.woped.process2text;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.models.Model;
import de.dhbw.woped.process2text.model.process.OpenAiApiDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TestLLM {

  public static void main(String[] args) {
    OpenAiApiDTO testDTO =
        new OpenAiApiDTO("", "gpt-3.5-turbo", "Count from 1 to 10", "openAi", false);
    // System.out.println(
    // callLLM("Count from 1 to 10, just list the numbers, nothing else.", testDTO));

    OpenAiApiDTO testDTOGemini =
        new OpenAiApiDTO("", "gemini-1.5-pro", "Count from 1 to 10", "gemini", false);
    // System.out.println(callLLMGemini("Count from 1 to 10, just list the numbers, nothing else.",
    // testDTOGemini));

    OpenAiApiDTO testDTOlmStudio =
        new OpenAiApiDTO(null, "llama-3.2-1b-instruct", "Count from 1 to 10", "lmStudio", false);

    System.out.println(getGptModels(testDTO.getApiKey()));
    // System.out.println(getGeminiModels(testDTOGemini.getApiKey()));
  }

  public static String callLLM(String body, OpenAiApiDTO openAiApiDTO) {
    System.out.println("Versuche Verbindung mit API-Key: " + openAiApiDTO.getApiKey());

    OpenAIClient client = OpenAIOkHttpClient.builder().apiKey(openAiApiDTO.getApiKey()).build();

    System.out.println("Client erstellt, sende Anfrage...");

    ChatCompletionCreateParams createParams =
        ChatCompletionCreateParams.builder()
            .model(openAiApiDTO.getGptModel())
            .maxCompletionTokens(4096)
            .temperature(0.7)
            .addSystemMessage("Tell me a story about building the best SDK!")
            .addUserMessage(openAiApiDTO.getPrompt())
            .addUserMessage(body)
            .build();

    ChatCompletion chatCompletion = client.chat().completions().create(createParams);

    try {
      String response = chatCompletion.choices().get(0).message().content().get();
      return response;
    } catch (Exception e) {
      System.err.println("Fehler beim API-Aufruf: " + e.getMessage());
      return "Fehler: " + e.getMessage();
    }
  }

  public static String callLLMGemini(String body, OpenAiApiDTO openAiApiDTO) {
    System.out.println("Versuche Verbindung mit API-Key: " + openAiApiDTO.getApiKey());

    Client client = Client.builder().apiKey(openAiApiDTO.getApiKey()).build();
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
            .parts(List.of(Part.fromText(openAiApiDTO.getPrompt() + body)))
            .build());

    GenerateContentResponse response =
        client.models.generateContent(openAiApiDTO.getGptModel(), chatMessages, config);
    System.out.println(response.text());

    return "";
  }

  public static List<String> getGptModels(String apiKey) {
    OpenAIClient client = OpenAIOkHttpClient.builder().apiKey(apiKey).build();

    List<Model> models = client.models().list().items();
    return models.stream().map(Model::id).collect(Collectors.toList());
  }
  // public static List<String> getGeminiModels(String apiKey) {

  //   Client client = Client.builder().apiKey(apiKey).build();

  //   return client.listModels();
  // }
}
