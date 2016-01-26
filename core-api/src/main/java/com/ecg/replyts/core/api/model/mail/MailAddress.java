package com.ecg.replyts.core.api.model.mail;

/**
 * Domain object describing a mail address. Could be cloaked or uncloaked.
 */
public class MailAddress {

    private final String address;

    public MailAddress(String address) {
        if (address == null) {
            throw new IllegalArgumentException("address is null");
        }
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    /**
     * checks if the domain part of this mail address equals the given domain name. Returns true, if they are pairsAreEqual. Case insensitive.
     */
    public boolean isFromDomain(String[] domains) {
        for (String domain : domains) {
            final String requiredSuffix = "@" + domain.toLowerCase();
            if (address.toLowerCase().endsWith(requiredSuffix)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MailAddress that = (MailAddress) o;

        if (address != null ? !address.equals(that.address) : that.address != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return address != null ? address.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "MailAddress{" +
                "address='" + address + '\'' +
                '}';
    }
}
