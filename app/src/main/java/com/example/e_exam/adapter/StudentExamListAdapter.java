package com.example.e_exam.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.e_exam.R;
import com.example.e_exam.model.StudentExamList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class StudentExamListAdapter extends RecyclerView.Adapter<StudentExamListAdapter.ViewHolder> {
    private final List<StudentExamList> examList = new ArrayList<>();  // Changed to private final
    private final List<StudentExamList> allExamList = new ArrayList<>();
    private OnExamClickListener listener;
    private boolean showAllButton = true;
    private boolean showDoneButton = true;
    private boolean showPendingButton = true;

    public interface OnExamClickListener {
        void onExamClick(StudentExamList exam);
    }

    public void setOnExamClickListener(OnExamClickListener listener) {
        this.listener = listener;
    }

    public void setButtonVisibility(boolean showAll, boolean showDone, boolean showPending) {
        this.showAllButton = showAll;
        this.showDoneButton = showDone;
        this.showPendingButton = showPending;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_student_exam, parent, false);

        View btnAll = parent.getRootView().findViewById(R.id.btnAllExam);
        View btnDone = parent.getRootView().findViewById(R.id.btnDoneExam);
        View btnPending = parent.getRootView().findViewById(R.id.btnPendingExam);

        if (btnAll != null) btnAll.setVisibility(showAllButton ? View.VISIBLE : View.GONE);
        if (btnDone != null) btnDone.setVisibility(showDoneButton ? View.VISIBLE : View.GONE);
        if (btnPending != null) btnPending.setVisibility(showPendingButton ? View.VISIBLE : View.GONE);

        return new ViewHolder(view, listener, examList);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentExamList exam = examList.get(position);
        holder.bind(exam);

        if ("completed".equals(exam.getStatus())) {
            holder.itemView.findViewById(R.id.examButton)
                    .setBackgroundColor(Color.parseColor("#4CAF50")); // Green
            holder.itemView.findViewById(R.id.examButton)
                    .setEnabled(true);
        } else {
            holder.itemView.findViewById(R.id.examButton)
                    .setBackgroundColor(Color.parseColor("#65558F")); // Primary
            holder.itemView.findViewById(R.id.examButton)
                    .setEnabled(true);
        }
    }

    @Override
    public int getItemCount() {
        return examList.size();
    }

    public void addExams(List<StudentExamList> newExams) {
        this.examList.addAll(newExams);
        this.allExamList.addAll(newExams);
        notifyDataSetChanged();
    }

    // Add method to clear the list if needed
    public void clearExams() {
        this.examList.clear();
        this.allExamList.clear();
        notifyDataSetChanged();
    }

    public void showAllExams() {
        if (!showAllButton) {
            return;
        }
        examList.clear();
        examList.addAll(allExamList);
        notifyDataSetChanged();
    }

    public void filterExams(boolean completed) {
        if ((completed && !showDoneButton) || (!completed && !showPendingButton)) return;

        examList.clear();
        examList.addAll(allExamList.stream()
                .filter(exam -> completed ?
                        "completed".equals(exam.getStatus()) :
                        !"completed".equals(exam.getStatus()))
                .collect(Collectors.toList()));
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView classNameTextView;
        private final TextView nameTextView;
        private final TextView dueTextView;
        private final Button examButton;

        public ViewHolder(View itemView, OnExamClickListener listener, List<StudentExamList> examList) {
            super(itemView);

            classNameTextView = itemView.findViewById(R.id.classNameTextView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            dueTextView = itemView.findViewById(R.id.dueTextView);
            examButton = itemView.findViewById(R.id.examButton);

            examButton.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onExamClick(examList.get(position));
                }
            });
        }

        void bind(StudentExamList exam) {
            classNameTextView.setText(exam.getClassName());
            nameTextView.setText(exam.getName());
            dueTextView.setText(exam.getFormattedDate());

            if (Objects.equals(exam.getStatus(), "pending")) {
                examButton.setBackgroundColor(itemView.getContext().getColor(R.color.primary));
                examButton.setText(itemView.getContext().getString(R.string.is_pending));
                examButton.setEnabled(true);
            } else if (Objects.equals(exam.getStatus(), "completed")) {
                examButton.setBackgroundColor(itemView.getContext().getColor(R.color.green));
                examButton.setText(itemView.getContext().getString(R.string.is_done));
                examButton.setEnabled(true);
            } else {
                examButton.setBackgroundColor(itemView.getContext().getColor(R.color.red));
                examButton.setText(itemView.getContext().getString(R.string.is_outdated));
                examButton.setEnabled(false);
            }
        }
    }
}