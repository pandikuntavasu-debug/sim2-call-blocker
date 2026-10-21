package com.vasu.simblocker;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CallScreeningServiceImpl extends CallScreeningService {

    @Override
    public void onScreenCall(Call.Details callDetails) {
        if (callDetails.getCallDirection() != Call.Details.DIRECTION_INCOMING) {
            return;
        }

        Uri handle = callDetails.getHandle();
        String incomingNumber = "";
        if (handle != null) {
            incomingNumber = handle.getSchemeSpecificPart();
        }

        String cleanNumber = normalizeNumber(incomingNumber);

        SharedPreferences prefs = getSharedPreferences("SIM_BLOCKER_PREFS", Context.MODE_PRIVATE);
        boolean sim1BlockActive = prefs.getBoolean("sim1_block_active", false);
        boolean sim2BlockActive = prefs.getBoolean("sim2_block_active", false);

        Set<String> sim1Whitelist = prefs.getStringSet("sim1_whitelist", new HashSet<>());
        Set<String> sim2Whitelist = prefs.getStringSet("sim2_whitelist", new HashSet<>());

        int simSlot = getSimSlotIndex(callDetails);

        boolean shouldBlock = false;

        // SIM 1 లో కాల్ వస్తే తనిఖీ
        if (simSlot == 0 && sim1BlockActive) {
            if (!isNumberWhitelisted(cleanNumber, sim1Whitelist)) {
                shouldBlock = true;
            }
        }
        // SIM 2 లో కాల్ వస్తే తనిఖీ
        else if (simSlot == 1 && sim2BlockActive) {
            if (!isNumberWhitelisted(cleanNumber, sim2Whitelist)) {
                shouldBlock = true;
            }
        }

        CallResponse.Builder response = new CallResponse.Builder();
        if (shouldBlock) {
            response.setDisallowCall(true);
            response.setRejectCall(true);
            response.setSkipCallLog(false);
            response.setSkipNotification(true);
        } else {
            response.setDisallowCall(false);
        }

        respondToCall(callDetails, response.build());
    }

    private boolean isNumberWhitelisted(String incoming, Set<String> whitelist) {
        if (incoming.isEmpty()) return false;
        for (String allowed : whitelist) {
            String cleanAllowed = normalizeNumber(allowed);
            if (!cleanAllowed.isEmpty() && incoming.endsWith(cleanAllowed)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeNumber(String number) {
        if (number == null) return "";
        String clean = number.replaceAll("[^0-9]", "");
        if (clean.length() > 10) {
            clean = clean.substring(clean.length() - 10);
        }
        return clean;
    }

    private int getSimSlotIndex(Call.Details details) {
        try {
            SubscriptionManager sm = (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            if (sm != null && details.getAccountHandle() != null) {
                List<SubscriptionInfo> subList = sm.getActiveSubscriptionInfoList();
                if (subList != null) {
                    for (SubscriptionInfo info : subList) {
                        if (details.getAccountHandle().getId().contains(info.getIccId()) ||
                            details.getAccountHandle().getId().equals(String.valueOf(info.getSubscriptionId()))) {
                            return info.getSimSlotIndex();
                        }
                    }
                }
            }
        } catch (SecurityException ignored) {
        }
        return 0;
    }
}
