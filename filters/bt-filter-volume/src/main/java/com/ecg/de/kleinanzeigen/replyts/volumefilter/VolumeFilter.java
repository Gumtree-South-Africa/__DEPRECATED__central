package com.ecg.de.kleinanzeigen.replyts.volumefilter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.ecg.replyts.core.api.model.conversation.ConversationRole;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.api.processing.ProcessingTimeExceededException;
import com.google.common.collect.ImmutableList;
import com.hazelcast.core.HazelcastInstance;


/**
 * Uses Esper to query for the number of mails received by the mail's sender within a quota window.
 * If a quota is violated, a score is assigned.
 * If the quota is configured with "score memory" and a message arrives while we still
 * remember about the previous violation, the score is immediately assigned.
 * Expects to be given a list of quotas sorted by score in a descending order.
 * If multiple quotas are violated, only the one with the highest score is returned.
 * The filter doesn't execute if configured to ignore follow-ups and the message doesn't contain
 * the X-ADID header (indicates initial reply sent from platform).
 */
class VolumeFilter implements Filter{

    private static final Logger LOG = LoggerFactory.getLogger(VolumeFilter.class);
    private static final String PROVIDER_NAME_PREFIX = "volumefilter_provider_";
    private static final int HAZELCAST_OP_RETRIES = 1;

    private final EventStreamProcessor processor;
    private final List<Quota> sortedQuotas;
    private final SharedBrain brain;
    private final boolean ignoreFollowUps;
    
    private final List<Integer> exceptCategoriesList;
    private final List<Integer> allowedCategoriesList;

    VolumeFilter(String filterName, 
    		     HazelcastInstance hazelcastInstance, 
    		     List<Quota> sortedQuotas,
    		     boolean ignoreFollowUps, 
    		     List<Integer> exceptCategoriesList,
    		     List<Integer> allowedCategoriesList) {
    	
        // Random integer added to processor name, so that it's never reused, and no conflicts arise when
        // a new instance is created due to a configuration update.
        this.processor = new EventStreamProcessor(PROVIDER_NAME_PREFIX + filterName + "_" + new Random().nextInt(), sortedQuotas);
        this.sortedQuotas = sortedQuotas;
        this.ignoreFollowUps = ignoreFollowUps;
        this.brain = new SharedBrain(filterName, hazelcastInstance, processor);
        this.exceptCategoriesList = exceptCategoriesList;
        this.allowedCategoriesList = allowedCategoriesList;

        LOG.info("Set up volume filter [{}] with ignoreFollowUps [{}], quotas [{}], exceptCategoriesList [{}] and allowedCategoriesList [{}]", filterName, ignoreFollowUps, sortedQuotas, exceptCategoriesList, allowedCategoriesList);

	}

	private List<FilterFeedback> getRememberedScoreFeedbacks(String senderMailAddress) {
        QuotaViolationRecord violationRecord = null;
      
/*        RetryCommand<QuotaViolationRecord> getViolationFromMemoryCmd = new RetryCommand<>(HAZELCAST_OP_RETRIES);
        try {
            violationRecord = getViolationFromMemoryCmd.run(() -> {
                try {
                    return brain.getViolationRecordFromMemory(senderMailAddress);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            LOG.warn("Couldn't get score from violation memory. Assuming none.", e);
        }
*/

        try {
			violationRecord = brain.getViolationRecordFromMemory(senderMailAddress);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			LOG.warn("Couldn't get score from violation memory. Assuming none.", e);
		}

        if (violationRecord != null) {
            return Collections.singletonList(new FilterFeedback(
                    violationRecord.getDescription(),
                    "sender previously exceeded quota",
                    violationRecord.getScore(),
                    FilterResultState.OK));
        }
        return null;
    }

    private void rememberQuotaViolation(String senderMailAddress, Quota q, String violationDescription) {

/*    	RetryCommand<Integer> rememberViolationCmd = new RetryCommand<>(HAZELCAST_OP_RETRIES);
        try {
            rememberViolationCmd.run(() -> {
                try {
                    brain.rememberViolation(
                            senderMailAddress,
                            q.getScore(),
                            violationDescription + " (triggered at " + LocalDateTime.now() + ")",
                            (int) q.getScoreMemoryDurationUnit().toSeconds(q.getScoreMemoryDurationValue())
                    );
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    throw new RuntimeException(e);
                }

                return 1;
            });
        } catch (Exception e) {
            LOG.warn("Couldn't remember quota violation", e);
        }
*/    

        try {
			brain.rememberViolation(
			        senderMailAddress,
			        q.getScore(),
			        //violationDescription + " (triggered at " + LocalDateTime.now() + ")",
			        violationDescription + " (triggered at " +new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS").format(Calendar.getInstance().getTime())+ ")",
			        (int) q.getScoreMemoryDurationUnit().toSeconds(q.getScoreMemoryDurationValue())
			);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			LOG.warn("Couldn't remember quota violation", e);
		}
    }


	@Override
	public List<FilterFeedback> filter(MessageProcessingContext messageProcessingContext) throws ProcessingTimeExceededException {
        Message message = messageProcessingContext.getMessage();
        ConversationRole fromRole = message.getMessageDirection().getFromRole();
        String senderMailAddress = messageProcessingContext.getConversation().getUserId(fromRole);

		String conversation_id = messageProcessingContext.getMail().getCustomHeaders().get("conversation_id") != null
				? messageProcessingContext.getMail().getCustomHeaders().get("conversation_id") : null;
		if (conversation_id == null) 
			conversation_id = messageProcessingContext.getConversation().getCustomValues()
					.get("conversation_id") != null
							? messageProcessingContext.getConversation().getCustomValues().get("conversation_id")
							: null;
		LOG.debug("Conversation id is ::: "+conversation_id);
			
        if (ignoreFollowUps && conversation_id !=null) {
            LOG.debug("Ignoring follow-up from [{}]. Msg id: [{}]", senderMailAddress, message.getId());
            return Collections.emptyList();
        }

        Set<String> categorySet = getInMsgCatTree(messageProcessingContext);
        
        //Check for except categories list
        if(isExceptCategory(categorySet)){
			LOG.debug("Ignoring the message from  [{}] as the category belongs to the configured except categories list",senderMailAddress);
        	return Collections.emptyList();
        }

        
        //Either apply for all the categories or the list of configured categories
        if(isAllowedCategory(categorySet)){
	        
	        brain.markSeen(senderMailAddress);
	        List<FilterFeedback> feedbacksFromRememberedScore = getRememberedScoreFeedbacks(senderMailAddress);
	        if (feedbacksFromRememberedScore != null) {
	            return feedbacksFromRememberedScore;
	        }
	        for (Quota q : sortedQuotas) {
	            long mailsInTimeWindow = processor.count(senderMailAddress, q);
	
	            LOG.debug("No. of mails in {} {}: {}", q.getPerTimeValue(), q.getPerTimeUnit(), mailsInTimeWindow);
	
	            if (mailsInTimeWindow > q.getAllowance()) {
	                String violationDescription = q.describeViolation(mailsInTimeWindow);
	                rememberQuotaViolation(senderMailAddress, q, violationDescription);
	                return Collections.singletonList(new FilterFeedback(
	                        q.uihint(),
	                        violationDescription,
	                        q.getScore(),
	                        FilterResultState.OK));
	            }
	        }
        }

        return Collections.emptyList();
	}
	
	private Set<String> getInMsgCatTree(MessageProcessingContext messageProcessingContext) {
		
		String category_id = messageProcessingContext.getMail().getCustomHeaders().get("categoryid") != null
				? messageProcessingContext.getMail().getCustomHeaders().get("categoryid") : null;
		if (category_id == null) 
			category_id = messageProcessingContext.getConversation().getCustomValues().get("categoryid") != null
							? messageProcessingContext.getConversation().getCustomValues().get("categoryid")
							: null;

		String l1_category_id = messageProcessingContext.getMail().getCustomHeaders().get("l1-categoryid") != null
				? messageProcessingContext.getMail().getCustomHeaders().get("l1-categoryid") : null;
		if (l1_category_id == null) 
			l1_category_id = messageProcessingContext.getConversation().getCustomValues().get("l1-categoryid") != null
							? messageProcessingContext.getConversation().getCustomValues().get("l1-categoryid")
							: null;
					
		String l2_category_id = messageProcessingContext.getMail().getCustomHeaders().get("l2-categoryid") != null
				? messageProcessingContext.getMail().getCustomHeaders().get("l2-categoryid") : null;
		if (l2_category_id == null) 
			l2_category_id = messageProcessingContext.getConversation().getCustomValues().get("l2-categoryid") != null
					? messageProcessingContext.getConversation().getCustomValues().get("categoryid")
					: null;

		String l3_category_id = messageProcessingContext.getMail().getCustomHeaders().get("l3-categoryid") != null
				? messageProcessingContext.getMail().getCustomHeaders().get("l3-categoryid") : null;
		if (l3_category_id == null) 
			l3_category_id = messageProcessingContext.getConversation().getCustomValues().get("l3-categoryid") != null
					? messageProcessingContext.getConversation().getCustomValues().get("categoryid")
					: null;

		String l4_category_id = messageProcessingContext.getMail().getCustomHeaders().get("l4-categoryid") != null
				? messageProcessingContext.getMail().getCustomHeaders().get("l4-categoryid") : null;
		if (l4_category_id == null) 
			l4_category_id = messageProcessingContext.getConversation().getCustomValues().get("l4-categoryid") != null
			? messageProcessingContext.getConversation().getCustomValues().get("l4-categoryid")
			: null;

		Set<String> categorySet = new HashSet<String>();	
		
		if(StringUtils.hasText(category_id)){
			LOG.debug("Category id extracted from incoming msg is  :::: "+category_id);
			categorySet.add(category_id);
		}

		if(StringUtils.hasText(l1_category_id)){
			LOG.debug("L1 Category id extracted from incoming msg is  :::: "+l1_category_id);
			categorySet.add(l1_category_id);
		}

		if(StringUtils.hasText(l2_category_id)){
			LOG.debug("L2 Category id extracted from incoming msg is  :::: "+l2_category_id);
			categorySet.add(l2_category_id);
		}

		if(StringUtils.hasText(l3_category_id)){
			LOG.debug("L3 Category id extracted from incoming msg is  :::: "+l3_category_id);
			categorySet.add(l3_category_id);
		}

		if(StringUtils.hasText(l4_category_id)){
			LOG.debug("L4 Category id extracted from incoming msg is  :::: "+l4_category_id);
			categorySet.add(l4_category_id);
		}

		return categorySet;
	}
	
	private boolean isExceptCategory(Set<String> categorySet) {
		
		boolean exceptCat = false;
		if(exceptCategoriesList.isEmpty())
			return false;

		if(categorySet.isEmpty())
			return false;

		for(String catId: categorySet){
			if(exceptCategoriesList.contains(Integer.parseInt(catId))){
				LOG.debug("category id [{}] found in the except category list :::: ", catId);
				exceptCat =true;
				break;
			}
		}
		return exceptCat;
	}
	
	private boolean isAllowedCategory(Set<String> categorySet) {
		
		boolean allowedCat = false;
		if(allowedCategoriesList.isEmpty())
			return true;
		
		if(categorySet == null)
			return true;
		
		for(String catId: categorySet){
			if(allowedCategoriesList.contains(Integer.parseInt(catId))){
				LOG.debug("category id [{}] found in the allowed category list :::: ", catId);
				allowedCat = true;
				break;
			}
		}
		return allowedCat;
	}
}