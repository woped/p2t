package de.dhbw.woped.process2text;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.azure.core.credential.KeyCredential;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import de.dhbw.woped.process2text.model.process.OpenAiApiDTO;
import java.util.ArrayList;
import java.util.List;

public class TestLLM {

  public static void main(String[] args) {
    OpenAiApiDTO testDTO = new OpenAiApiDTO("", "gpt-3.5-turbo", "Count from 1 to 10");
    // System.out.println(callLLM("Count from 1 to 10, just list the numbers, nothing else.",
    // testDTO));

    OpenAiApiDTO testDTOGemini = new OpenAiApiDTO("", "gemini-1.5-pro", "Count from 1 to 10");
    // System.out.println(callLLMGemini("Count from 1 to 10, just list the numbers, nothing else.",
    // testDTOGemini));

    System.out.println(getGptModels(testDTO.getApiKey()));
    System.out.println(getGeminiModels(testDTOGemini.getApiKey()));
  }

  public static String callLLM(String body, OpenAiApiDTO openAiApiDTO) {
    System.out.println("Versuche Verbindung mit API-Key: " + openAiApiDTO.getApiKey());

    OpenAIClient client =
        new OpenAIClientBuilder()
            .credential(new KeyCredential(openAiApiDTO.getApiKey()))
            .buildClient();

    System.out.println("Client erstellt, sende Anfrage...");

    List<ChatRequestMessage> chatMessages = new ArrayList<>();
    chatMessages.add(new ChatRequestSystemMessage("You are a helpful assistant."));
    chatMessages.add(new ChatRequestUserMessage(openAiApiDTO.getPrompt()));
    chatMessages.add(new ChatRequestUserMessage(body));

    ChatCompletionsOptions options =
        new ChatCompletionsOptions(chatMessages).setMaxTokens(4096).setTemperature(0.7);

    try {
      ChatCompletions chatCompletions =
          client.getChatCompletions(openAiApiDTO.getGptModel(), options);
      String response = chatCompletions.getChoices().get(0).getMessage().getContent();
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

    OpenAIClient client =
        new OpenAIClientBuilder().credential(new KeyCredential(apiKey)).buildClient();

    return client.getModels();
  }

  public static List<String> getGeminiModels(String apiKey) {

    Client client = Client.builder().apiKey(apiKey).build();

    return client.listModels();
  }
}
