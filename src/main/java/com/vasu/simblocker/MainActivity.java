package com.vasu.simblocker;

import android.app.AlertDialog;
import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences prefs;
    private Switch switchSim1, switchSim2;
    private ArrayList<String> sim1List, sim2List;
    private ArrayAdapter<String> adapterSim1, adapterSim2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("SIM_BLOCKER_PREFS", Context.MODE_PRIVATE);

        // కాల్ స్క్రీనింగ్ పర్మిషన్ అడగడం
        requestScreeningRole();

        switchSim1 = findViewById(R.id.switchSim1);
        switchSim2 = findViewById(R.id.switchSim2);
        Button btnAddSim1 = findViewById(R.id.btnAddSim1);
        Button btnAddSim2 = findViewById(R.id.btnAddSim2);
        ListView lvSim1 = findViewById(R.id.lvSim1);
        ListView lvSim2 = findViewById(R.id.lvSim2);

        // మునుపటి సెట్టింగ్స్ లోడ్ చేయడం
        switchSim1.setChecked(prefs.getBoolean("sim1_block_active", false));
        switchSim2.setChecked(prefs.getBoolean("sim2_block_active", false));

        switchSim1.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean("sim1_block_active", isChecked).apply();
            Toast.makeText(this, "SIM 1 Protection: " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        });

        switchSim2.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.edit().putBoolean("sim2_block_active", isChecked).apply();
            Toast.makeText(this, "SIM 2 Protection: " + (isChecked ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
        });

        // SIM 1 వైట్‌లిస్ట్
        Set<String> set1 = prefs.getStringSet("sim1_whitelist", new HashSet<>());
        sim1List = new ArrayList<>(set1);
        adapterSim1 = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sim1List);
        lvSim1.setAdapter(adapterSim1);

        // SIM 2 వైట్‌లిస్ట్
        Set<String> set2 = prefs.getStringSet("sim2_whitelist", new HashSet<>());
        sim2List = new ArrayList<>(set2);
        adapterSim2 = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sim2List);
        lvSim2.setAdapter(adapterSim2);

        // SIM 1 నంబర్ జోడించడం
        btnAddSim1.setOnClickListener(v -> showAddDialog(1));

        // SIM 2 నంబర్ జోడించడం
        btnAddSim2.setOnClickListener(v -> showAddDialog(2));

        // లిస్ట్‌లోని నంబర్‌పై నొక్కితే డిలీట్ చేయడం
        lvSim1.setOnItemClickListener((parent, view, position, id) -> {
            String num = sim1List.get(position);
            showDeleteDialog(1, num, position);
        });

        lvSim2.setOnItemClickListener((parent, view, position, id) -> {
            String num = sim2List.get(position);
            showDeleteDialog(2, num, position);
        });
    }

    private void showAddDialog(int simSlot) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Whitelist Number (SIM " + simSlot + ")");

        final EditText input = new EditText(this);
        input.setHint("Enter 10 digit mobile number");
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String number = input.getText().toString().trim();
            if (!number.isEmpty()) {
                if (simSlot == 1) {
                    sim1List.add(number);
                    adapterSim1.notifyDataSetChanged();
                    prefs.edit().putStringSet("sim1_whitelist", new HashSet<>(sim1List)).apply();
                } else {
                    sim2List.add(number);
                    adapterSim2.notifyDataSetChanged();
                    prefs.edit().putStringSet("sim2_whitelist", new HashSet<>(sim2List)).apply();
                }
                Toast.makeText(this, "Number added to SIM " + simSlot, Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeleteDialog(int simSlot, String number, int position) {
        new AlertDialog.Builder(this)
            .setTitle("Remove Number")
            .setMessage("Do you want to remove " + number + " from SIM " + simSlot + " whitelist?")
            .setPositiveButton("Delete", (dialog, which) -> {
                if (simSlot == 1) {
                    sim1List.remove(position);
                    adapterSim1.notifyDataSetChanged();
                    prefs.edit().putStringSet("sim1_whitelist", new HashSet<>(sim1List)).apply();
                } else {
                    sim2List.remove(position);
                    adapterSim2.notifyDataSetChanged();
                    prefs.edit().putStringSet("sim2_whitelist", new HashSet<>(sim2List)).apply();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void requestScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = (RoleManager) getSystemService(Context.ROLE_SERVICE);
            if (roleManager != null && !roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                Intent intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING);
                startActivityForResult(intent, 101);
            }
        }
    }
}
