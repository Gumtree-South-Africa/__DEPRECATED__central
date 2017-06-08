package com.ecg.messagecenter.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolver;

import java.util.Locale;
import java.util.Map;

/**
 * Created by jaludden on 21/12/15.
 */
@Component
public class Template {

    private String siteUrl;
    private String cdnUrl;

    @Autowired public Template(@Value("${site.baseurl:https://kijiji.it}") String siteUrl,
                    @Value("${site.cdnurl:https://static.annuncicdn.it/it/images/}")
                    String cdnUrl) {
        this.siteUrl = siteUrl;
        this.cdnUrl = cdnUrl;
    }

    private TemplateEngine getTemplateEngine() {
        TemplateEngine result = new TemplateEngine();
        TemplateResolver emailTemplateResolver = new ClassLoaderTemplateResolver();
        emailTemplateResolver.setPrefix("templates/");
        emailTemplateResolver.setTemplateMode("HTML5");
        emailTemplateResolver.setOrder(1);
        result.addTemplateResolver(emailTemplateResolver);
        result.addDialect(new KijijiDialect(siteUrl, cdnUrl));
        return result;
    }

    public String createPostReplyMessage(Map<String, Object> variables) {
        final Context ctx = new Context(Locale.ITALIAN);
        ctx.setVariables(variables);
        return getTemplateEngine().process("postReplyFirstMessage.html", ctx);

    }
}
