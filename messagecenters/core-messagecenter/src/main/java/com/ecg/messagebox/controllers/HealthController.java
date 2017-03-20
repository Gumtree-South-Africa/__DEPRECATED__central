package com.ecg.messagebox.controllers;

import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@Controller
@RequestMapping(value = "/health")
public class HealthController {
    @RequestMapping(method = GET)
    @ResponseBody
    public ResponseObject<Health> getResponseData() {
        return ResponseObject.of(new Health());
    }

    class Health {
        private Health() {
        }

        public String getModel() {
            return "messagebox";
        }

        public String getStatus() {
            return "OK";
        }
    }
}