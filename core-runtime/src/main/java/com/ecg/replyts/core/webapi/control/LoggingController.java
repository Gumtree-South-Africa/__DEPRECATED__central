package com.ecg.replyts.core.webapi.control;

import com.ecg.replyts.core.runtime.LoggingPropagationService;
import com.ecg.replyts.core.runtime.LoggingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.ws.rs.NotFoundException;
import java.util.Map;

@RestController
@RequestMapping("/logging")
public class LoggingController {
    @Autowired
    private LoggingService loggingService;

    @Autowired
    private LoggingPropagationService loggingPropagationService;

    @RequestMapping(method = RequestMethod.GET)
    public Map<String, String> get() {
        return loggingService.getLevels();
    }

    @RequestMapping(value = "/{logPackage}", method = RequestMethod.GET)
    public String get(@PathVariable String logPackage) {
        String result = loggingService.getLevels().get(logPackage);

        if (result != null) {
            return result;
        } else {
            throw new NotFoundException("No explicit override given for this log package");
        }
    }

    @RequestMapping(method = RequestMethod.PUT)
    public void put(@RequestBody Map<String, String> levels, @RequestParam(defaultValue = "false") boolean propagate) {
        loggingService.replaceAll(levels);

        if (propagate) {
            loggingPropagationService.propagateReplaceAll(levels);
        }
    }

    @RequestMapping(value = "/{logPackage}", method = RequestMethod.PUT)
    public void put(@PathVariable String logPackage, @RequestBody String level, @RequestParam(defaultValue = "false") boolean propagate) {
        loggingService.upsertAndSet(logPackage, level);

        if (propagate) {
            loggingPropagationService.propagateUpsertAndSet(logPackage, level);
        }
    }

    @RequestMapping(method = RequestMethod.DELETE)
    public void reset(@RequestParam(defaultValue = "false") boolean propagate) {
        loggingService.initializeToProperties();

        if (propagate) {
            loggingPropagationService.propagateInitializeToProperties();
        }
    }
}