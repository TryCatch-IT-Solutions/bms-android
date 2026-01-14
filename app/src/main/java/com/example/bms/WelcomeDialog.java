package com.example.bms;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;

public class WelcomeDialog extends AppCompatActivity {

    private String getAccess() {
        SharedPreferences sharedPreferences = getSharedPreferences(Configuration.PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString("ACCESS", "offline");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_welcome_dialog);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView greeting = findViewById(R.id.greeting);
        greeting.setText("Welcome to the app!");

        if(getAccess().equals("offline")) {
            findViewById(R.id.reminder_label).setVisibility(View.GONE);
        }

        RecyclerView announcementList = findViewById(R.id.announcement_list);
        announcementList.setLayoutManager(new LinearLayoutManager(this));

        // Example data, replace with actual data
        AnnouncementModel[] announcements = {
//                new AnnouncementModel("SALN Submission", "Please submit your SALN on or before April 30, 2022.", "2022-04-30"),
//                new AnnouncementModel("Meeting Reminder", "Team meeting at 10:00 AM on Monday.", "2022-12-12"),
//                new AnnouncementModel("Holiday Notice", "Office will be closed on December 25, 2022.", "2022-12-25")
        };

        AnnouncementAdapter adapter = new AnnouncementAdapter(Arrays.asList(announcements));
        announcementList.setAdapter(adapter);
    }
}