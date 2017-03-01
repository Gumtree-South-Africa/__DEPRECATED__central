package com.ecg.comaas.r2cmigration.difftool.util;

import com.google.common.base.Throwables;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

import java.util.Arrays;

public class CommaSeparatedListOptionHandler extends OptionHandler<String> {
    public CommaSeparatedListOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super String> setter) {
        super(parser, option, setter);
    }

    public int parseArguments(Parameters in_parameters) throws CmdLineException {
        String option_value = in_parameters.getParameter(0);

        if (option_value == null) {
            setter.addValue(null);
            return 1;
        }

        Arrays.stream(option_value.split(",")).forEach(value -> {
                    try {
                        setter.addValue(value);
                    } catch (CmdLineException e) {
                        throw Throwables.propagate(e);
                    }
                }
        );

        return 1;
    }

    public String getDefaultMetaVariable() {
        return "VAL";
    }
}
