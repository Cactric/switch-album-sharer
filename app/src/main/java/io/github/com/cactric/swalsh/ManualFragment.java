package io.github.com.cactric.swalsh;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;

public class ManualFragment extends Fragment {
    public ManualFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final FragmentActivity activity = requireActivity();
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_manual, container, false);

        EditText ssidEditText = root.findViewById(R.id.manual_wifi_name);
        EditText passEditText = root.findViewById(R.id.manual_wifi_password);

        root.findViewById(R.id.manual_submit).setOnClickListener(v -> {
            // Change fragment
            NavHostFragment navHostFragment = (NavHostFragment)
                    activity.getSupportFragmentManager().findFragmentById(R.id.mainFragmentContainer);
            NavController navController = navHostFragment.getNavController();

            Bundle bundle = new Bundle();
            // Construct the same kind of string that would've been in the code
            String sb = "WIFI:" + "S:" +
                    ssidEditText.getText() +
                    ";T:WPA" + // just assume it's WPA2
                    ";P:" +
                    passEditText.getText() +
                    ";;"; // blank field would be hidden (or not)

            bundle.putString("scanned_data", sb);
            navController.navigate(R.id.action_destination_scan_to_connectFragment, bundle);
        });
        return root;
    }
}