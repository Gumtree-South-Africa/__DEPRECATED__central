package com.ecg.comaas.core.filter.belenblockeduser;

import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BlockedUserFilterFactory implements FilterFactory {

    static final String IDENTIFIER = "com.ecg.de.kleinanzeigen.replyts.belen.blockeduser.BlockedUserFilterFactory";

    private final JdbcTemplate jdbcTemplate;
    private final boolean extTnsEnabled;

    @Autowired
    public BlockedUserFilterFactory(
            @Qualifier("belenBlockedUserJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Value("${replyts2-belenblockeduserfilter-plugin.externalTnsEnabled:true}") boolean extTnsEnabled
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.extTnsEnabled = extTnsEnabled;
    }

    @Override
    public Filter createPlugin(String s, JsonNode jsonNode) {
        return new BlockedUserFilter(jdbcTemplate, extTnsEnabled);
    }

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }
}
