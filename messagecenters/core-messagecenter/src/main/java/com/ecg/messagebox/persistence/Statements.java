package com.ecg.messagebox.persistence;

public enum Statements {
    // select the user's unread counts
    SELECT_USER_UNREAD_COUNTS("SELECT unread FROM mb_conversation_unread_counts WHERE usrid = ?"),

    // select the ad's unread counts
    SELECT_AD_UNREAD_COUNT("SELECT unread FROM mb_ad_conversation_unread_counts WHERE usrid = ? and adid = ?"),

    // select the user's conversations
    SELECT_CONVERSATION_INDICES("SELECT convid, adid, vis, latestmsgid FROM mb_ad_conversation_idx WHERE usrid = ?"),
    SELECT_AD_CONVERSATION_INDICES("SELECT convid, vis, latestmsgid FROM mb_ad_conversation_idx WHERE usrId = ? AND adid = ?"),
    SELECT_CONVERSATIONS("SELECT convid, vis, ntfynew, participants, adid, latestmsg, metadata FROM mb_conversations WHERE usrid = ? AND convid IN ?"),
    SELECT_CONVERSATIONS_UNREAD_COUNTS("SELECT convid, unread, unreadother FROM mb_conversation_unread_counts WHERE usrid = ?"),

    SELECT_AD_IDS("SELECT convid, adid FROM mb_conversations WHERE usrid = ? AND convid IN ?"),

    // select single conversation + messages
    SELECT_CONVERSATION("SELECT convid, adid, vis, ntfynew, participants, latestmsg, metadata FROM mb_conversations WHERE usrid = ? AND convid = ?"),
    SELECT_CONVERSATION_MESSAGE_NOTIFICATION("SELECT ntfynew FROM mb_conversations WHERE usrid = ? AND convid = ?"),
    SELECT_CONVERSATION_UNREAD_COUNT("SELECT unread FROM mb_conversation_unread_counts WHERE usrid = ? AND convid = ?"),
    SELECT_CONVERSATION_OTHER_PARTICIPANT_UNREAD_COUNT("SELECT unreadother FROM mb_conversation_unread_counts WHERE usrid = ? AND convid = ?"),
    SELECT_CONVERSATION_MESSAGES_WITHOUT_CURSOR("SELECT msgid, type, metadata FROM mb_messages WHERE usrid = ? AND convid = ? LIMIT ?"),
    SELECT_CONVERSATION_MESSAGES_WITH_CURSOR("SELECT msgid, type, metadata FROM mb_messages WHERE usrid = ? AND convid = ? AND msgid < ? LIMIT ?"),
    SELECT_CONVERSATION_IDS_BY_USER_ID_AND_AD_ID("SELECT convid from mb_ad_conversation_idx WHERE usrid = ? AND adid = ? LIMIT ?"),
    SELECT_CONVERSATION_AD_ID("SELECT adid FROM mb_conversations WHERE usrid = ? AND convid = ?"),

    // update a single conversation when a new message comes in
    UPDATE_CONVERSATION_UNREAD_COUNT("UPDATE mb_conversation_unread_counts SET unread = ? WHERE usrid = ? AND convid = ?", true),
    UPDATE_CONVERSATION_OTHER_PARTICIPANT_UNREAD_COUNT("UPDATE mb_conversation_unread_counts SET unreadother = ? WHERE usrid = ? AND convid = ?", true),
    UPDATE_AD_CONVERSATION_UNREAD_COUNT("UPDATE mb_ad_conversation_unread_counts SET unread = ? WHERE usrid = ? AND adid = ? AND convid = ?", true),

    UPDATE_AD_CONVERSATION_INDEX("UPDATE mb_ad_conversation_idx SET vis = ?, latestmsgid = ? WHERE usrid = ? AND adid = ? AND convid = ?", true),
    UPDATE_CONVERSATION_LATEST_MESSAGE("UPDATE mb_conversations SET vis = ?, latestmsg = ? WHERE usrid = ? AND convid = ?", true),
    UPDATE_CONVERSATION("UPDATE mb_conversations SET adid = ?, vis = ?, ntfynew = ?, participants = ?, latestmsg = ?, metadata = ? WHERE usrid = ? AND convid = ?", true),
    INSERT_MESSAGE("INSERT INTO mb_messages (usrid, convid, msgid, type, metadata) VALUES (?, ?, ?, ?, ?)", true),

    CHANGE_CONVERSATION_VISIBILITY("UPDATE mb_conversations SET vis = ? WHERE usrid = ? AND convid = ?", true),
    CHANGE_CONVERSATION_IDX_VISIBILITY("UPDATE mb_ad_conversation_idx SET vis = ? WHERE usrid = ? AND adid = ? AND convid = ?", true),

    // cleanup of old messages and conversations
    INSERT_AD_CONVERSATION_MODIFICATION_IDX("INSERT INTO mb_ad_conversation_modification_idx (usrid, convid, msgid, adid) VALUES (?, ?, ?, ?)", true),

    SELECT_AD_CONVERSATION_MODIFICATION_IDXS("SELECT adid, msgid FROM mb_ad_conversation_modification_idx WHERE usrid = ? AND convid = ?"),
    SELECT_LATEST_AD_CONVERSATION_MODIFICATION_IDX("SELECT adid, msgid FROM mb_ad_conversation_modification_idx WHERE usrid = ? AND convid = ? LIMIT 1"),

    DELETE_CONVERSATION_UNREAD_COUNT("DELETE FROM mb_conversation_unread_counts WHERE usrid = ? AND convid = ?", true),
    DELETE_AD_CONVERSATION_UNREAD_COUNT("DELETE FROM mb_ad_conversation_unread_counts WHERE usrid = ? AND adid = ? AND convid = ?", true),
    DELETE_AD_CONVERSATION_INDEX("DELETE FROM mb_ad_conversation_idx WHERE usrid = ? AND adid = ? AND convid = ?", true),
    DELETE_CONVERSATION("DELETE FROM mb_conversations WHERE usrid = ? AND convid = ?", true),
    DELETE_CONVERSATION_MESSAGES("DELETE FROM mb_messages WHERE usrid = ? AND convid = ?", true),

    DELETE_AD_CONVERSATION_MODIFICATION_IDX("DELETE FROM mb_ad_conversation_modification_idx WHERE usrid = ? AND convid = ? AND msgid = ?", true),
    DELETE_AD_CONVERSATION_MODIFICATION_IDXS("DELETE FROM mb_ad_conversation_modification_idx WHERE usrid = ? AND convid = ?", true),

    SELECT_RESPONSE_DATA("SELECT userid, convid, convtype, createdate, responsespeed FROM mb_response_data WHERE userid=? LIMIT ?"),
    UPDATE_RESPONSE_DATA("UPDATE mb_response_data USING TTL ? SET convtype=?, createdate=?, responsespeed=? WHERE userid=? AND convid=?", true);

    public boolean isModifying() {
        return modifying;
    }

    private final boolean modifying;
    private final String cql;

    Statements(String cql, boolean modifying) {
        this.cql = cql;
        this.modifying = modifying;
    }

    Statements(String cql) {
        this(cql, false);
    }

    public String getCql() {
        return cql;
    }

}