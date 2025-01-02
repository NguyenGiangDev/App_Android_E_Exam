package com.example.e_exam;

import android.os.Bundle;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.e_exam.adapter.ExamListResultAdapter;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class ExamResultFragment extends Fragment {
    private View mView;
    private TextView tvClass, tvExamName, tvScore;
    private ListView lvResult;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_exam_result, container, false);
        initUI();
        processExamResults();
        return mView;
    }

    private void initUI() {
        tvClass = mView.findViewById(R.id.tv_class);
        tvExamName = mView.findViewById(R.id.tv_examName);
        tvScore = mView.findViewById(R.id.tv_score);
        lvResult = mView.findViewById(R.id.lv_result);
    }

    private void processExamResults() {
        Bundle args = getArguments();
        if (args == null) {
            Log.e("ExamResult", "No arguments passed to fragment");
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> resultData = (Map<String, Object>) args.getSerializable("resultData");
        if (resultData == null) {
            Log.e("ExamResult", "Result data is null");
            return;
        }

        // Tạo danh sách câu hỏi
        ArrayList<Map<String, Object>> questionsList = new ArrayList<>();
        ArrayList<Map.Entry<String, Object>> questionEntries = new ArrayList<>();

        // Lọc các câu hỏi từ resultData
        for (Map.Entry<String, Object> entry : resultData.entrySet()) {
            if (entry.getKey().startsWith("question_")) {
                questionEntries.add(entry);
            }
        }

        // Sắp xếp các câu hỏi theo thứ tự số học
        questionEntries.sort((entry1, entry2) -> {
            // Lấy số từ khóa (ví dụ: "question_1" -> 1)
            int num1 = Integer.parseInt(entry1.getKey().replace("question_", ""));
            int num2 = Integer.parseInt(entry2.getKey().replace("question_", ""));
            return Integer.compare(num1, num2);
        });

        // Thêm các câu hỏi đã sắp xếp vào danh sách
        for (Map.Entry<String, Object> entry : questionEntries) {
            @SuppressWarnings("unchecked")
            Map<String, Object> questionData = (Map<String, Object>) entry.getValue();
            questionsList.add(questionData);
        }

        // Tính điểm
        int correctAnswers = 0;
        for (Map<String, Object> question : questionsList) {
            String selected = (String) question.get("selected");
            String correct = (String) question.get("correct");
            if (selected != null && selected.equals(correct)) {
                correctAnswers++;
            }
        }

        // Cập nhật UI
        String examName = args.getString("examName", "Bài kiểm tra");
        String className = args.getString("className", "Lớp");
        tvExamName.setText(examName);
        tvClass.setText(className);
        tvScore.setText(String.format("Điểm: %d", correctAnswers));

        ExamListResultAdapter adapter = new ExamListResultAdapter(getContext(), questionsList);
        lvResult.setAdapter(adapter);
    }

}