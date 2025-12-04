package io.github.cactric.swalsh.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import java.util.Date;

import io.github.cactric.swalsh.R;

public class ManualActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manual);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets freeInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(freeInsets.left, freeInsets.top, freeInsets.right, freeInsets.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.manual_toolbar);
        toolbar.setTitle(R.string.manual_entry);

        EditText ssidEditText = findViewById(R.id.manual_wifi_name);
        getSavedSsid(ssidEditText);

        EditText passEditText = findViewById(R.id.manual_wifi_password);

        findViewById(R.id.manual_submit).setOnClickListener(view -> {
            if (!ssidEditText.getText().toString().startsWith("switch_")) {
                showInvalidDialog(R.string.bad_prefix);
            } else if (passEditText.getText().length() != 8) {
                showInvalidDialog(R.string.bad_password);
            } else {
                setSavedSsid(ssidEditText.getText().toString());
                Intent intent = new Intent(this, ConnectActivity.class);
                intent.putExtra("ssid", ssidEditText.getText().toString());
                intent.putExtra("pass", passEditText.getText().toString());
                intent.putExtra("scan_time", new Date().getTime());
                startActivity(intent);
            }
        });
        findViewById(R.id.manual_submit).setOnLongClickListener(view -> {
            // Long press for less validation
            if (ssidEditText.length() < 1) {
                showInvalidDialog(R.string.ssid_required);
            } else if (passEditText.length() < 1) {
                showInvalidDialog(R.string.password_required);
            } else {
                setSavedSsid(ssidEditText.getText().toString());
                Intent intent = new Intent(this, ConnectActivity.class);
                intent.putExtra("ssid", ssidEditText.getText().toString());
                intent.putExtra("pass", passEditText.getText().toString());
                intent.putExtra("scan_time", new Date().getTime());
                startActivity(intent);
            }
            return true;
        });

    }

    private void getSavedSsid(EditText ssidTextbox) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String last_manual_ssid = sp.getString("last_manual_ssid", null);
        if (last_manual_ssid != null)
            ssidTextbox.setText(last_manual_ssid);

    }

    private void setSavedSsid(String ssid) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor spEditor = sp.edit();
        spEditor.putString("last_manual_ssid", ssid);
        spEditor.apply();
    }

    private void showInvalidDialog(int message_res) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message_res);
        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
        builder.setOnDismissListener(dialog -> {});
        builder.create().show();
    }
}