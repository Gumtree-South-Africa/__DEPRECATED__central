package com.ecg.replyts.core.runtime.mailparser;

/**
 * gives the semantics of a Mime4J entity (only observable via complicated instanceof operations). Mime4J entities are
 * mail parts consisting of a header and a body who can be nested recursively. Typical situation: a mulitpart mail is an
 * entity with mail headers and a body. that body however contains a couple of parts which are Entities
 * themselves - each one with own mail headers an another body
 */
public enum EntityFlavour {
    /**
     * The entities body is <code>instanceof {@link org.apache.james.mime4j.dom.Entity}</code>. In theory this is possible, altough it does not make sense. It's hard
     * to figure out tough if mails with such a setup exist.
     */
    ContainsAnotherEntity,
    /**
     * the entities body is <code>instanceof {@link org.apache.james.mime4j.dom.Multipart} and therefore that body contains a list of further entities</code>
     */
    Multipart,
    /**
     * the entities body is <code>instanceof {@link org.apache.james.mime4j.dom.SingleBody} and contains actual valuable mail data</code>
     */
    ContainsSingleBody;
}
