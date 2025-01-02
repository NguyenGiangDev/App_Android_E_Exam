package com.example.e_exam;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.e_exam.databinding.ActivityStudentBinding;
import com.example.e_exam.user.UserFragment;

public class StudentActivity extends AppCompatActivity {

    ActivityStudentBinding binding;
    private String studentID;
    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityStudentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        studentID = getIntent().getStringExtra("Customuid");
        replaceFragment(new StudentClassFragment(studentID));

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.classroom) {
                replaceFragment(new StudentClassFragment(studentID));
            } else if (itemId == R.id.exam) {
                replaceFragment(new StudentExamFragment());
            } else if (itemId == R.id.user) {
                replaceFragment(new UserFragment());
            }

            return true;
        });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    public void setBottomNavVisibility(boolean visible) {
        binding.bottomNavigationView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}