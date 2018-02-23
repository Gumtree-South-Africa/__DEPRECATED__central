package com.ecg.messagebox.resources;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "Internal")
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class HealthResource {

    @ApiOperation(
            value = "Health check",
            notes = "Returns status code 200 if the MessageBox is UP and RUNNING",
            nickname = "health",
            tags = "Internal")
    @ApiResponses(@ApiResponse(code = 200, message = "Success"))
    @GetMapping("health")
    public Health health() {
        return new Health();
    }

    static class Health {
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