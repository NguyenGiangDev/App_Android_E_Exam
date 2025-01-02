package com.example.e_exam;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ExamStudentFragment extends Fragment {

    private RecyclerView recyclerView;
    private ExamAdapter examAdapter;
    private List<Exam> examList;
    private String className;
    private String StudentId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate layout cho Fragment
        View view = inflater.inflate(R.layout.fragment_exam_student, container, false);

        // Khởi tạo RecyclerView
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        examList = new ArrayList<>();
        examAdapter = new ExamAdapter(examList, requireContext());
        recyclerView.setAdapter(examAdapter);

        // Nhận giá trị className và StudentId từ Bundle
        if (getArguments() != null) {
            className = getArguments().getString("CLASS_NAME");
            StudentId = getArguments().getString("StudentId");
        }

        // Kết nối Firebase
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("exams");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                examList.clear();
                for (DataSnapshot examSnapshot : dataSnapshot.getChildren()) {
                    String examClassName = examSnapshot.child("class").getValue(String.class);
                    String name = examSnapshot.child("name").getValue(String.class);
                    if (examClassName != null && examClassName.equals(className) && name != null) {
                        examList.add(new Exam(name));
                    }
                }
                examAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("ExamStudent", "Failed to read exams", databaseError.toException());
            }
        });

        return view;
    }

    private static class Exam {
        String name;

        Exam(String name) {
            this.name = name;
        }
    }

    private static class ExamViewHolder extends RecyclerView.ViewHolder {
        TextView examName;

        ExamViewHolder(View itemView) {
            super(itemView);
            examName = itemView.findViewById(R.id.tvExamName);
        }
    }

    private class ExamAdapter extends RecyclerView.Adapter<ExamViewHolder> {
        private List<Exam> examList;
        private Context context;

        ExamAdapter(List<Exam> examList, Context context) {
            this.examList = examList;
            this.context = context;
        }

        @NonNull
        @Override
        public ExamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exam, parent, false);
            return new ExamViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ExamViewHolder holder, int position) {
            Exam exam = examList.get(position);
            holder.examName.setText(exam.name);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ExamStart.class);
                intent.putExtra("CLASS_NAME", className);
                intent.putExtra("EXAM_NAME", exam.name);
                intent.putExtra("StudentId", StudentId);
                context.startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return examList.size();
        }
    }
}
