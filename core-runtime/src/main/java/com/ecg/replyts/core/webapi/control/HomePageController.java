package com.ecg.replyts.core.webapi.control;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.InputStream;
import java.io.InputStreamReader;

@Controller
class HomePageController {

    @ResponseBody
    @RequestMapping(value = "/")
    public String index() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/webapi_home.html")) {
            return CharStreams.toString(new InputStreamReader(input, Charsets.UTF_8));
        }
    }

    @RequestMapping("favicon.ico")
    @ResponseBody
    public void favicon() {}
}
