package com.ecg.comaas.gtuk.filter.integration;

import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.gumtree.api.category.CategoryModel;
import com.gumtree.api.category.stub.StubCategoryModel;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static com.ecg.comaas.gtuk.filter.integration.Utils.readFileContent;
import static com.ecg.replyts.core.api.model.Tenants.TENANT_GTUK;
import static com.ecg.replyts.integration.test.support.IntegrationTestUtils.propertiesWithTenant;

@Configuration
public class FilterConfigurationIntegrationTest {

    @Bean
    public CategoryModel unfilteredCategoryModel() {
        return new StubCategoryModel();
    }

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule(propertiesWithTenant(TENANT_GTUK), FilterConfigurationIntegrationTest.class);

    @Test
    public void loadsAllConfiguration() throws IOException, URISyntaxException, JSONException {
        URL url = getClass().getResource("/gtukJsonContract");
        assertConfigLoadingReturns200(Files.newDirectoryStream(Paths.get(url.toURI())));
    }

    @Test
    public void loadsVolumeConfigTwice() throws IOException, URISyntaxException, JSONException {
        URL url = getClass().getResource("/gtukJsonContract/velocity.json");
        assertConfigLoadingReturns200(Arrays.asList(Paths.get(url.toURI()), Paths.get(url.toURI())));
    }

    private void assertConfigLoadingReturns200(Iterable<Path> paths) throws JSONException {
        for (Path path : paths) {
            try {
                String body = "[" + readFileContent(path) + "]";
                Response response = RestAssured.given().when().request().port(rule.getHttpPort()).body(body)
                        .contentType(ContentType.JSON).put("/configv2/").then().statusCode(200).extract().response();
                new JSONObject(response.asString());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}