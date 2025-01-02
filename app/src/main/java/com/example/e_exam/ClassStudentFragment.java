package com.example.e_exam;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;

public class ClassStudentFragment extends Fragment {

    private TextView classNameTextView;
    private CardView cardAssignment, cardTest;
    private String className;
    private String studentId;
    private String testName;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_class_student, container, false); // Đảm bảo layout là fragment_class_student.xml

        initUI(view);

        // Lấy tên lớp từ arguments
        Bundle arguments = getArguments();
        if (arguments != null) {
            className = arguments.getString("CLASS_NAME");
            studentId = arguments.getString("studentId");
            testName = arguments.getString("testName");
        }
        if (className != null) {
            classNameTextView.setText(className);
        } else {
            Log.e("ClassStudentFragment", "className is null from arguments");
        }

        onClickListener();

        return view;
    }

    private void initUI(View view) {
        classNameTextView = view.findViewById(R.id.class_name_text_view);
        cardAssignment = view.findViewById(R.id.card_assignment_student);
        cardTest = view.findViewById(R.id.card_test_student);
    }

    private void onClickListener() {
        if (cardAssignment != null) {
            cardAssignment.setOnClickListener(v -> {
                openExamStudentFragment();
            });
        }

        if (cardTest != null) {
            cardTest.setOnClickListener(v -> {
                openTestStudentFragment();
            });
        }
    }

    // Phương thức mở ExamStudentFragment
    private void openExamStudentFragment() {
        ExamStudentFragment fragment = new ExamStudentFragment();

        // Gửi dữ liệu vào Fragment thông qua Bundle
        Bundle bundle = new Bundle();
        bundle.putString("CLASS_NAME", className);
        bundle.putString("studentId", studentId);
        fragment.setArguments(bundle);

        // Thêm Fragment vào Activity
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, fragment) // Đảm bảo rằng bạn có container cho fragment
                .addToBackStack(null) // Thêm vào back stack
                .commit();
    }

    private void openTestStudentFragment() {
        TestStudentFragment fragment = new TestStudentFragment();

        // Gửi dữ liệu vào Fragment thông qua Bundle
        Bundle bundle = new Bundle();
        bundle.putString("CLASS_NAME", className);
        bundle.putString("studentId", studentId);
        bundle.putString("testName", testName);
        fragment.setArguments(bundle);

        // Thêm Fragment vào Activity
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, fragment) // Đảm bảo rằng bạn có container cho fragment
                .addToBackStack(null) // Thêm vào back stack
                .commit();
    }
}
