
import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;

public class TestLLM{

    public static void main(String[] args){
        
        testDTO = new OpenAiApiDTO();
        testDTO
       System.out.println(callLLM("Test",openAiApiDTO));
    }
    public String callLLM(String body, OpenAiApiDTO openAiApiDTO) {
    OpenAIClient client =
        new OpenAIClientBuilder()
            .credential(new KeyCredential(openAiApiDTO.getApiKey()))
            .buildClient();

    List<ChatRequestMessage> chatMessages = new ArrayList<>();
    chatMessages.add(new ChatRequestSystemMessage("You are a helpful assistant."));
    chatMessages.add(new ChatRequestUserMessage(openAiApiDTO.getPrompt()));
    chatMessages.add(new ChatRequestUserMessage(body));

    ChatCompletionsOptions options =
        new ChatCompletionsOptions(chatMessages).setMaxTokens(4096).setTemperature(0.7);

    ChatCompletions chatCompletions =
        client.getChatCompletions(openAiApiDTO.getGptModel(), options);

    String response = chatCompletions.getChoices().get(0).getMessage().getContent();

    return extractContentFromResponse(response);
    }
}