package io.github.cactric.swalsh.ui.scan_flow_fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.transition.TransitionInflater;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;

import java.util.Date;

import io.github.cactric.swalsh.R;

public class ManualFragment extends Fragment {
    public ManualFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TransitionInflater ti = TransitionInflater.from(requireContext());
        setEnterTransition(ti.inflateTransition(android.R.transition.slide_right));
        setExitTransition(ti.inflateTransition(android.R.transition.fade));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final FragmentActivity activity = requireActivity();
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_manual, container, false);

        EditText ssidEditText = root.findViewById(R.id.manual_wifi_name);
        EditText passEditText = root.findViewById(R.id.manual_wifi_password);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String last_manual_ssid = sp.getString("last_manual_ssid", null);
        if (last_manual_ssid != null)
            ssidEditText.setText(last_manual_ssid);

        root.findViewById(R.id.manual_submit).setOnClickListener(v -> {
            // Change fragment
            NavHostFragment navHostFragment = (NavHostFragment)
                    activity.getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
            NavController navController = null;
            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();
            }

            // Do some validation
            if (!ssidEditText.getText().toString().startsWith("switch_")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setMessage(R.string.bad_prefix);
                builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
                builder.setOnDismissListener(dialog -> {});
                builder.create().show();
                return;
            }
            if (passEditText.getText().length() != 8) {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setMessage(R.string.bad_password);
                builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());
                builder.setOnDismissListener(dialog -> {});
                builder.create().show();
                return;
            }

            // Save the network name so the user won't have to type it in again
            SharedPreferences.Editor spEditor = sp.edit();
            spEditor.putString("last_manual_ssid", ssidEditText.getText().toString());
            spEditor.apply();

            Bundle bundle = new Bundle();
            // Construct the same kind of string that would've been in the code
            String sb = "WIFI:" + "S:" +
                    ssidEditText.getText() +
                    ";T:WPA" + // just assume it's WPA2
                    ";P:" +
                    passEditText.getText() +
                    ";;"; // blank field would be hidden (or not)

            bundle.putString("scanned_data", sb);
            Date d = new Date();
            bundle.putLong("scan_time", d.getTime());
            if (navController != null) {
                navController.navigate(R.id.action_manual_to_connect, bundle);
            }
        });
        root.findViewById(R.id.manual_submit).setOnLongClickListener(v -> {
            // Long press for looser validation
            // (in case something changes)
            NavHostFragment navHostFragment = (NavHostFragment)
                    activity.getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
            NavController navController = null;
            if (navHostFragment != null) {
                navController = navHostFragment.getNavController();
            }

            if (ssidEditText.getText().length() < 1) {
                Toast.makeText(activity, activity.getString(R.string.ssid_required), Toast.LENGTH_SHORT).show();
                return false;
            }
            if (passEditText.getText().length() < 1) {
                Toast.makeText(activity, activity.getString(R.string.password_required), Toast.LENGTH_SHORT).show();
                return false;
            }

            // Save the network name so the user won't have to type it in again
            SharedPreferences.Editor spEditor = sp.edit();
            spEditor.putString("last_manual_ssid", ssidEditText.getText().toString());
            spEditor.apply();

            Bundle bundle = new Bundle();
            // Construct the same kind of string that would've been in the code
            String sb = "WIFI:" + "S:" +
                    ssidEditText.getText() +
                    ";T:WPA" + // just assume it's WPA2
                    ";P:" +
                    passEditText.getText() +
                    ";;"; // blank field would be hidden (or not)

            bundle.putString("scanned_data", sb);
            if (navController != null) {
                navController.navigate(R.id.action_manual_to_connect, bundle);
            }
            return true;
        });
        return root;
    }
}