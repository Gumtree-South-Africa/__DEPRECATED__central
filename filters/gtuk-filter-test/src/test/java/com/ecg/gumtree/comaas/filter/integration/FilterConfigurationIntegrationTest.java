package com.ecg.gumtree.comaas.filter.integration;

import com.ecg.replyts.integration.test.ReplyTsIntegrationTestRule;
import com.gumtree.api.category.CategoryModel;
import com.gumtree.api.category.stub.StubCategoryModel;
import io.netty.buffer.ByteBuf;
import io.netty.handler.logging.LogLevel;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.vavr.Tuple2;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import rx.Observable;

import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;

import static com.ecg.gumtree.comaas.filter.integration.Utils.readFileContent;
import static org.assertj.core.api.Assertions.assertThat;

@Configuration
public class FilterConfigurationIntegrationTest {

    @Bean
    public CategoryModel unfilteredCategoryModel() {
        return new StubCategoryModel();
    }

    @Rule
    public ReplyTsIntegrationTestRule rule = new ReplyTsIntegrationTestRule(
            new Properties() {{
                put("replyts.tenant", "gtuk");
            }},
            null, 20, false,
            new Class[]{FilterConfigurationIntegrationTest.class},
            "cassandra_schema.cql");

    @Test
    public void loadsAllConfiguration() throws Exception {
        URL url = getClass().getResource("/gtukJsonContract");
        assertConfigLoadingReturns200(Files.newDirectoryStream(Paths.get(url.toURI())));
    }

    @Test
    public void loadsVolumeConfigTwice() throws Exception {
        URL url = getClass().getResource("/gtukJsonContract/velocity.json");
        assertConfigLoadingReturns200(Arrays.asList(Paths.get(url.toURI()), Paths.get(url.toURI())));
    }

    private void assertConfigLoadingReturns200(Iterable<Path> paths) {
        Observable.from(paths)
                .map(name -> new Tuple2<>(name.toString(), "[" + readFileContent(name) + "]"))
                .flatMap(t -> putRequest()
                                .writeStringContent(Observable.just(t._2))
                                .doOnNext(response -> assertThat(response.getStatus().code())
                                        .describedAs("Should return 200 OK for file %s", t._1)
                                        .isEqualTo(200))
                                .flatMap(response -> response.getContent()
                                        .map(byteBuf -> byteBuf.toString(Charset.defaultCharset())))
                                .map(this::toJson),
                        1)
                .toBlocking().last();
    }

    private HttpClientRequest<ByteBuf, ByteBuf> putRequest() {
        return HttpClient.newClient(InetSocketAddress.createUnresolved("localhost", rule.getHttpPort()))
                .enableWireLogging("inter-client", LogLevel.DEBUG)
                .createPut("/configv2/")
                .addHeader("Content-Type", "application/json");
    }

    private JSONObject toJson(String body) {
        try {
            return new JSONObject(body);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
