package com.example.e_exam;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.e_exam.adapter.StudentExamListAdapter;
import com.example.e_exam.model.StudentExamList;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TestStudentFragment extends Fragment {
    private RecyclerView recyclerView;
    private StudentExamListAdapter adapter;
    private List<StudentExamList> examList;
    private FirebaseFirestore db;
    private ListenerRegistration examListener;
    private String className;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_test_student, container, false);

        initializeFirebase();
        initializeViews(view);
        getIntentData();

        return view;
    }

    private void getIntentData() {
        if (getArguments() != null) {
            className = getArguments().getString("CLASS_NAME");
            if (className != null && db != null) {  // Kiểm tra db đã được khởi tạo
                Log.d("TestStudentFragment", "Loading exams for class: " + className);
                loadExams(className);
            } else {
                Log.e("TestStudentFragment", "className or db is null");
            }
        }
    }

    private void initializeViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new StudentExamListAdapter();
        adapter.setOnExamClickListener(this::onExamClick);
        recyclerView.setAdapter(adapter);
    }

    private void initializeFirebase() {
        // Khởi tạo Firestore trước
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("classes")
                    .child(currentUser.getUid());

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String classNameFromDb = snapshot.child("className").getValue(String.class);
                    // Chỉ load nếu chưa có className từ arguments
                    if (className == null) {
                        className = classNameFromDb;
                        loadExams(className);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("TestStudentFragment", "Error getting custom UID", error.toException());
                }
            });
        }
    }


    private void loadExams(String className) {
        // Kiểm tra null
        if (db == null) {
            Log.e("TestStudentFragment", "Firestore instance is null");
            return;
        }

        if (className == null || className.isEmpty()) {
            showError("Không có thông tin lớp học");
            return;
        }

        Query examQuery = db.collection("exams")
                .whereEqualTo("className", className)
                .orderBy("deadline", Query.Direction.DESCENDING);

        examListener = examQuery.addSnapshotListener((value, error) -> {

            if (error != null) {
                handleQueryError(error);
                return;
            }

            if (value != null && !value.isEmpty()) {
                processExamDocuments(value.getDocuments());
            } else {
                showEmptyMessage();
            }
        });
    }

    private void processExamDocuments(List<DocumentSnapshot> documents) {
        List<StudentExamList> newExamList = new ArrayList<>();

        for (DocumentSnapshot document : documents) {
            try {
                // Lấy dữ liệu từ document theo cấu trúc của bạn
                StudentExamList exam = new StudentExamList(
                        document.getString("className"),
                        document.getString("name"),
                        document.getString("status"),
                        document.getLong("deadline"),
                        document.getId()
                );

                exam.setPdfUrl(document.getString("pdfUrl"));
                exam.setAnswerUrl(document.getString("answerUrl"));

                // Kiểm tra deadline
                Long deadline = document.getLong("deadline");
                if (deadline != null) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime > deadline) {
                        exam.setStatus("expired");
                    }
                }

                checkExamCompletion(exam);
                newExamList.add(exam);
            } catch (Exception e) {
                Log.e("StudentExamFragment", "Error processing exam " + document.getId(), e);
            }
        }

        updateUI(newExamList);
    }

    private void updateUI(List<StudentExamList> newExamList) {
        examList = newExamList;
        adapter.clearExams();
        adapter.addExams(examList);

        if (examList.isEmpty()) {
            showEmptyMessage();
        }
    }

    private void checkExamCompletion(StudentExamList exam) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            db.collection("exams")
                    .document(exam.getId())
                    .collection("userStatus")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(userStatusDoc -> {
                        if (userStatusDoc.exists() && "completed".equals(userStatusDoc.getString("status"))) {
                            db.collection("examResults")
                                    .document(exam.getId())
                                    .collection("submissions")
                                    .document(currentUser.getUid())
                                    .get()
                                    .addOnSuccessListener(resultDoc -> {
                                        if (resultDoc.exists()) {
                                            exam.setStatus("completed");
                                            Long score = resultDoc.getLong("score");
                                            if (score != null) {
                                                exam.setScore(score.intValue());
                                            }
                                            Long submittedAt = resultDoc.getLong("submittedAt");
                                            if (submittedAt != null) {
                                                exam.setSubmittedAt(submittedAt);
                                            }
                                        } else {
                                            exam.setStatus("error");
                                        }
                                        adapter.notifyDataSetChanged();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("ExamStatus", "Error checking exam results", e);
                                        exam.setStatus("pending");
                                        adapter.notifyDataSetChanged();
                                    });
                        } else {
                            if (exam.getDueDate() < System.currentTimeMillis()) {
                                exam.setStatus("expired");
                            } else {
                                exam.setStatus("pending");
                            }
                            adapter.notifyDataSetChanged();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ExamStatus", "Error checking user status", e);
                        exam.setStatus("error");
                        adapter.notifyDataSetChanged();
                    });
        }
    }

    private void handleQueryError(FirebaseFirestoreException error) {
        String errorMessage;
        if (error.getCode() == FirebaseFirestoreException.Code.UNAVAILABLE) {
            errorMessage = "Không có kết nối mạng";
        } else if (error.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            errorMessage = "Bạn không có quyền truy cập";
        } else {
            errorMessage = "Lỗi: " + error.getMessage();
        }
        showError(errorMessage);
    }

    private void showError(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showEmptyMessage() {
        if (getContext() != null) {
            Toast.makeText(getContext(), "Không có bài kiểm tra nào", Toast.LENGTH_SHORT).show();
        }
    }

    public void onExamClick(StudentExamList exam) {
        if ("completed".equals(exam.getStatus())) {
            loadExamResult(exam);
        } else if ("expired".equals(exam.getStatus())) {
            showError("Bài kiểm tra đã hết hạn");
        } else {
            openExamDetail(exam);
        }
    }

    private void loadExamResult(StudentExamList exam) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Log.e("ExamResult", "No user is currently logged in");
            Toast.makeText(getContext(), "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            return;
        }

        String examId = exam.getId();
        String userId = currentUser.getUid();

        Log.d("ExamResult", String.format("Fetching result for exam: %s (ID: %s), User: %s",
                exam.getName(), examId, userId));

        db.collection("examResults")
                .document(examId)
                .collection("submissions")
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    Log.d("ExamResult", "Document retrieved successfully");

                    if (document.exists()) {
                        Map<String, Object> data = document.getData();
                        Log.d("ExamResult", "Document data: " + data);

                        @SuppressWarnings("unchecked")
                        ArrayList<Map<String, Object>> answersList = (ArrayList<Map<String, Object>>) document.get("answers");

                        if (answersList != null && !answersList.isEmpty()) {
                            Log.d("ExamResult", "Found " + answersList.size() + " answers");

                            // Convert answers to the format expected by ExamResultFragment
                            Map<String, Object> resultData = new HashMap<>();
                            for (Map<String, Object> answer : answersList) {
                                String questionId = String.valueOf(answer.get("questionId"));
                                Map<String, Object> questionData = new HashMap<>();
                                questionData.put("questionId", questionId);
                                questionData.put("selected", answer.get("selected"));
                                questionData.put("correct", answer.get("correct"));
                                resultData.put("question_" + questionId, questionData);
                            }

                            ExamResultFragment resultFragment = new ExamResultFragment();
                            Bundle args = new Bundle();
                            args.putSerializable("resultData", new HashMap<>(resultData));
                            args.putString("examName", exam.getName());
                            args.putString("examId", exam.getId());
                            resultFragment.setArguments(args);

                            requireActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.frame_layout, resultFragment)
                                    .addToBackStack(null)
                                    .commit();
                        } else {
                            Log.e("ExamResult", "No answers found in document");
                            Toast.makeText(getContext(),
                                    "Không tìm thấy câu trả lời trong bài làm",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("ExamResult", String.format(
                                "No submission found for exam %s (ID: %s) and user %s",
                                exam.getName(), examId, userId));
                        Toast.makeText(getContext(),
                                "Không tìm thấy bài làm của bạn",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ExamResult", String.format(
                            "Error fetching submission for exam %s (ID: %s): %s",
                            exam.getName(), examId, e.getMessage()), e);
                    Toast.makeText(getContext(),
                            "Lỗi khi tải bài làm: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void openExamDetail(StudentExamList exam) {
        ExamDetailFragment detailFragment = ExamDetailFragment.newInstance(
                exam.getId(),
                exam.getClassName(),
                exam.getName(),
                exam.getDueDate(),
                exam.getPdfUrl(),
                exam.getAnswerUrl()
        );

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, detailFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (examListener != null) {
            examListener.remove();
        }
    }
}