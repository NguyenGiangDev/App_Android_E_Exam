package com.example.e_exam;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class ClassStudent extends AppCompatActivity {

    private TextView classNameTextView;
    private CardView cardAssignment;
    private String className;
    private String StudentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class_student); // Đảm bảo tên file layout đúng

        classNameTextView = findViewById(R.id.class_name_text_view);
        cardAssignment = findViewById(R.id.card_assignment_student);

        // Lấy tên lớp từ Intent
        className = getIntent().getStringExtra("className");
        StudentId = getIntent().getStringExtra("StudentId");
        classNameTextView.setText(className);

        // Đảm bảo các CardView không bị null trước khi gán sự kiện
        if (cardAssignment != null) {
            cardAssignment.setOnClickListener(v -> {
                openExamStudentActivity();
            });
        }
    }

    // Phương thức mở AddExam Activity
    private void openExamStudentActivity() {
        Intent intent = new Intent(ClassStudent.this, ExamStudentFragment.class);
        intent.putExtra("CLASS_NAME", className);
        intent.putExtra("StudentId", StudentId);
        startActivity(intent);
    }
}
