package de.dhbw.woped.process2text.model.process;

/** Data Transfer Object to hold OpenAI API related information. */
public class OpenAiApiDTO {

  // Gemini wird als Provider hardgecoded -- Nur Testwecke!
  public OpenAiApiDTO(String provider, String apiKey, String gptModel, String prompt) {
    this.provider = provider;
    this.apiKey = apiKey;
    this.gptModel = gptModel;
    this.prompt = prompt;
  }

  private String provider;
  private String apiKey;
  private String gptModel;
  private String prompt;

  // Kann wieder entfallen, wenn der Provider nicht mehr hardgecoded wird
  public String getprovider() {
    return provider;
  }

  public String getApiKey() {
    return apiKey;
  }

  public String getGptModel() {
    return gptModel;
  }

  public String getPrompt() {
    return prompt;
  }

  // Kann wieder entfallen, wenn der Provider nicht mehr hardgecoded wird
  public void setprovider(String provider) {
    this.provider = provider;
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
