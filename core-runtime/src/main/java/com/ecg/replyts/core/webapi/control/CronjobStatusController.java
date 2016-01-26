package com.ecg.replyts.core.webapi.control;

import com.ecg.replyts.core.api.cron.CronExecution;
import com.ecg.replyts.core.api.cron.ExecutionStatusMonitor;
import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
class CronjobStatusController {

    private final ExecutionStatusMonitor monitor;

    @Autowired
    CronjobStatusController(ExecutionStatusMonitor monitor) {
        this.monitor = monitor;
    }

    @RequestMapping(value = "statusCrons", produces = "application/json")
    @ResponseBody
    public Object cronjobStatus() {
        ArrayNode data = JsonObjects.newJsonArray();

        for (CronExecution execution : monitor.currentlyExecuted()) {
            data.add(JsonObjects.builder()
                    .attr("job", execution.getJobName())
                    .attr("started", execution.getStartDate().toString("yyyy-MM-dd'T'hh:mm:ss"))
                    .attr("on-host", execution.getExecutedOnHost())
                    .build());
        }


        return JsonObjects.builder().attr("title", "Current Cron Job Executions").attr("running", data).toJson();
    }
}
