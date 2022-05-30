import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import net.minidev.json.JSONObject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;

class MockTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().extensions(new ResponseTemplateTransformer(true)))
            .build();

    @Test
    void returnStringForGetRequestTest() {
        stubFor(get(urlPathEqualTo("/person/first"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, "application/json")
                        .withBody("{\n" +
                                "  \"id\": 1,\n" +
                                "  \"name\": \"Alex Green\"\n" +
                                "}"))
        );

        Person response = given()
                .get("/person/first")
                .then()
                .statusCode(200)
                .extract()
                .as(Person.class);

        Assertions.assertThat(response).isEqualTo(new Person(1, "Alex Green"));
    }

    @Test
    void returnJsonFromFileForRequestMatchingRegexTest() {
        stubFor(get(urlPathMatching("/person/[0-9]"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("simplePerson.json"))
        );

        Person response = given()
                .get("/person/2")
                .then()
                .statusCode(200)
                .extract()
                .as(Person.class);

        Assertions.assertThat(response).isEqualTo(new Person(2, "Charles Parker"));
    }

    @Test
    void returnResponseToRequestWithQueryParamTest() {
        String name = "Jane";

        stubFor(get(urlPathEqualTo("/person/byName"))
                .withQueryParam("name", equalTo(name))
                .willReturn(ok("Name is: " + name))
        );

        String response = given()
                .queryParam("name", name)
                .get("/person/byName")
                .then()
                .statusCode(200)
                .extract()
                .asString();

        Assertions.assertThat(response).isEqualTo("Name is: " + name);
    }

    @Test
    void useTemplateToReturnPartOfPathAndQueryParamTest() {
        String name = "Ronald";
        int id = 123;

        stubFor(get(urlPathMatching("/person/[0-9]{3}"))
                .withQueryParam("name", equalTo(name))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, "application/json")
                        .withBodyFile("personTemplate.json"))
        );

        Person response = given()
                .queryParam("name", name)
                .get(format("/person/%s", id))
                .then()
                .statusCode(200)
                .extract()
                .as(Person.class);

        Assertions.assertThat(response).isEqualTo(new Person(id, name));

    }

    @Test
    void useRequestBodyContainAndResponseBodyTemplateTest() {
        int id = 4;
        String name = "Emma";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", id);
        jsonObject.put("name", name);
        jsonObject.put("phone", "555-555-5555");
        jsonObject.put("address", "1, Main st.");
        String bodyJson = jsonObject.toString();

        stubFor(post(urlEqualTo("/people"))
                .withRequestBody(containing(name))
                .willReturn(WireMock.aResponse()
                        .withStatus(202)
                        .withHeader(CONTENT_TYPE, "application/json")
                        .withBodyFile("personTemplateWithBodyParsing.json"))
        );

        Person response = given()
                .contentType(JSON)
                .body(bodyJson)
                .post("/people")
                .then()
                .statusCode(202)
                .extract()
                .as(Person.class);

        Assertions.assertThat(response).isEqualTo(new Person(id, name));
    }

    @Test
    void generateRandomValuesInResponseBodyTest() {
        stubFor(get(urlEqualTo("/person/random"))
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, "application/json")
                        .withBodyFile("randomPerson.json"))
        );

        given()
                .get("/person/random")
                .then()
                .statusCode(200)
                .extract()
                .asString();
    }
}
