package com.example.joshuatrabkap1;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Implement the adapter's listener interface to handle clicks from the RecyclerView
public class MainActivity extends AppCompatActivity implements WeightEntryAdapter.OnItemInteractionListener {

    private UserDataSource userDataSource;
    private WeightDataSource weightDataSource;
    private long currentUserId = -1; // Use -1 if not logged in
    private Screen currentScreen = Screen.LOGIN;

    // View related fields
    private View loginScreen;
    private View dataGridScreen;
    private View smsScreen;
    private ViewGroup contentContainer;
    private BottomNavigationView bottomNavigation;

    // SMS Screen Views
    private TextView smsTextViewStatus;
    private Button smsBtnGrantPermission;
    private TextView smsTextViewPhoneNumber;

    // SMS State Management
    private String smsAlertPhoneNumber = "";
    private static final String PREFS_NAME = "WeightTrackerPrefs";
    private static final String KEY_SMS_NUMBER = "smsAlertNumber";


    // RecyclerView fields
    private RecyclerView recyclerView;
    private WeightEntryAdapter adapter;

    // Enum for navigation
    private enum Screen {
        LOGIN, DATA_GRID, SMS_ALERT
    }

    // Activity Result Launcher for Permissions
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // Handle the user's response to the permission request
                if (isGranted) {
                    Toast.makeText(this, "SMS Permission Granted.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "SMS Permission Denied. SMS alerts will not be sent.", Toast.LENGTH_LONG).show();
                }
                updateSmsUi(); // Update the UI based on the new status
            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        userDataSource = new UserDataSource(this);
        weightDataSource = new WeightDataSource(this);

        // Load the saved phone number
        loadSmsPhoneNumber();

        contentContainer = findViewById(R.id.content_container_main);
        bottomNavigation = findViewById(R.id.bottom_navigation_main);

        LayoutInflater inflater = getLayoutInflater();

        loginScreen = inflater.inflate(R.layout.res_layout_login, contentContainer, false);
        dataGridScreen = inflater.inflate(R.layout.res_layout_data_grid, contentContainer, false);
        smsScreen = inflater.inflate(R.layout.res_layout_sms_prompt, contentContainer, false);

        setupLoginScreen();
        setupDataGridScreen();
        setupSmsScreen(); // Caches views and sets listeners

        bottomNavigation.setOnItemSelectedListener(this::onNavigationItemSelected);

        showScreen(Screen.LOGIN);
    }

    //  Helper functions for SMS Phone Number persistence
    private void saveSmsPhoneNumber(String number) {
        SharedPreferences sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(KEY_SMS_NUMBER, number);
        editor.apply();
        smsAlertPhoneNumber = number;
    }

    private void loadSmsPhoneNumber() {
        SharedPreferences sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Set to a default emulator number,can change this to any 10-digit number for the emulator
        smsAlertPhoneNumber = sharedPref.getString(KEY_SMS_NUMBER, "555-521-5554");}


    //  Navigation and Screen Switching

    private boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_login) {
            showScreen(Screen.LOGIN);
            return true;
        } else if (itemId == R.id.nav_data_grid) {
            if (currentUserId != -1) {
                showScreen(Screen.DATA_GRID);
            } else {
                Toast.makeText(this, R.string.error_must_login, Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (itemId == R.id.nav_sms) {
            if (currentUserId != -1) {
                showScreen(Screen.SMS_ALERT);
            } else {
                Toast.makeText(this, R.string.error_must_login, Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return false;
    }

    private void showScreen(Screen screen) {
        currentScreen = screen;
        contentContainer.removeAllViews();
        bottomNavigation.setVisibility(View.VISIBLE);

        if (currentUserId == -1 && screen != Screen.LOGIN) {
            screen = Screen.LOGIN; // Force back to login if not authenticated
        }

        switch (screen) {
            case LOGIN:
                contentContainer.addView(loginScreen);
                bottomNavigation.setVisibility(View.GONE);
                break;
            case DATA_GRID:
                contentContainer.addView(dataGridScreen);
                loadGoalWeight();
                loadWeightEntries();
                break;
            case SMS_ALERT:
                contentContainer.addView(smsScreen);

                updateSmsUi();
                break;
        }
    }

    // Setup Screens (Login and Data Grid omitted for brevity)


    private void setupLoginScreen() {
        EditText editUsername = loginScreen.findViewById(R.id.edit_text_username);
        EditText editPassword = loginScreen.findViewById(R.id.edit_text_password);
        Button btnSignIn = loginScreen.findViewById(R.id.button_sign_in);
        Button btnCreateAccount = loginScreen.findViewById(R.id.button_create_account);
        //Validates Log in
        btnSignIn.setOnClickListener(v -> {
            String username = editUsername.getText().toString();
            String password = editPassword.getText().toString();
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            userDataSource.open();
            long userId = userDataSource.loginUser(username, password);
            userDataSource.close();

            if (userId != -1) {
                currentUserId = userId;
                Toast.makeText(this, R.string.success_signed_in, Toast.LENGTH_SHORT).show();
                showScreen(Screen.DATA_GRID);
            } else {
                Toast.makeText(this, R.string.error_invalid_credentials, Toast.LENGTH_SHORT).show();
            }
        });
        //Validates Sign Up Feature
        btnCreateAccount.setOnClickListener(v -> {
            String username = editUsername.getText().toString();
            String password = editPassword.getText().toString();
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            userDataSource.open();
            long userId = userDataSource.registerUser(username, password);
            userDataSource.close();

            if (userId != -1) {
                currentUserId = userId;
                Toast.makeText(this, R.string.success_account_created, Toast.LENGTH_SHORT).show();
                showScreen(Screen.DATA_GRID);
            } else {
                Toast.makeText(this, R.string.error_username_taken, Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void setupDataGridScreen() {
        // Goal Setting
        EditText editGoalWeight = dataGridScreen.findViewById(R.id.edit_text_goal_weight);
        Button btnSetGoal = dataGridScreen.findViewById(R.id.button_set_goal);

        btnSetGoal.setOnClickListener(v -> {
            try {
                double goalWeight = Double.parseDouble(editGoalWeight.getText().toString());
                weightDataSource.open();
                weightDataSource.setGoalWeight(currentUserId, goalWeight);
                weightDataSource.close();
                Toast.makeText(this, R.string.success_goal_set, Toast.LENGTH_SHORT).show();
                loadGoalWeight();
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.error_invalid_weight, Toast.LENGTH_SHORT).show();
            }
        });

        // New Entry Logging
        EditText editDate = dataGridScreen.findViewById(R.id.edit_text_entry_date);
        EditText editWeight = dataGridScreen.findViewById(R.id.edit_text_entry_weight);
        Button btnSubmitEntry = dataGridScreen.findViewById(R.id.button_submit_entry);

        btnSubmitEntry.setOnClickListener(v -> {
            String dateStr = editDate.getText().toString();
            String weightStr = editWeight.getText().toString();

            if (dateStr.isEmpty() || weightStr.isEmpty()) {
                Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
                return;
            }

            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            try {
                Date date = dateFormat.parse(dateStr);
                if (date == null) {
                    Toast.makeText(this, R.string.error_invalid_date_format, Toast.LENGTH_SHORT).show();
                    return;
                }

                double weight = Double.parseDouble(weightStr);

                WeightEntry entry = new WeightEntry(currentUserId, weight, date);

                weightDataSource.open();
                weightDataSource.addEntry(entry);
                weightDataSource.close();

                Toast.makeText(this, R.string.success_entry_added, Toast.LENGTH_SHORT).show();
                editDate.setText("");
                editWeight.setText("");
                loadWeightEntries();
                checkGoalAndNotify(weight); // Check goal and notify

            } catch (ParseException e) {
                Toast.makeText(this, R.string.error_invalid_date_format, Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.error_invalid_weight, Toast.LENGTH_SHORT).show();
            }
        });

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        recyclerView = dataGridScreen.findViewById(R.id.recyclerview_weight_history);
        adapter = new WeightEntryAdapter(this, new ArrayList<>(), this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void loadGoalWeight() {
        TextView goalDisplay = dataGridScreen.findViewById(R.id.title_history);
        weightDataSource.open();
        Double goal = weightDataSource.getGoalWeight(currentUserId);
        weightDataSource.close();

        if (goalDisplay == null) return;

        if (goal != null && goal != -1.0) {
            goalDisplay.setText(getString(R.string.title_weight_history) + " (Goal: " + String.format(Locale.US, "%.1f", goal) + " lbs)");
        } else {
            goalDisplay.setText(R.string.title_weight_history);
        }
    }
    //Loads Weight Entries for the current User
    private void loadWeightEntries() {
        weightDataSource.open();
        List<WeightEntry> entries = weightDataSource.getAllEntries(currentUserId);
        weightDataSource.close();

        if (adapter != null) {
            adapter.setEntries(entries);
        }
    }

    @Override
    public void onEditClick(WeightEntry entry) {
        showEditDialog(entry);
    }

    @Override //For deleting a value
    public void onDeleteClick(WeightEntry entry) {
        showDeleteConfirmationDialog(entry);
    }

    private void showEditDialog(WeightEntry entry) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.res_layout_dialog_edit_entry, null);
        builder.setView(dialogView);

        builder.setTitle(R.string.dialog_title_edit_entry);

        EditText editDate = dialogView.findViewById(R.id.edit_dialog_date);
        EditText editWeight = dialogView.findViewById(R.id.edit_dialog_weight);

        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

        editDate.setText(dateFormat.format(entry.getDate()));
        editWeight.setText(String.format(Locale.US, "%.1f", entry.getWeight()));

        builder.setPositiveButton(R.string.dialog_save, (dialog, which) -> {
            weightDataSource.open();
            try {
                Date newDate = dateFormat.parse(editDate.getText().toString());
                if (newDate == null) {
                    Toast.makeText(this, R.string.error_invalid_date_format, Toast.LENGTH_SHORT).show();
                    return;
                }

                double newWeight = Double.parseDouble(editWeight.getText().toString());

                entry.setDate(newDate);
                entry.setWeight(newWeight);

                weightDataSource.updateEntry(entry);
                checkGoalAndNotify(newWeight);

                if (adapter != null) {
                    loadWeightEntries();
                }
                Toast.makeText(this, R.string.success_entry_updated, Toast.LENGTH_SHORT).show();
            } catch (ParseException e) {
                Toast.makeText(this, R.string.error_invalid_date_format, Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.error_invalid_weight, Toast.LENGTH_SHORT).show();
            } finally {
                weightDataSource.close();
            }
        });

        builder.setNegativeButton(R.string.dialog_cancel, (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void showDeleteConfirmationDialog(WeightEntry entry) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_title_confirm_delete)
                .setMessage(R.string.dialog_message_confirm_delete)
                .setPositiveButton(R.string.dialog_delete_confirm, (dialog, which) -> {
                    weightDataSource.open();
                    weightDataSource.deleteEntry(entry.getId());
                    weightDataSource.close();

                    Toast.makeText(this, R.string.success_entry_deleted, Toast.LENGTH_SHORT).show();
                    loadWeightEntries();
                })
                .setNegativeButton(R.string.dialog_cancel, (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    // --- SMS Alert Screen Setup ---

    private void setupSmsScreen() {
        // Cache the views once during setup
        smsTextViewStatus = smsScreen.findViewById(R.id.text_sms_status);
        smsBtnGrantPermission = smsScreen.findViewById(R.id.button_grant_sms_permission);
        smsTextViewPhoneNumber = smsScreen.findViewById(R.id.text_sms_phone_number);

        // Initial button click listener
        smsBtnGrantPermission.setOnClickListener(v -> {
            boolean isGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;

            if (isGranted) {
                // If permission is already granted, prompt to set the number
                showPhoneNumberDialog();
            } else {
                // Request the permission
                requestPermissionLauncher.launch(Manifest.permission.SEND_SMS);
            }
        });


    }

    /**
     * Shows a dialog to let the user set the alert phone number.
     */
    private void showPhoneNumberDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set SMS Alert Number");

        final EditText input = new EditText(this);
        input.setHint("e.g., 5555215554");
        input.setText(smsAlertPhoneNumber.equals("555-521-5554") ? "" : smsAlertPhoneNumber);
        input.setInputType(android.text.InputType.TYPE_CLASS_PHONE);

        builder.setView(input);

        builder.setPositiveButton("Set Number", (dialog, which) -> {
            String number = input.getText().toString().trim();
            if (number.matches("^\\d{10}$") || number.equals("555-521-5554")) { //  Checks for 10 digits or the emulator default
                saveSmsPhoneNumber(number);
                Toast.makeText(this, "SMS number set to " + number, Toast.LENGTH_SHORT).show();
                updateSmsUi();
            } else {
                Toast.makeText(this, "Please enter a valid 10-digit number (e.g. 5551234567).", Toast.LENGTH_LONG).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }


    /**
     * Updates the SMS screen UI based on the current permission status and phone number.
     */
    private void updateSmsUi() {
        if (smsTextViewStatus == null || smsBtnGrantPermission == null || smsTextViewPhoneNumber == null) return;

        boolean isGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;

        // 1. Update Permission Status
        if (isGranted) {
            smsTextViewStatus.setText(R.string.status_sms_granted);
            smsBtnGrantPermission.setText(R.string.button_sms_set_number); // New string resource needed
            smsBtnGrantPermission.setEnabled(true); // Always let them set the number
        } else {
            smsTextViewStatus.setText(R.string.status_sms_permission_needed);
            smsBtnGrantPermission.setText(R.string.button_grant_sms_permission);
            smsBtnGrantPermission.setEnabled(true);
        }
        smsTextViewStatus.setVisibility(View.VISIBLE);

        // 2. Update Phone Number Status
        smsTextViewPhoneNumber.setText(getString(R.string.text_current_sms_number, smsAlertPhoneNumber)); // New string resource needed
        smsTextViewPhoneNumber.setVisibility(View.VISIBLE);
    }

    //  Goal Check Logic

    private void checkGoalAndNotify(double currentWeight) {
        weightDataSource.open();
        Double goal = weightDataSource.getGoalWeight(currentUserId);
        weightDataSource.close();

        if (goal != null && goal != -1.0 && currentWeight <= goal) {
            // Check if SMS Permission is granted
            boolean smsPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;

            if (smsPermissionGranted) {
                try {
                    String message = String.format(
                            "GOAL REACHED! Congratulations! Your latest weight is %.1f lbs, meeting or beating your goal of %.1f lbs.",
                            currentWeight,
                            goal
                    );

                    // --- Send the SMS ---
                    SmsManager smsManager = SmsManager.getDefault();
                    // Strip non-digit characters to ensure the number is clean for sending
                    String cleanNumber = smsAlertPhoneNumber.replaceAll("[^\\d]", "");

                    smsManager.sendTextMessage(cleanNumber, null, message, null, null);

                    Toast.makeText(this, "Goal Reached! SMS alert sent to " + smsAlertPhoneNumber, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(this, "SMS failed to send. Check number and try again.", Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            } else {
                // If permission NOT granted
                Toast.makeText(this, "Goal Reached! (SMS Permission needed for full alert)", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //  Lifecycle and Database (omitted for brevity)

    @Override
    protected void onResume() {
        super.onResume();
        userDataSource.open();
        weightDataSource.open();

        if (currentUserId != -1 && currentScreen == Screen.DATA_GRID) {
            loadWeightEntries();
            loadGoalWeight();
        }

        // If we return to the SMS screen, refresh the UI just in case
        if (currentScreen == Screen.SMS_ALERT) {
            loadSmsPhoneNumber();
            updateSmsUi();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        userDataSource.close();
        weightDataSource.close();
    }
}
