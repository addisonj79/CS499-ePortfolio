package com.example.sensormanager;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;


public class EventActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 100;
    private DatabaseHelper dbHelper;
    private RecyclerView recyclerView;
    private List<EventItem> eventList;
    private boolean smsPermissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        dbHelper = new DatabaseHelper(this);
        eventList = new ArrayList<>();

        recyclerView = findViewById(R.id.recyclerEvents);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        loadEvents();

        findViewById(R.id.btnAddEvent).setOnClickListener(v -> showEventDialog(null));
        findViewById(R.id.btnSmsPermission).setOnClickListener(v -> requestSmsPermission());

        smsPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

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

        EventAdapter adapter = new EventAdapter(eventList, new EventAdapter.OnItemClickListener() {
            @Override
            public void onUpdateClick(EventItem item) {
                showEventDialog(item);
            }

            @Override
            public void onDeleteClick(EventItem item) {
                if (dbHelper.deleteEvent(item.getId())) {
                    loadEvents();
                    Toast.makeText(EventActivity.this, "Event deleted", Toast.LENGTH_SHORT).show();
                }
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void showEventDialog(EventItem itemToUpdate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_event, null);
        builder.setView(dialogView);

        EditText editName = dialogView.findViewById(R.id.editEventName);
        EditText editDate = dialogView.findViewById(R.id.editEventDate);

        if (itemToUpdate != null) {
            editName.setText(itemToUpdate.getName());
            editDate.setText(itemToUpdate.getDate());
            builder.setTitle(R.string.update_event_title);
        } else {
            builder.setTitle(R.string.add_new_event_title);
        }

        builder.setPositiveButton(R.string.save_button, (dialog, which) -> {
            String name = editName.getText().toString().trim();
            String date = editDate.getText().toString().trim();

            if (name.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            if (itemToUpdate == null) {
                dbHelper.addEvent(name, date);
            } else {
                dbHelper.updateEvent(itemToUpdate.getId(), name, date);
            }

            loadEvents();
            if (smsPermissionGranted) {
                sendSmsNotification("New/Updated event: " + name + " on " + date);
            }
        });

        builder.setNegativeButton(R.string.cancel_button, null);
        builder.create().show();
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
