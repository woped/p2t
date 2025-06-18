package de.dhbw.woped.process2text;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.dhbw.woped.process2text.model.process.OpenAiApiDTO;
import de.dhbw.woped.process2text.service.P2TLLMService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.EnableWireMock;



@SpringBootTest
@EnableWireMock
class Process2textApplicationTests {

  @Value("${wiremock.server.baseUrl}")
  private String wiremockBaseUrl;

  @Autowired private P2TLLMService p2tllmService;


  @Test
  void contextLoads() {}

  @Test
  void testGetGeminiModels() {
    stubFor(
        get(urlPathEqualTo("/v1beta/models"))
            .withHeader("X-Goog-Api-Key", equalTo("test-api-key"))
            .willReturn(okJson("{\"models\":[{\"name\":\"mod1\"},{\"name\":\"mod2\"}]}")));

    System.setProperty("gemini.api.baseUrl", wiremockBaseUrl);
    System.setProperty("gemini.api.url", wiremockBaseUrl + "/v1beta/models");

    List<String> models = p2tllmService.getGeminiModels("test-api-key");
    assertNotNull(models);
    assertEquals(2, models.size());
    assertTrue(models.contains("mod1"));
    assertTrue(models.contains("mod2"));
  }

  @Test
  void testGetGptModels() {
    stubFor(
        get(urlPathEqualTo("/v1/models"))
            .withHeader("Authorization", equalTo("Bearer test-api-key"))
            .willReturn(okJson("{\"data\":[{\"id\":\"gpt-4\"},{\"id\":\"gpt-3.5-turbo\"}]}")));

    System.setProperty("openai.api.url", wiremockBaseUrl + "/v1/models");

    List<String> models = p2tllmService.getGptModels("test-api-key");
    assertNotNull(models);
    assertEquals(2, models.size());
    assertTrue(models.contains("gpt-4"));
    assertTrue(models.contains("gpt-3.5-turbo"));
  }

  @Test
  void testCallLLM() {
    String body =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + //
            "<bpmn2:definitions xmlns:bpmn2=\"http://www.omg.org/spec/BPMN/20100524/MODEL\""
          + " xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\""
          + " xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\""
          + " xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" id=\"empty-definitions\""
          + " targetNamespace=\"http://bpmn.io/schema/bpmn\"><bpmn2:collaboration"
          + " id=\"Collaboration_id-12a40ec6-9fc5-4b12-aaa1-d8f8bc718b41\"><bpmn2:participant"
          + " id=\"id-54d8a5a1-c344-492c-bf9f-b29ad9662303\" name=\"Customer\""
          + " processRef=\"Process_id-54d8a5a1-c344-492c-bf9f-b29ad9662303\" /><bpmn2:participant"
          + " id=\"id-ba446fe3-fc5a-468a-879f-636efa8ae458\" name=\"Car Wash Machine\""
          + " processRef=\"Process_id-ba446fe3-fc5a-468a-879f-636efa8ae458\"><bpmn2:documentation>&lt;p"
          + " class=\"m-0 mb-[8px] relative"
          + " last:mb-0\"&gt;&lt;br&gt;&lt;/p&gt;</bpmn2:documentation></bpmn2:participant><bpmn2:messageFlow"
          + " id=\"id-4d638cc4-851d-4b91-b5af-9e6957efa908\" name=\"\""
          + " sourceRef=\"id-f8d9d1e1-c93c-4092-bebb-038274d493b7\""
          + " targetRef=\"id-394d3246-4552-4999-b0f4-2c11960a843e\" /><bpmn2:messageFlow"
          + " id=\"id-9ab5afb1-3d22-4a19-8630-b1231247348d\" name=\"\""
          + " sourceRef=\"id-5953c915-9df3-4c62-b85e-5ac1aa52a21b\""
          + " targetRef=\"id-c14199c8-b786-4fa5-b985-bbb856303824\" /><bpmn2:messageFlow"
          + " id=\"id-cfeb8788-b62b-4cf5-8592-1fce0893264f\" name=\"\""
          + " sourceRef=\"id-de9b2fec-0186-4892-aefa-b6a905e4db1e\""
          + " targetRef=\"id-394d3246-4552-4999-b0f4-2c11960a843e\""
          + " /></bpmn2:collaboration><bpmn2:process"
          + " id=\"Process_id-54d8a5a1-c344-492c-bf9f-b29ad9662303\" name=\"Customer\"><bpmn2:task"
          + " id=\"id-c14199c8-b786-4fa5-b985-bbb856303824\" name=\"Drives"
          + " away\"><bpmn2:outgoing>id-c25381f4-76c6-4098-885a-8fc28c7c6d05</bpmn2:outgoing></bpmn2:task><bpmn2:endEvent"
          + " id=\"id-d4d348ba-02b9-4f83-8106-257e0e1a5500\""
          + " name=\"\"><bpmn2:incoming>id-c25381f4-76c6-4098-885a-8fc28c7c6d05</bpmn2:incoming></bpmn2:endEvent><bpmn2:task"
          + " id=\"id-3921bc36-892b-410f-a1c6-2a043833d753\" name=\"Pulls car up to car"
          + " wash\"><bpmn2:incoming>id-1258cd0e-8bc5-4c52-85b8-567500708524</bpmn2:incoming><bpmn2:outgoing>id-25164739-c8de-4780-8bca-9a02c2340979</bpmn2:outgoing></bpmn2:task><bpmn2:task"
          + " id=\"id-91a31efc-38eb-4075-ba95-8eecebe90a48\" name=\"Chooses wash"
          + " \"><bpmn2:incoming>id-25164739-c8de-4780-8bca-9a02c2340979</bpmn2:incoming><bpmn2:outgoing>id-c9a4b533-5a8b-4a30-b9c4-bf300bd9f6e4</bpmn2:outgoing></bpmn2:task><bpmn2:task"
          + " id=\"id-f8d9d1e1-c93c-4092-bebb-038274d493b7\" name=\"Pay"
          + " 8\"><bpmn2:documentation>&lt;p class=\"m-0 mb-[8px] relative"
          + " last:mb-0\"&gt;&lt;br&gt;&lt;/p&gt;</bpmn2:documentation><bpmn2:incoming>id-205183a7-69dd-425b-af32-cbfcd0b7c740</bpmn2:incoming></bpmn2:task><bpmn2:exclusiveGateway"
          + " id=\"id-6bd3013b-8646-4137-ade7-499b31893131\" name=\"Which wash"
          + " program?\"><bpmn2:documentation>&lt;p class=\"m-0 mb-[8px] relative"
          + " last:mb-0\"&gt;&lt;br&gt;&lt;/p&gt;</bpmn2:documentation><bpmn2:incoming>id-c9a4b533-5a8b-4a30-b9c4-bf300bd9f6e4</bpmn2:incoming><bpmn2:outgoing>id-88ed032f-2ad4-4688-987e-77b41bb11ede</bpmn2:outgoing><bpmn2:outgoing>id-205183a7-69dd-425b-af32-cbfcd0b7c740</bpmn2:outgoing></bpmn2:exclusiveGateway><bpmn2:task"
          + " id=\"id-de9b2fec-0186-4892-aefa-b6a905e4db1e\" name=\"Pays"
          + " 15\"><bpmn2:documentation>&lt;p class=\"m-0 mb-[8px] relative"
          + " last:mb-0\"&gt;&lt;br&gt;&lt;/p&gt;</bpmn2:documentation><bpmn2:incoming>id-88ed032f-2ad4-4688-987e-77b41bb11ede</bpmn2:incoming></bpmn2:task><bpmn2:startEvent"
          + " id=\"id-add7903c-fc54-40ce-9fa5-d7f5894d584c\""
          + " name=\"\"><bpmn2:outgoing>id-1258cd0e-8bc5-4c52-85b8-567500708524</bpmn2:outgoing></bpmn2:startEvent><bpmn2:sequenceFlow"
          + " id=\"id-c25381f4-76c6-4098-885a-8fc28c7c6d05\" name=\"\""
          + " sourceRef=\"id-c14199c8-b786-4fa5-b985-bbb856303824\""
          + " targetRef=\"id-d4d348ba-02b9-4f83-8106-257e0e1a5500\" /><bpmn2:sequenceFlow"
          + " id=\"id-88ed032f-2ad4-4688-987e-77b41bb11ede\" name=\"Polish Plus\""
          + " sourceRef=\"id-6bd3013b-8646-4137-ade7-499b31893131\""
          + " targetRef=\"id-de9b2fec-0186-4892-aefa-b6a905e4db1e\" /><bpmn2:sequenceFlow"
          + " id=\"id-1258cd0e-8bc5-4c52-85b8-567500708524\" name=\"\""
          + " sourceRef=\"id-add7903c-fc54-40ce-9fa5-d7f5894d584c\""
          + " targetRef=\"id-3921bc36-892b-410f-a1c6-2a043833d753\" /><bpmn2:sequenceFlow"
          + " id=\"id-205183a7-69dd-425b-af32-cbfcd0b7c740\" name=\"ECO\""
          + " sourceRef=\"id-6bd3013b-8646-4137-ade7-499b31893131\""
          + " targetRef=\"id-f8d9d1e1-c93c-4092-bebb-038274d493b7\" /><bpmn2:sequenceFlow"
          + " id=\"id-25164739-c8de-4780-8bca-9a02c2340979\" name=\"\""
          + " sourceRef=\"id-3921bc36-892b-410f-a1c6-2a043833d753\""
          + " targetRef=\"id-91a31efc-38eb-4075-ba95-8eecebe90a48\" /><bpmn2:sequenceFlow"
          + " id=\"id-c9a4b533-5a8b-4a30-b9c4-bf300bd9f6e4\" name=\"\""
          + " sourceRef=\"id-91a31efc-38eb-4075-ba95-8eecebe90a48\""
          + " targetRef=\"id-6bd3013b-8646-4137-ade7-499b31893131\""
          + " /></bpmn2:process><bpmn2:process"
          + " id=\"Process_id-ba446fe3-fc5a-468a-879f-636efa8ae458\" name=\"Car Wash"
          + " Machine\"><bpmn2:task id=\"id-6d5ef94f-fbf3-4a8a-b2ae-d456469c1895\" name=\"Double"
          + " Polish\"><bpmn2:incoming>id-de245238-6e8a-4d57-b623-72f986474d09</bpmn2:incoming><bpmn2:outgoing>id-8a6a0d49-851e-465d-9659-f592a8a5c9be</bpmn2:outgoing></bpmn2:task><bpmn2:task"
          + " id=\"id-5953c915-9df3-4c62-b85e-5ac1aa52a21b\""
          + " name=\"Dry\"><bpmn2:incoming>id-79bb7477-c416-4163-9f93-7888a2dba8eb</bpmn2:incoming><bpmn2:incoming>id-9b9aa774-0225-4a45-b8a5-0a4060c75cc2</bpmn2:incoming></bpmn2:task><bpmn2:task"
          + " id=\"id-740aff66-728c-497b-81e8-cfcdf074d3de\" name=\"Wheel Luster Wheel"
          + " Clean\"><bpmn2:incoming>id-ad7f7a53-cfc9-433e-834b-265393ef2cc3</bpmn2:incoming><bpmn2:outgoing>id-9b9aa774-0225-4a45-b8a5-0a4060c75cc2</bpmn2:outgoing></bpmn2:task><bpmn2:task"
          + " id=\"id-394d3246-4552-4999-b0f4-2c11960a843e\" name=\"Soft Cloth"
          + " Wash\"><bpmn2:outgoing>id-661afac8-579e-4dc0-9b7a-5f5dd50b96eb</bpmn2:outgoing></bpmn2:task><bpmn2:exclusiveGateway"
          + " id=\"id-eb413d52-5649-47d4-9c0a-1fe4b0fdbf98\" name=\"Which wash"
          + " program?\"><bpmn2:documentation>&lt;p class=\"m-0 mb-[8px] relative"
          + " last:mb-0\"&gt;&lt;br&gt;&lt;/p&gt;</bpmn2:documentation><bpmn2:incoming>id-661afac8-579e-4dc0-9b7a-5f5dd50b96eb</bpmn2:incoming><bpmn2:outgoing>id-7297e7a8-c9b6-4154-93e0-02fec7ce9b85</bpmn2:outgoing><bpmn2:outgoing>id-de245238-6e8a-4d57-b623-72f986474d09</bpmn2:outgoing></bpmn2:exclusiveGateway><bpmn2:task"
          + " id=\"id-e5d2c95a-7d1d-450a-bc94-7fadc95d356c\" name=\"Wheel"
          + " Clean\"><bpmn2:incoming>id-7297e7a8-c9b6-4154-93e0-02fec7ce9b85</bpmn2:incoming><bpmn2:outgoing>id-79bb7477-c416-4163-9f93-7888a2dba8eb</bpmn2:outgoing></bpmn2:task><bpmn2:task"
          + " id=\"id-b069b6b0-8929-4de3-bb09-b5719c98c5a7\" name=\"Clear Coat"
          + " Protection\"><bpmn2:incoming>id-8a6a0d49-851e-465d-9659-f592a8a5c9be</bpmn2:incoming><bpmn2:outgoing>id-ad7f7a53-cfc9-433e-834b-265393ef2cc3</bpmn2:outgoing></bpmn2:task><bpmn2:sequenceFlow"
          + " id=\"id-8a6a0d49-851e-465d-9659-f592a8a5c9be\" name=\"\""
          + " sourceRef=\"id-6d5ef94f-fbf3-4a8a-b2ae-d456469c1895\""
          + " targetRef=\"id-b069b6b0-8929-4de3-bb09-b5719c98c5a7\" /><bpmn2:sequenceFlow"
          + " id=\"id-79bb7477-c416-4163-9f93-7888a2dba8eb\" name=\"\""
          + " sourceRef=\"id-e5d2c95a-7d1d-450a-bc94-7fadc95d356c\""
          + " targetRef=\"id-5953c915-9df3-4c62-b85e-5ac1aa52a21b\" /><bpmn2:sequenceFlow"
          + " id=\"id-ad7f7a53-cfc9-433e-834b-265393ef2cc3\" name=\"\""
          + " sourceRef=\"id-b069b6b0-8929-4de3-bb09-b5719c98c5a7\""
          + " targetRef=\"id-740aff66-728c-497b-81e8-cfcdf074d3de\" /><bpmn2:sequenceFlow"
          + " id=\"id-7297e7a8-c9b6-4154-93e0-02fec7ce9b85\" name=\"ECO\""
          + " sourceRef=\"id-eb413d52-5649-47d4-9c0a-1fe4b0fdbf98\""
          + " targetRef=\"id-e5d2c95a-7d1d-450a-bc94-7fadc95d356c\" /><bpmn2:sequenceFlow"
          + " id=\"id-de245238-6e8a-4d57-b623-72f986474d09\" name=\"Polish Plus\""
          + " sourceRef=\"id-eb413d52-5649-47d4-9c0a-1fe4b0fdbf98\""
          + " targetRef=\"id-6d5ef94f-fbf3-4a8a-b2ae-d456469c1895\" /><bpmn2:sequenceFlow"
          + " id=\"id-9b9aa774-0225-4a45-b8a5-0a4060c75cc2\" name=\"\""
          + " sourceRef=\"id-740aff66-728c-497b-81e8-cfcdf074d3de\""
          + " targetRef=\"id-5953c915-9df3-4c62-b85e-5ac1aa52a21b\" /><bpmn2:sequenceFlow"
          + " id=\"id-661afac8-579e-4dc0-9b7a-5f5dd50b96eb\" name=\"\""
          + " sourceRef=\"id-394d3246-4552-4999-b0f4-2c11960a843e\""
          + " targetRef=\"id-eb413d52-5649-47d4-9c0a-1fe4b0fdbf98\""
          + " /></bpmn2:process><bpmndi:BPMNDiagram"
          + " id=\"id-12a40ec6-9fc5-4b12-aaa1-d8f8bc718b41di\"><bpmndi:BPMNPlane"
          + " id=\"id-12a40ec6-9fc5-4b12-aaa1-d8f8bc718b41_plane\""
          + " bpmnElement=\"Collaboration_id-12a40ec6-9fc5-4b12-aaa1-d8f8bc718b41\"><bpmndi:BPMNShape"
          + " id=\"id-54d8a5a1-c344-492c-bf9f-b29ad9662303_shape\""
          + " bpmnElement=\"id-54d8a5a1-c344-492c-bf9f-b29ad9662303\""
          + " isHorizontal=\"true\"><dc:Bounds x=\"20\" y=\"0\" width=\"910\" height=\"250\""
          + " /></bpmndi:BPMNShape><bpmndi:BPMNShape"
          + " id=\"id-ba446fe3-fc5a-468a-879f-636efa8ae458_shape\""
          + " bpmnElement=\"id-ba446fe3-fc5a-468a-879f-636efa8ae458\""
          + " isHorizontal=\"true\"><dc:Bounds x=\"20\" y=\"280\" width=\"910\" height=\"250\""
          + " /></bpmndi:BPMNShape><bpmndi:BPMNShape"
          + " id=\"id-c14199c8-b786-4fa5-b985-bbb856303824_shape\""
          + " bpmnElement=\"id-c14199c8-b786-4fa5-b985-bbb856303824\"><dc:Bounds x=\"780\" y=\"85\""
          + " width=\"100\" height=\"80\" /></bpmndi:BPMNShape><bpmndi:BPMNShape"
          + " id=\"id-6d5ef94f-fbf3-4a8a-b2ae-d456469c1895_shape\""
          + " bpmnElement=\"id-6d5ef94f-fbf3-4a8a-b2ae-d456469c1895\"><dc:Bounds x=\"470\""
          + " y=\"429\" width=\"100\" height=\"80\" /></bpmndi:BPMNShape><bpmndi:BPMNShape"
          + " id=\"id-d4d348ba-02b9-4f83-8106-257e0e1a5500_shape\""
          + " bpmnElement=\"id-d4d348ba-02b9-4f83-8106-257e0e1a5500\"><dc:Bounds x=\"872\" y=\"22\""
          + " width=\"36\" height=\"36\" /><bpmndi:BPMNLabel><dc:Bounds x=\"840\" y=\"56\""
          + " width=\"100\" height=\"40\" /></bpmndi:BPMNLabel></bpmndi:BPMNShape><bpmndi:BPMNShape"
          + " id=\"id-5953c915-9df3-4c62-b85e-5ac1aa52a21b_shape\""
          + " bpmnElement=\"id-5953c915-9df3-4c62-b85e-5ac1aa52a21b\"><dc:Bounds x=\"780\""
          + " y=\"310\" width=\"100\" height=\"80\" /></bpmndi:BPMNShape><bpmndi:BPMNShape"
          + " id=\"id-740aff66-728c-497b-81e8-cfcdf074d3de_shape\""
          + " bpmnElement=\"id-740aff66-728c-497b-81e8-cfcdf074d3de\"><dc:Bounds x=\"780\""
          + " y=\"429\" width=\"100\" height=\"80\" /></bpmndi:BPMNShape><bpmndi:BPMNShape"
          + " id=\"id-3921bc36-892b-410f-a1c6-2a043833d753_shape\""
          + " bpmnElement=\"id-3921bc36-892b-410f-a1c6-2a043833d753\"><dc:Bounds x=\"120\" y=\"35\""
          + " width=\"100\" height=\"80\" /></bpmndi:BPMNShape><bpmndi:BPMNShape"
          + " id=\"id-91a31efc-38eb-4075-ba95-8eecebe90a48_shape\""
          + " bpmnElement=\"id-91a31efc-38eb-4075-ba95-8eecebe90a48\"><dc:Bounds x=\"250\" y=\"30\""
          + " width=\"100\" height=\"80\" /></bpmndi:BPMNShape><bpmndi:BPMNShape"
          + " id=\"id-f8d9d1e1-c93c-4092-bebb-038274d493b7_shape\""
          + " bpmnElement=\"id-f8d9d1e1-c93c-4092-bebb-038274d493b7\"><dc:Bounds x=\"481\" y=\"20\""
          + " width=\"100\" height=\"80\" /></bpmndi:BPMNShape><bpmndi:BPMNShape"
          + " id=\"id-394d3246-4552-4999-b0f4-2c11960a843e_shape\""
          + " bpmnElement=\"id-394d3246-4552-4999-b0f4-2c11960a843e\"><dc:Bounds x=\"351\""
          + " y=\"310\" width=\"100\" height=\"80\" /></bpmndi:BPMNShape><bpmndi:BPMNShape"
          + " id=\"id-eb413d52-5649-47d4-9c0a-1fe4b0fdbf98_shape\""
          + " bpmnElement=\"id-eb413d52-5649-47d4-9c0a-1fe4b0fdbf98\"><dc:Bounds x=\"495\""
          + " y=\"325\" width=\"50\" height=\"50\" /><bpmndi:BPMNLabel><dc:Bounds x=\"470\""
          + " y=\"290\" width=\"100\" height=\"40\""
          + " /></bpmndi:BPMNLabel></bpmndi:BPMNShape><bpmndi:BPMNShape"
          + " id=\"id-6bd3013b-8646-4137-ade7-499b31893131_shape\""
          + " bpmnElement=\"id-6bd3013b-8646-4137-ade7-499b31893131\"><dc:Bounds x=\"385\" y=\"50\""
          + " width=\"50\" height=\"50\" /><bpmndi:BPMNLabel><dc:Bounds x=\"360\" y=\"10\""
          + " width=\"100\" height=\"40\" /></bpmndi:BPMNLabel></bpmndi:BPMNShape><bpmndi:BPMNShape"
          + " id=\"id-de9b2fec-0186-4892-aefa-b6a905e4db1e_shape\""
          + " bpmnElement=\"id-de9b2fec-0186-4892-aefa-b6a905e4db1e\"><dc:Bounds x=\"360\""
          + " y=\"140\" width=\"100\" height=\"80\" /></bpmndi:BPMNShape><bpmndi:BPMNShape"
          + " id=\"id-add7903c-fc54-40ce-9fa5-d7f5894d584c_shape\""
          + " bpmnElement=\"id-add7903c-fc54-40ce-9fa5-d7f5894d584c\"><dc:Bounds x=\"72\" y=\"137\""
          + " width=\"36\" height=\"36\" /><bpmndi:BPMNLabel><dc:Bounds x=\"40\" y=\"171\""
          + " width=\"100\" height=\"40\" /></bpmndi:BPMNLabel></bpmndi:BPMNShape><bpmndi:BPMNShape"
          + " id=\"id-e5d2c95a-7d1d-450a-bc94-7fadc95d356c_shape\""
          + " bpmnElement=\"id-e5d2c95a-7d1d-450a-bc94-7fadc95d356c\"><dc:Bounds x=\"630\""
          + " y=\"310\" width=\"100\" height=\"80\" /></bpmndi:BPMNShape><bpmndi:BPMNShape"
          + " id=\"id-b069b6b0-8929-4de3-bb09-b5719c98c5a7_shape\""
          + " bpmnElement=\"id-b069b6b0-8929-4de3-bb09-b5719c98c5a7\"><dc:Bounds x=\"630\""
          + " y=\"429\" width=\"100\" height=\"80\" /></bpmndi:BPMNShape><bpmndi:BPMNEdge"
          + " id=\"id-4d638cc4-851d-4b91-b5af-9e6957efa908_shape\""
          + " bpmnElement=\"id-4d638cc4-851d-4b91-b5af-9e6957efa908\"><di:waypoint x=\"531\""
          + " y=\"100\" /><di:waypoint x=\"531\" y=\"290\" /><di:waypoint x=\"470\" y=\"290\""
          + " /><di:waypoint x=\"470\" y=\"330\" /><di:waypoint x=\"451\" y=\"330\""
          + " /><bpmndi:BPMNLabel><dc:Bounds x=\"481\" y=\"167\" width=\"100\" height=\"40\""
          + " /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge"
          + " id=\"id-8a6a0d49-851e-465d-9659-f592a8a5c9be_shape\""
          + " bpmnElement=\"id-8a6a0d49-851e-465d-9659-f592a8a5c9be\"><di:waypoint x=\"570\""
          + " y=\"469\" /><di:waypoint x=\"630\" y=\"469\" /><bpmndi:BPMNLabel><dc:Bounds x=\"550\""
          + " y=\"457\" width=\"100\" height=\"40\""
          + " /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge"
          + " id=\"id-79bb7477-c416-4163-9f93-7888a2dba8eb_shape\""
          + " bpmnElement=\"id-79bb7477-c416-4163-9f93-7888a2dba8eb\"><di:waypoint x=\"730\""
          + " y=\"350\" /><di:waypoint x=\"780\" y=\"350\" /><bpmndi:BPMNLabel><dc:Bounds x=\"705\""
          + " y=\"338\" width=\"100\" height=\"40\""
          + " /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge"
          + " id=\"id-ad7f7a53-cfc9-433e-834b-265393ef2cc3_shape\""
          + " bpmnElement=\"id-ad7f7a53-cfc9-433e-834b-265393ef2cc3\"><di:waypoint x=\"730\""
          + " y=\"469\" /><di:waypoint x=\"780\" y=\"469\" /><bpmndi:BPMNLabel><dc:Bounds x=\"705\""
          + " y=\"457\" width=\"100\" height=\"40\""
          + " /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge"
          + " id=\"id-c25381f4-76c6-4098-885a-8fc28c7c6d05_shape\""
          + " bpmnElement=\"id-c25381f4-76c6-4098-885a-8fc28c7c6d05\"><di:waypoint x=\"830\""
          + " y=\"85\" /><di:waypoint x=\"830\" y=\"40\" /><di:waypoint x=\"872\" y=\"40\""
          + " /><bpmndi:BPMNLabel><dc:Bounds x=\"780\" y=\"50.5\" width=\"100\" height=\"40\""
          + " /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge"
          + " id=\"id-9ab5afb1-3d22-4a19-8630-b1231247348d_shape\""
          + " bpmnElement=\"id-9ab5afb1-3d22-4a19-8630-b1231247348d\"><di:waypoint x=\"830\""
          + " y=\"310\" /><di:waypoint x=\"830\" y=\"165\" /><bpmndi:BPMNLabel><dc:Bounds x=\"780\""
          + " y=\"225.5\" width=\"100\" height=\"40\""
          + " /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge"
          + " id=\"id-88ed032f-2ad4-4688-987e-77b41bb11ede_shape\""
          + " bpmnElement=\"id-88ed032f-2ad4-4688-987e-77b41bb11ede\"><di:waypoint x=\"410\""
          + " y=\"100\" /><di:waypoint x=\"410\" y=\"140\" /><bpmndi:BPMNLabel><dc:Bounds x=\"330\""
          + " y=\"90\" width=\"100\" height=\"40\""
          + " /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge"
          + " id=\"id-1258cd0e-8bc5-4c52-85b8-567500708524_shape\""
          + " bpmnElement=\"id-1258cd0e-8bc5-4c52-85b8-567500708524\"><di:waypoint x=\"108\""
          + " y=\"155\" /><di:waypoint x=\"170\" y=\"155\" /><di:waypoint x=\"170\" y=\"115\""
          + " /><bpmndi:BPMNLabel><dc:Bounds x=\"89\" y=\"143\" width=\"100\" height=\"40\""
          + " /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge"
          + " id=\"id-7297e7a8-c9b6-4154-93e0-02fec7ce9b85_shape\""
          + " bpmnElement=\"id-7297e7a8-c9b6-4154-93e0-02fec7ce9b85\"><di:waypoint x=\"545\""
          + " y=\"350\" /><di:waypoint x=\"630\" y=\"350\" /><bpmndi:BPMNLabel><dc:Bounds x=\"540\""
          + " y=\"350\" width=\"100\" height=\"40\""
          + " /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge"
          + " id=\"id-205183a7-69dd-425b-af32-cbfcd0b7c740_shape\""
          + " bpmnElement=\"id-205183a7-69dd-425b-af32-cbfcd0b7c740\"><di:waypoint x=\"435\""
          + " y=\"75\" /><di:waypoint x=\"481\" y=\"75\" /><bpmndi:BPMNLabel><dc:Bounds x=\"410\""
          + " y=\"70\" width=\"100\" height=\"40\""
          + " /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge"
          + " id=\"id-de245238-6e8a-4d57-b623-72f986474d09_shape\""
          + " bpmnElement=\"id-de245238-6e8a-4d57-b623-72f986474d09\"><di:waypoint x=\"520\""
          + " y=\"375\" /><di:waypoint x=\"520\" y=\"429\" /><bpmndi:BPMNLabel><dc:Bounds x=\"500\""
          + " y=\"370\" width=\"100\" height=\"40\""
          + " /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge"
          + " id=\"id-25164739-c8de-4780-8bca-9a02c2340979_shape\""
          + " bpmnElement=\"id-25164739-c8de-4780-8bca-9a02c2340979\"><di:waypoint x=\"220\""
          + " y=\"75\" /><di:waypoint x=\"250\" y=\"75\" /><bpmndi:BPMNLabel><dc:Bounds x=\"185\""
          + " y=\"63\" width=\"100\" height=\"40\""
          + " /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge"
          + " id=\"id-9b9aa774-0225-4a45-b8a5-0a4060c75cc2_shape\""
          + " bpmnElement=\"id-9b9aa774-0225-4a45-b8a5-0a4060c75cc2\"><di:waypoint x=\"830\""
          + " y=\"429\" /><di:waypoint x=\"830\" y=\"390\" /><bpmndi:BPMNLabel><dc:Bounds x=\"780\""
          + " y=\"397.5\" width=\"100\" height=\"40\""
          + " /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge"
          + " id=\"id-661afac8-579e-4dc0-9b7a-5f5dd50b96eb_shape\""
          + " bpmnElement=\"id-661afac8-579e-4dc0-9b7a-5f5dd50b96eb\"><di:waypoint x=\"451\""
          + " y=\"350\" /><di:waypoint x=\"495\" y=\"350\" /><bpmndi:BPMNLabel><dc:Bounds x=\"423\""
          + " y=\"338\" width=\"100\" height=\"40\""
          + " /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge"
          + " id=\"id-cfeb8788-b62b-4cf5-8592-1fce0893264f_shape\""
          + " bpmnElement=\"id-cfeb8788-b62b-4cf5-8592-1fce0893264f\"><di:waypoint x=\"410\""
          + " y=\"220\" /><di:waypoint x=\"410\" y=\"310\" /><bpmndi:BPMNLabel><dc:Bounds x=\"360\""
          + " y=\"237\" width=\"100\" height=\"40\""
          + " /></bpmndi:BPMNLabel></bpmndi:BPMNEdge><bpmndi:BPMNEdge"
          + " id=\"id-c9a4b533-5a8b-4a30-b9c4-bf300bd9f6e4_shape\""
          + " bpmnElement=\"id-c9a4b533-5a8b-4a30-b9c4-bf300bd9f6e4\"><di:waypoint x=\"350\""
          + " y=\"70\" /><di:waypoint x=\"385\" y=\"70\" /><bpmndi:BPMNLabel><dc:Bounds x=\"317.5\""
          + " y=\"58\" width=\"100\" height=\"40\""
          + " /></bpmndi:BPMNLabel></bpmndi:BPMNEdge></bpmndi:BPMNPlane></bpmndi:BPMNDiagram></bpmn2:definitions>";
    OpenAiApiDTO dto =
        new OpenAiApiDTO(
            "sk-testapikey", "gpt-3.5-turbo", "Summarize the following text:", "openAi", false);

    stubFor(
        post(urlPathEqualTo("/v1/chat/completions"))
            .withHeader("Authorization", equalTo("Bearer " + dto.getApiKey()))
            .withHeader("Content-Type", equalTo("application/json"))
            // WireMock kann den Request Body auf bestimmte Inhalte prüfen
            .withRequestBody(containing(dto.getPrompt()))
            .withRequestBody(containing(body))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"choices\":[{\"message\":{\"content\":\"This is a mocked OpenAI"
                            + " response.\"}}]}"))); // Direkte JSON-Antwort


    System.setProperty("openai.api.url", wiremockBaseUrl + "/v1/models");

    
    String actualResponse = p2tllmService.callLLM(body, dto);

    assertNotNull(actualResponse);
    assertEquals("This is a mocked OpenAI response.", actualResponse);
  }
}
