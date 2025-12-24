package io.github.cactric.swalsh.ui;

import static io.github.cactric.swalsh.WifiUtils.looseValidate;
import static io.github.cactric.swalsh.WifiUtils.strictValidate;

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
import io.github.cactric.swalsh.databinding.ActivityManualBinding;

public class ManualActivity extends AppCompatActivity {
    private ActivityManualBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityManualBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets freeInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(freeInsets.left, freeInsets.top, freeInsets.right, freeInsets.bottom);
            return insets;
        });

        binding.manualToolbar.setTitle(R.string.manual_entry);
        getSavedSsid(binding.manualWifiName);

        binding.manualSubmit.setOnClickListener(view -> {
            String ssid = binding.manualWifiName.getText().toString();
            String pass = binding.manualWifiPassword.getText().toString();
            int validOrMsg = strictValidate(ssid, pass);
            if (validOrMsg != 0) {
                showInvalidDialog(validOrMsg);
            } else {
                setSavedSsid(ssid);
                Intent intent = new Intent(this, ConnectActivity.class);
                intent.putExtra("ssid", ssid);
                intent.putExtra("pass", pass);
                intent.putExtra("scan_time", new Date().getTime());
                startActivity(intent);
            }
        });
        binding.manualSubmit.setOnLongClickListener(view -> {
            // Long press for less validation
            String ssid = binding.manualWifiName.getText().toString();
            String pass = binding.manualWifiPassword.getText().toString();
            int validOrMsg = looseValidate(ssid, pass);
            if (validOrMsg != 0) {
                showInvalidDialog(validOrMsg);
            } else {
                setSavedSsid(ssid);
                Intent intent = new Intent(this, ConnectActivity.class);
                intent.putExtra("ssid", ssid);
                intent.putExtra("pass", pass);
                intent.putExtra("scan_time", new Date().getTime());
                startActivity(intent);
            }
            return true;
        });

        reportFullyDrawn();
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