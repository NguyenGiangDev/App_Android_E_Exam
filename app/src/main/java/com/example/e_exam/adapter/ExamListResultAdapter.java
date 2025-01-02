package com.example.e_exam.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.example.e_exam.R;

import java.util.ArrayList;
import java.util.Map;

public class ExamListResultAdapter extends BaseAdapter {
    private final Context context;
    private final ArrayList<Map<String, Object>> questions;

    public ExamListResultAdapter(Context context, ArrayList<Map<String, Object>> questions) {
        this.context = context;
        this.questions = questions;
    }

    @Override
    public int getCount() {
        return questions.size();
    }

    @Override
    public Object getItem(int position) {
        return questions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_result_exam, parent, false);
            holder = new ViewHolder();
            holder.tvQuestion = convertView.findViewById(R.id.tv_question);
            holder.tvSelected = convertView.findViewById(R.id.tv_selected);
            holder.tvCorrectAnswer = convertView.findViewById(R.id.tv_correct_answer);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Map<String, Object> question = questions.get(position);

        // Lấy dữ liệu câu hỏi
        String questionId = (String) question.get("questionId");
        String selected = (String) question.get("selected");
        String correct = (String) question.get("correct");

        // Cập nhật UI
        holder.tvQuestion.setText("Câu " + questionId);

        // Hiển thị đáp án đã chọn
        if (selected != null && !selected.isEmpty()) {
            holder.tvSelected.setText(selected);
        } else {
            holder.tvSelected.setText("X");
        }

        // Hiển thị đáp án đúng
        holder.tvCorrectAnswer.setText(correct);

        // Set màu sắc cho đáp án
        if (selected != null && !selected.isEmpty()) {
            if (selected.equals(correct)) {
                // Đáp án đúng - màu xanh
                holder.tvSelected.setTextColor(Color.parseColor("#4CAF50"));
            } else {
                // Đáp án sai - màu đỏ
                holder.tvSelected.setTextColor(Color.parseColor("#F44336"));
            }
        } else {
            // Không chọn đáp án - màu đỏ
            holder.tvSelected.setTextColor(Color.parseColor("#F44336"));
        }

        // Đáp án đúng luôn màu xanh
        holder.tvCorrectAnswer.setTextColor(Color.parseColor("#4CAF50"));

        return convertView;
    }

    static class ViewHolder {
        TextView tvQuestion;
        TextView tvSelected;
        TextView tvCorrectAnswer;
    }
}