package com.ecg.de.mobile.replyts.rating.svc;

import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageState;
import com.google.common.collect.Lists;
import com.google.gson.GsonBuilder;
import de.mobile.dealer.rating.invite.EmailInviteEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit.ErrorHandler;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import retrofit.http.Body;
import retrofit.http.Headers;
import retrofit.http.PUT;

import java.util.List;
import java.util.Optional;

import static com.ecg.de.mobile.replyts.rating.svc.EmailInviteAssembler.assemble;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

public class DealerRatingService {

    private final static Logger logger = LoggerFactory.getLogger(DealerRatingService.class);

    private final DealerRatingServiceClient client;
    private final boolean isActive;
    private final List<String> allowedSources = Lists.newArrayList(
            "mob-mportal",
            "SITE-GERMANY",
            "mob-android",
            "mob-iPhone",
            "mob-ipad");

    public DealerRatingService(final String endPointUrl, final boolean isActive) {
        client = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.BASIC)
                .setEndpoint(endPointUrl)
                .setErrorHandler(new DealerRatingServiceErrorHandler())
                .setConverter(new GsonConverter(new GsonBuilder()
                        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                        .create()))
                .setLog(logger::info)
                .build()
                .create(DealerRatingServiceClient.class);

        this.isActive = isActive;
    }

    DealerRatingService(DealerRatingServiceClient client, boolean active) {
        this.client = client;
        this.isActive = active;
    }

    public void saveInvitation(final Message message, final String conversationId) {
        if (isActive) {
            final EmailInviteEntity invite = assemble(message, conversationId);
            if (isSourceAllowed(invite)) {
                if (message.getState() == MessageState.SENT) {
                    logger.info("Persisting via service, " + invite.getDealerId() + "; " + invite.getBuyerEmail());
                    client.createEmailInvite(invite);
                } else {
                    logger.info("Don't create emal trigger because message is in state {}", message.getState());
                }
            }
        } else {
            logger.debug("DealerRating plugin not active");
        }
    }

    private boolean isSourceAllowed(final EmailInviteEntity invite) {
        return null != invite && (allowedSources.contains(invite.getSource())
                || invite.getSource().startsWith("newCars"));
    }

    interface DealerRatingServiceClient {
        @Headers({
                "Content-type:application/json",
                "Accept:application/json"
        })
        @PUT("/emailInvite")
        Response createEmailInvite(@Body EmailInviteEntity invite);
    }

    private final class DealerRatingServiceErrorHandler implements ErrorHandler {
        @Override
        public Throwable handleError(final RetrofitError cause) {
            return Optional.ofNullable(cause.getResponse())
                    .filter(response -> response.getStatus() >= SC_BAD_REQUEST)
                    .map(response -> new RuntimeException("Error caught during dealer rating service request.", cause).getCause())
                    .orElseGet(() -> cause);
        }
    }
}
