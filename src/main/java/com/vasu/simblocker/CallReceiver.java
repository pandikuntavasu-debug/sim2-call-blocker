package com.vasu.simblocker;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import androidx.core.app.ActivityCompat;
import java.util.HashSet;
import java.util.Set;

public class CallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                
                if (incomingNumber != null && !incomingNumber.isEmpty()) {
                    String cleanIncoming = normalizeNumber(incomingNumber);
                    
                    android.content.SharedPreferences prefs = context.getSharedPreferences("SIM_BLOCKER_PREFS", Context.MODE_PRIVATE);
                    boolean sim1BlockActive = prefs.getBoolean("sim1_block_active", false);
                    
                    Set<String> sim1Whitelist = prefs.getStringSet("sim1_whitelist", new HashSet<>());
                    
                    if (sim1BlockActive) {
                        if (!isNumberWhitelisted(cleanIncoming, sim1Whitelist)) {
                            terminateCall(context);
                        }
                    }
                }
            }
        }
    }

    private void terminateCall(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                TelecomManager tm = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
                if (tm != null && ActivityCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                    tm.endCall();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private boolean isNumberWhitelisted(String incoming, Set<String> whitelist) {
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
}
