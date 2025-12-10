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
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.manual_toolbar);
        toolbar.setTitle(R.string.manual_entry);

        EditText ssidEditText = findViewById(R.id.manual_wifi_name);
        getSavedSsid(ssidEditText);

        EditText passEditText = findViewById(R.id.manual_wifi_password);

        findViewById(R.id.manual_submit).setOnClickListener(view -> {
            int validOrMsg = strictValidate(ssidEditText.getText().toString(), passEditText.getText().toString());
            if (validOrMsg != 0) {
                showInvalidDialog(validOrMsg);
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
            int validOrMsg = looseValidate(ssidEditText.getText().toString(), passEditText.getText().toString());
            if (validOrMsg != 0) {
                showInvalidDialog(validOrMsg);
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

    /**
     * Validates the SSID and password provided strictly
     * i.e. the SSID starts with switch_ and the password is 8 characters long
     * @param ssid The SSID to check
     * @param pass The password to check
     * @return The resource ID of the error message to display to the user, or 0 if it passes
     */
    private int strictValidate(String ssid, String pass) {
        if (!ssid.startsWith("switch_")) {
            return R.string.bad_prefix;
        }
        if (pass.length() != 8) {
            return R.string.bad_password;
        }
        return 0;
    }

    /**
     * Same as strictValidate but only checks the SSID and password are not empty
     * @param ssid The SSID to check
     * @param pass The password to check
     * @return The resource ID of the error message to display to the user, or 0 if it passes
     */
    private int looseValidate(String ssid, String pass) {
        if (ssid.isEmpty()) {
            return R.string.ssid_required;
        }
        if (pass.isEmpty()) {
            return R.string.password_required;
        }
        return 0;
    }
}