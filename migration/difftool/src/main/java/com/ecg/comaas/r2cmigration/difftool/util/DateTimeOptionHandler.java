package com.ecg.comaas.r2cmigration.difftool.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;

import static com.ecg.comaas.r2cmigration.difftool.DiffToolConfiguration.DATETIME_STRING;


public class DateTimeOptionHandler extends OneArgumentOptionHandler<DateTime> {

    public DateTimeOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super DateTime> setter) {
        super(parser, option, setter);
    }

    // expected input "04-02-2011T20:27"
    @Override
    public DateTime parse(String arg) {
        DateTimeFormatter formatter = DateTimeFormat.forPattern(DATETIME_STRING);
        return formatter.parseLocalDateTime(arg).toDateTime(DateTimeZone.UTC);
    }
}
