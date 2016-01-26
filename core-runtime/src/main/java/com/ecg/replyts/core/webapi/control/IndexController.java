package com.ecg.replyts.core.webapi.control;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.InputStreamReader;

@Controller
class IndexController {

    @ResponseBody
    @RequestMapping(value = "/")
    public String index(HttpServletResponse resp) throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/webapi_index.html")) {
            return CharStreams.toString(new InputStreamReader(input, Charsets.UTF_8));
        }
    }
}
