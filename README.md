# Process to Text (P2T)

This webservice is used to translate a petrinet into plain text.

# Live demo

| URL                                             | Description | 
|-------------------------------------------------|:-----------:|
| https://woped.dhbw-karlsruhe.de/p2t/            | Embedded UI |
| https://woped.dhbw-karlsruhe.de/p2t/swagger-ui/ | Swagger UI  |

# Related repositories

| URL                               |       Description       |
|-----------------------------------|:-----------------------:|
| https://github.com/tfreytag/T2P   | Text2Process Webservice |
| https://github.com/tfreytag/WoPeD |      WoPeD-Client       |

# Resources

| URL                                         | Description |
|---------------------------------------------|:-----------:|
| https://hub.docker.com/r/woped/process2text | Docker Hub  |

# Requirements for development

* IDE of your choice
* Java 11

# Configuration guide

_It is recommended to use IntelliJ IDE._

1. Git clone this project onto your machine.
2. Start IntelliJ and open the project.
3. Wait until all files have been loaded.
4. Run Application with the Start-Button or with `mvn spring-boot:run`

After cloning this repository, it's essential to [set up git hooks](https://github.com/woped/woped-git-hooks/blob/main/README.md#activating-git-hooks-after-cloning-a-repository) to ensure project standards.

# Testing

### Testing via Swagger UI

1. Start the application.
2. Navigate to `http://localhost:8080/p2t/swagger-ui.`
3. Paste your petrinet (the content of the xml file) in the body of the `POST /p2t/generateText` endpoint.

### Testing via the embedded GUI

1. Start the application.
2. Navigate to `http://localhost:8080/p2t/`.
3. Paste your petrinet (the content of the xml file) in the first text area and submit the form.

### Testing with Postman

1. Add a new collection in Postman.
2. Add a new request in your created collection.
3. For your request change `Get` to `Post`.
4. Enter URL `http://localhost:8080/p2t/generateText`
5. Open the body configuration and choose `raw`.
6. Copy the content of a `.pnml` file (must be a sound petrinet) in the body of the request.
7. Click send button

### Testing via the WoPeD-Client

1. Start the application.
2. Follow the installation instructions of the WoPeD-Client (`https://github.com/tfreytag/WoPeD`).
3. Start WoPeD-Client and.
4. Open the configuration and navigate to `NLP Tools`. Adapt the `Process2Text` configuration:
    - `Server host`: `localhost`
    - `Port`: `8080`
    - `URI`: `/p2t`
5. Test your configuration.
6. Close the configuration and import or create a new petrinet.
7. Navigate to `Analyse` -> `Translate to text` and execute. The petrinet will now be transformed by your locally
   started P2T webservice.

# Hosting the webservice yourself

### Option 1: Use our pre-build docker image

1. Pull our pre-build docker image from docker hub (see above).
2. Run this image on your server.

### Option 2: Build the docker image yourself

1. Build your own docker image with the Dockerfile.
2. Run this image on your server.

# Dependencies
This repository uses jars that are unavailable on Maven central. Hence, these jar files are stored in this repository in
the folder `lib`. The chosen procedure was described in this [SO answer](https://stackoverflow.com/a/51647143/11711692).

# Formatting
To check the formatting of all Java files, run `mvn spotless:check`. <br>
If formatting are identified, run `mvn spotless:apply` to automatically reformat that affected files.

# LM Studio Integration

## LM Studio Setup

1. **Download LM Studio**:
   - Visit the official [LM Studio website](https://lmstudio.ai/) and download the latest version for your operating system.
   - Alternatively, you can download LM Studio from [GitHub](https://github.com/lmstudio-ai).

2. **Install and launch LM Studio**:
   - Run the downloaded installation file and follow the instructions.
   - Launch LM Studio after installation.

3. **Download and load a model**:
   - In LM Studio, navigate to the "Models" section.
   - Choose a model to download or import an already downloaded model.
      <br>
      <img src="docs/images/Bildschirmfoto%202025-06-27%20um%2011.26.43.png" alt="LM Studio Model Selection" width="400"/>
   - Ensure the context length is set correctly, 10 000 tokens suits most cases, then load the model.
      <br>
      <img src="docs/images/Bildschirmfoto%202025-06-27%20um%2011.21.19.png" alt="LM Studio Model Download" width="400"/>
   - Start the local server with the loaded model by clicking "Start Server".

4. **Verify the server is running**:
   - The LM Studio server should be running on `http://localhost:1234` by default.
      <br>
      <img src="docs/images/Bildschirmfoto%202025-06-27%20um%2011.28.55.png" alt="LM Studio Model Download" width="400"/>
   - You can test the API availability by accessing `http://localhost:1234/v1/models` in your browser.


## Using with WoPeD

1. **Configure the web client**:
   - Start this P2T service and for example the web client.
   - Navigate to "P2T (Process2Text)".
   - Select "lmStudio" as the provider.
   - No API key is needed for LM Studio since it runs locally.

2. **Use Process2Text with LM Studio**:
   - Select "lmStudio" as the provider.
   - Choose the desired model from the dropdown list.
   - Execute the translation.

3. **Troubleshooting**:
   - Ensure the LM Studio server is running before starting a translation.
   - If no models are displayed, verify that LM Studio was started correctly and a model is loaded.
   - For connection issues, check if the default URL `http://localhost:1234` is accessible.