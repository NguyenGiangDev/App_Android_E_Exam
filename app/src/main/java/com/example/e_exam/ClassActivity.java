package com.example.e_exam;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class ClassActivity extends AppCompatActivity {

    private CardView cardListStudent;
    private CardView cardAssignment;
    private String className;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_class);

        // Nhận tên lớp từ Intent
        className = getIntent().getStringExtra("CLASS_NAME");

        // Hiển thị tên lớp
        TextView classNameTextView = findViewById(R.id.class_name_text_view);
        classNameTextView.setText(className);

        // CardView cho danh sách sinh viên
        cardListStudent = findViewById(R.id.card_listStudent);
        cardListStudent.setOnClickListener(v -> openListStudentActivity());

        // CardView cho danh sách điểm của học sinh
        cardAssignment = findViewById(R.id.card_assignment);
        cardAssignment.setOnClickListener(v -> openListScoreStudentActivity());
    }

    private void openListStudentActivity() {
        Intent intent = new Intent(ClassActivity.this, ListStudent2.class);
        intent.putExtra("CLASS_NAME", className);
        startActivity(intent);
    }

    private void openListScoreStudentActivity() {
        Intent intent = new Intent(ClassActivity.this, ListAssignment.class);
        intent.putExtra("CLASS_NAME", className);
        startActivity(intent);
    }
}
