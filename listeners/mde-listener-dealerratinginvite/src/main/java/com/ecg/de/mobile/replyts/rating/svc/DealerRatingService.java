package com.ecg.de.mobile.replyts.rating.svc;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import de.mobile.dealer.rating.invite.EmailInviteEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

import java.util.List;

/**
 * Created by vbalaramiah on 4/23/15.
 */
public class DealerRatingService {

    private final DealerRatingServiceClient client;

    private final EmailInviteAssembler assembler;

    private final boolean isActive;

    private final List<String> allowedSources;

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create();

    private final static Logger logger = LoggerFactory.getLogger(DealerRatingService.class);

    public DealerRatingService(String endPointUrl, EmailInviteAssembler assembler, boolean isActive) {
        client = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.BASIC)
                .setEndpoint(endPointUrl)
                .setErrorHandler(new DealerRatingServiceErrorHandler())
                .setConverter(new GsonConverter(GSON))
                .setLog(new RestAdapter.Log() {
                    @Override
                    public void log(String s) {
                        logger.info(s);
                    }
                })
                .build()
                .create(DealerRatingServiceClient.class);
        this.isActive = isActive;
        this.assembler = assembler;
        this.allowedSources = Lists.newArrayList(
                "mob-mportal",
                "SITE-GERMANY",
                "mob-android",
                "mob-iPhone",
                "mob-ipad");
    }

    public void saveInvitation(Message message, String conversationId) {
        if (isActive) {
            final EmailInviteEntity invite = assembler.toEmailInvite(message, conversationId);
            if (isSourceAllowed(invite)) {
                logger.info("Persisting via service, " + invite.getDealerId() + "; " + invite.getBuyerEmail());
                client.createEmailInvite(invite);
            }
        } else {
            logger.debug("DealerRating plugin not active");
        }
    }

    private boolean isSourceAllowed(EmailInviteEntity invite) {
        if (null == invite) {
            return false;
        } else return (allowedSources.contains(invite.getSource())
                || invite.getSource().startsWith("newCars"));
    }
}
