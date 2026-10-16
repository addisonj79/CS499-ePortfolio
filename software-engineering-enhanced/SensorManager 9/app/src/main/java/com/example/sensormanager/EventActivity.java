package com.example.sensormanager;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EventActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 100;

    private DatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private TextView textEmptyState;
    private List<EventItem> eventList;
    private EventAdapter adapter;
    private boolean smsPermissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        dbHelper = new DatabaseHelper(this);
        eventList = new ArrayList<>();

        recyclerView = findViewById(R.id.recyclerEvents);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        textEmptyState = findViewById(R.id.textEmptyState);

        // set this up once instead of rebuilding it every time the list changes
        adapter = new EventAdapter(eventList, new EventAdapter.OnItemClickListener() {
            @Override
            public void onUpdateClick(EventItem item) {
                showEventDialog(item);
            }

            @Override
            public void onDeleteClick(EventItem item) {
                confirmDelete(item);
            }
        });
        recyclerView.setAdapter(adapter);

        loadEvents();

        findViewById(R.id.btnAddEvent).setOnClickListener(v -> showEventDialog(null));
        findViewById(R.id.btnSmsPermission).setOnClickListener(v -> requestSmsPermission());

        smsPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    // pulls events from the db and refreshes the list/adapter + empty state
    private void loadEvents() {
        eventList.clear();

        Cursor cursor = dbHelper.getAllEvents();
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENT_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENT_NAME));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENT_DATE));
                eventList.add(new EventItem(id, name, date));
            } while (cursor.moveToNext());
        }
        cursor.close();

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean isEmpty = eventList.isEmpty();
        textEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void confirmDelete(EventItem item) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.confirm_delete_title)
                .setMessage(getString(R.string.confirm_delete_message, item.getName()))
                .setPositiveButton(R.string.delete_button, (dialog, which) -> {
                    if (dbHelper.deleteEvent(item.getId())) {
                        loadEvents();
                        Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Could not delete event", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel_button, null)
                .show();
    }

    private void showEventDialog(EventItem itemToUpdate) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_event, null);

        TextInputLayout layoutName = dialogView.findViewById(R.id.layoutEventName);
        TextInputLayout layoutDate = dialogView.findViewById(R.id.layoutEventDate);
        TextInputEditText editName = dialogView.findViewById(R.id.editEventName);
        TextInputEditText editDate = dialogView.findViewById(R.id.editEventDate);
        View btnPickDate = dialogView.findViewById(R.id.btnPickDate);

        boolean isEditing = itemToUpdate != null;
        if (isEditing) {
            editName.setText(itemToUpdate.getName());
            editDate.setText(itemToUpdate.getDate());
        }

        btnPickDate.setOnClickListener(v -> showDatePicker(editDate));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(isEditing ? R.string.update_event_title : R.string.add_new_event_title)
                .setView(dialogView)
                .setPositiveButton(R.string.save_button, null) // set below, need to override so it doesn't auto-close
                .setNegativeButton(R.string.cancel_button, null)
                .create();

        dialog.setOnShowListener(d -> {
            View positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String name = editName.getText() == null ? "" : editName.getText().toString().trim();
                String date = editDate.getText() == null ? "" : editDate.getText().toString().trim();

                String nameError = EventValidator.validateName(name);
                String dateError = EventValidator.validateDate(date);
                layoutName.setError(nameError);
                layoutDate.setError(dateError);

                if (nameError != null || dateError != null) {
                    return; // don't save, let them fix the error first
                }

                boolean saved = isEditing
                        ? dbHelper.updateEvent(itemToUpdate.getId(), name, date)
                        : dbHelper.addEvent(name, date);

                if (!saved) {
                    Toast.makeText(this, "Could not save event", Toast.LENGTH_SHORT).show();
                    return;
                }

                loadEvents();
                if (smsPermissionGranted) {
                    sendSmsNotification((isEditing ? "Updated event: " : "New event: ") + name + " on " + date);
                }
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private void showDatePicker(TextInputEditText target) {
        Calendar calendar = Calendar.getInstance();

        String existing = target.getText() == null ? "" : target.getText().toString().trim();
        if (EventValidator.validateDate(existing) == null) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(EventValidator.DATE_PATTERN, Locale.US);
                calendar.setTime(format.parse(existing));
            } catch (ParseException ignored) {
                // Fall back to today's date below.
            }
        }

        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String formatted = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            target.setText(formatted);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void sendSmsNotification(String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage("5551234", null, message, null, null);
            Toast.makeText(this, "SMS alert sent!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to send SMS", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        } else {
            Toast.makeText(this, "SMS permission already granted", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                smsPermissionGranted = true;
                Toast.makeText(this, "SMS Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                smsPermissionGranted = false;
                Toast.makeText(this, "SMS Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
