package de.dhbw.woped.process2text.model.process;

/** Data Transfer Object to hold OpenAI API related information. */
public class OpenAiApiDTO {

  public OpenAiApiDTO(
      String apiKey, String gptModel, String prompt, String provider, boolean useRAG) {
    this.apiKey = apiKey;
    this.gptModel = gptModel;
    this.prompt = prompt;
    this.provider = provider;
    this.useRAG = useRAG;
  }

  private String apiKey;
  private String gptModel;
  private String prompt;
  private String provider;
  private boolean useRAG;

  public String getApiKey() {
    return apiKey;
  }

  public String getGptModel() {
    return gptModel;
  }

  public String getPrompt() {
    return prompt;
  }

  public void setGptModel(String gptModel) {
    this.gptModel = gptModel;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public void setPrompt(String prompt) {
    this.prompt = prompt;
  }
}
