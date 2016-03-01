package com.ecg.de.kleinanzeigen.replyts.bankaccountfilter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;

import java.util.List;

public class NumberStream {
    private final List<String> items;

    public NumberStream(List<String> items) {
        this.items = ImmutableList.copyOf(items);
    }

    List<String> getItems() {
        return items;
    }

    public boolean containsBoth(String accountNumber, String bankCode) {
        boolean foundAccount = false, foundBankCode = false, foundBothOverlapping = false;
        for (String item : items) {
            boolean snippetHasAccount = item.contains(accountNumber);
            boolean snippetHasBankCode = item.contains(bankCode);


            if (snippetHasAccount ^ snippetHasBankCode) {
                foundAccount |= snippetHasAccount;
                foundBankCode |= snippetHasBankCode;
            } else if (snippetHasAccount && snippetHasBankCode) {

                if (overlapping(accountNumber, bankCode, item)) {
                    foundBothOverlapping = true;
                } else {
                    return true;
                }

            }

            if (foundBankCode && foundAccount) {
                return true;
            }


        }

        return (foundAccount && foundBankCode) || (foundBothOverlapping && foundAccount) || (foundBothOverlapping && foundBankCode);

    }

    private boolean overlapping(String accountNumber, String bankCode, String item) {
        int accNumIndex = item.indexOf(accountNumber);

        Range accountNumberRange = Range.closed(accNumIndex, accNumIndex+accountNumber.length()-1);

        int bankCodeIndex= item.indexOf(bankCode);
        Range bankCodeNumberRange = Range.closed(bankCodeIndex, bankCodeIndex+bankCode.length()-1);

        return bankCodeNumberRange.isConnected(accountNumberRange);
    }
}
