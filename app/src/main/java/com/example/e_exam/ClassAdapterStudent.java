package com.example.e_exam;

import static android.app.PendingIntent.getActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class ClassAdapterStudent extends RecyclerView.Adapter<ClassAdapterStudent.ClassViewHolder> {

    private static final String TAG = "ClassAdapterStudent";
    private static final String CLASSES_PATH = "Classes";
    private static final String STUDENTS_PATH = "students";
    private static final String TEACHER_NAME_PATH = "teacherName";
    private static final String STUDENT_NAME_PATH = "studentName";

    private final ArrayList<String> classList;
    private final Context context;
    private final OnClassJoinListener listener;
    private final String studentId;
    private final DatabaseReference databaseRef;


    public interface OnClassJoinListener {
        void onClassJoin(String className, String studentName);
    }

    public ClassAdapterStudent(ArrayList<String> classList, Context context, OnClassJoinListener listener, String studentId) {
        this.classList = classList;
        this.context = context;
        this.listener = listener;
        this.studentId = studentId;

        this.databaseRef = FirebaseDatabase.getInstance().getReference(CLASSES_PATH);
    }

    @NonNull
    @Override
    public ClassViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_class, parent, false);
        return new ClassViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClassViewHolder holder, int position) {
        String className = classList.get(position);

        holder.bind(className);
        setupClassClickListener(holder.itemView, className);
    }

    @Override
    public int getItemCount() {
        return classList.size();
    }

    private void setupClassClickListener(View itemView, String className) {
        itemView.setOnClickListener(v -> {
            Log.d(TAG, "Clicked on class: " + className);
            if (isValidInput(className)) {
                checkStudentClassStatus(className);
            } else {
                showError("Class name or student ID is invalid.");

            }
        });
    }

    private boolean isValidInput(String className) {
        return className != null && !className.isEmpty() && studentId != null && !studentId.isEmpty();
    }

    private void checkStudentClassStatus(String className) {
        Log.d(TAG, "Checking status for student: " + studentId + " in class: " + className);
        DatabaseReference classRef = databaseRef.child(className).child(STUDENTS_PATH);

        classRef.orderByChild("studentId").equalTo(studentId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Log.d(TAG, "Student found in class with ID: " + studentId);
                    // Sinh viên đã tồn tại trong lớp, chuyển thẳng đến ClassStudent
                    navigateToClassStudent(className);
                } else {
                    Log.d(TAG, "Student not found in class, showing join dialog");
                    // Sinh viên chưa tồn tại trong lớp, hiển thị dialog tham gia
                    showJoinClassDialog(className);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                handleDatabaseError(error);
            }
        });
    }

    private void navigateToClassStudent(String className) {
        ClassStudentFragment fragment = new ClassStudentFragment();

        // Gửi dữ liệu vào Fragment thông qua Bundle
        Bundle bundle = new Bundle();
        bundle.putString("CLASS_NAME", className);
        bundle.putString("studentId", studentId);
        fragment.setArguments(bundle);

        // Kiểm tra nếu context là Activity
        if (context instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) context;
            activity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.frame_layout, fragment) // frame_layout là container của Fragment
                    .addToBackStack(null) // Thêm vào back stack
                    .commit();
        }
    }

    private void showJoinClassDialog(String className) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Bạn có muốn tham gia lớp này không?")
                .setMessage("Nhập tên của bạn:");

        EditText input = new EditText(context);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String studentName = input.getText().toString().trim();
            if (!studentName.isEmpty()) {
                listener.onClassJoin(className, studentName);
            } else {
                showError("Student name cannot be empty");
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void handleDatabaseError(DatabaseError error) {
        Log.e(TAG, "Database Error: " + error.getMessage());
        showError("Error: " + error.getMessage());
    }

    private void showError(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static class ClassViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvClassName;
        private final TextView tvTeacherName;
        private final ImageView iconCourse;
        private final DatabaseReference databaseRef;

        public ClassViewHolder(@NonNull View itemView) {
            super(itemView);
            tvClassName = itemView.findViewById(R.id.tvClassName);
            tvTeacherName = itemView.findViewById(R.id.tvTeacherName);
            iconCourse = itemView.findViewById(R.id.icon_course);
            databaseRef = FirebaseDatabase.getInstance().getReference(CLASSES_PATH);
        }

        public void bind(String className) {
            tvClassName.setText(className);
            iconCourse.setImageResource(R.drawable.course);
            loadTeacherName(className);
        }

        private void loadTeacherName(String className) {
            databaseRef.child(className).child(TEACHER_NAME_PATH)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String teacherName = snapshot.getValue(String.class);
                            tvTeacherName.setText(teacherName != null ? teacherName : "Unknown Teacher");
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Error loading teacher name: " + error.getMessage());
                        }
                    });
        }
    }
}