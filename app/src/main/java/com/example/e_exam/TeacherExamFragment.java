package com.example.e_exam;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.e_exam.adapter.TeacherExamListAdapter;
import com.example.e_exam.model.TeacherExamList;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class TeacherExamFragment extends Fragment {
    private RecyclerView recyclerView;
    private TeacherExamListAdapter adapter;
    private List<TeacherExamList> examList;
    private FirebaseFirestore db;
    private ListenerRegistration examListener;
    private String teacherId;

    public static TeacherExamFragment newInstance(String teacherId) {
        TeacherExamFragment fragment = new TeacherExamFragment();
        Bundle args = new Bundle();
        args.putString("teacherId", teacherId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher_exam, container, false);

        if (getArguments() != null) {
            teacherId = getArguments().getString("teacherId");
        }

        initializeFirebase();
        initializeViews(view);
        setupButtons(view);

        return view;
    }

    private void initializeFirebase() {
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUser.getUid());

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String customUid = snapshot.child("uid").getValue(String.class);
                    loadExams(customUid);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("TeacherExamFragment", "Error getting custom UID", error.toException());
                }
            });
        }
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TeacherExamListAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void setupButtons(View view) {
        Button createExamButton = view.findViewById(R.id.createExamButton);
        Button createTestButton = view.findViewById(R.id.createTestButton);

        createExamButton.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.frame_layout, ExamCreateFragment.newInstance(teacherId));
            transaction.addToBackStack(null);
            transaction.commit();
        });

        createTestButton.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.frame_layout, TestCreateFragment.newInstance(teacherId));
            transaction.addToBackStack(null);
            transaction.commit();
        });
    }

    private void loadExams(String customUid) {
        Log.d("TeacherExamFragment", "Loading exams for teacher: " + customUid);

        Query examQuery = db.collection("exams")
                .whereEqualTo("teacherId", customUid);

        examListener = examQuery.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("TeacherExamFragment", "Listen failed.", error);
                return;
            }

            if (value == null || value.isEmpty()) {
                Log.d("TeacherExamFragment", "No exams found for teacher: " + customUid);
                Toast.makeText(getContext(), "Không có bài kiểm tra nào", Toast.LENGTH_SHORT).show();
                return;
            }

            List<TeacherExamList> newExamList = new ArrayList<>();
            for (DocumentSnapshot document : value.getDocuments()) {
                try {
                    String id = document.getId();
                    String name = document.getString("name");
                    String classCode = document.getString("className");
                    Long timestamp = document.getLong("timestamp"); // Thời gian tạo bài kiểm tra
                    Boolean isAssigned = document.getBoolean("isAssigned"); // Trạng thái đã giao chưa
                    String pdfUrl = document.getString("pdfUrl");
                    String answerUrl = document.getString("answerUrl");

                    // Sử dụng constructor phù hợp với model
                    TeacherExamList exam = new TeacherExamList(
                            classCode,
                            name,
                            timestamp != null ? timestamp : System.currentTimeMillis()/1000,
                            isAssigned != null ? isAssigned : true,
                            id,
                            pdfUrl,
                            answerUrl
                    );

                    newExamList.add(exam);

                } catch (Exception e) {
                    Log.e("TeacherExamFragment", "Error processing exam " + document.getId(), e);
                }
            }

            examList = newExamList;
            adapter.clearExams();
            adapter.addExams(examList);
            Log.d("TeacherExamFragment", "Updated adapter with " + examList.size() + " items");
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (examListener != null) {
            examListener.remove();
        }
    }
}