package com.vasu.simblocker;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import androidx.core.app.ActivityCompat;
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

        String cleanIncoming = normalizeNumber(incomingNumber);

        SharedPreferences prefs = getSharedPreferences("SIM_BLOCKER_PREFS", Context.MODE_PRIVATE);
        boolean sim1BlockActive = prefs.getBoolean("sim1_block_active", false);
        boolean sim2BlockActive = prefs.getBoolean("sim2_block_active", false);

        Set<String> sim1Whitelist = prefs.getStringSet("sim1_whitelist", new HashSet<>());
        Set<String> sim2Whitelist = prefs.getStringSet("sim2_whitelist", new HashSet<>());

        int simSlot = getSimSlotIndex(callDetails);

        boolean shouldBlock = false;

        if (simSlot == 0 && sim1BlockActive) {
            if (!isNumberWhitelisted(cleanIncoming, sim1Whitelist)) {
                shouldBlock = true;
            }
        } else if (simSlot == 1 && sim2BlockActive) {
            if (!isNumberWhitelisted(cleanIncoming, sim2Whitelist)) {
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
            response.setRejectCall(false);
        }

        respondToCall(callDetails, response.build());
    }

    private boolean isNumberWhitelisted(String incoming, Set<String> whitelist) {
        if (incoming.isEmpty()) return false;
        for (String allowed : whitelist) {
            String cleanAllowed = normalizeNumber(allowed);
            if (!cleanAllowed.isEmpty()) {
                if (incoming.equals(cleanAllowed) || incoming.endsWith(cleanAllowed) || cleanAllowed.endsWith(incoming)) {
                    return true;
                }
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
        PhoneAccountHandle accountHandle = details.getAccountHandle();
        if (accountHandle == null) return 0;

        String handleId = accountHandle.getId();
        if (handleId == null) return 0;

        try {
            TelecomManager tm = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
            if (tm != null && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                List<PhoneAccountHandle> accounts = tm.getCallCapablePhoneAccounts();
                if (accounts != null && accounts.size() > 1) {
                    for (int i = 0; i < accounts.size(); i++) {
                        if (accounts.get(i).getId().equals(handleId) || accounts.get(i).equals(accountHandle)) {
                            return i; 
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        try {
            SubscriptionManager sm = (SubscriptionManager) getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            if (sm != null && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                List<SubscriptionInfo> subList = sm.getActiveSubscriptionInfoList();
                if (subList != null) {
                    for (SubscriptionInfo info : subList) {
                        String subIdStr = String.valueOf(info.getSubscriptionId());
                        if (handleId.equals(subIdStr) || handleId.contains(subIdStr)) {
                            return info.getSimSlotIndex();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return 0;
    }
}
