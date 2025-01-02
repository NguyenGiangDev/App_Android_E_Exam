package com.example.e_exam;

import static android.app.Activity.RESULT_OK;

import static org.apache.xmlbeans.impl.tool.StreamInstanceValidator.validateFiles;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TestCreateFragment extends Fragment {
    private static final String TAG = "TestCreate";
    private static final int PICK_PDF_REQUEST = 1;
    private static final int PICK_EXCEL_REQUEST = 2;
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String PDF_STORAGE_PATH = "exams/";
    private static final String EXCEL_STORAGE_PATH = "answers/";

    private View mView;
    private String teacherId;
    private EditText testNameInput, timeLimitInput, numberQuestionInput;
    private Button generateFileButton, createTestButton, uploadAnswerButton;
    private Spinner classPicker;
    private TextView deadlinePicker;
    private Uri pdfUri;
    private Uri excelUri;
    private Calendar selectedDeadline;
    private ClassInfo selectedClassInfo;
    private List<ClassInfo> classList;
    private DatabaseReference databaseReference;
    private ProgressDialog progressDialog;
    private FirebaseFirestore firestore;
    private StorageReference storageReference;

    // Class to hold class information
    private static class ClassInfo {
        private final String id;
        private final String name;

        public ClassInfo(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        @NonNull
        @Override
        public String toString() {
            return name;
        }
    }

    public static TestCreateFragment newInstance(String teacherId) {
        TestCreateFragment fragment = new TestCreateFragment();
        Bundle args = new Bundle();
        args.putString("teacherId", teacherId);
        fragment.setArguments(args);
        return fragment;
    }

    private interface UploadErrorHandler {
        void onError(String fileType, Exception e);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_test_create, container, false);
        if (getArguments() != null) {
            teacherId = getArguments().getString("teacherId");
        }

        initializeFirebase();
        initUI();
        setupClassPicker();
        setupDeadlinePicker();
        setupClickListeners();

        return mView;
    }

    private void initializeFirebase() {
        FirebaseApp.initializeApp(requireContext());
        databaseReference = FirebaseDatabase.getInstance().getReference("tests");
        firestore = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();
        classList = new ArrayList<>();
    }

    private void initUI(){
        testNameInput = mView.findViewById(R.id.testNameInput);
        timeLimitInput = mView.findViewById(R.id.timeLimitInput);
        numberQuestionInput = mView.findViewById(R.id.numberQuestionInput);
        classPicker = mView.findViewById(R.id.classPicker);
        deadlinePicker = mView.findViewById(R.id.deadlinePicker);
        generateFileButton = mView.findViewById(R.id.generateFileButton);
        createTestButton = mView.findViewById(R.id.createTestButton);
        uploadAnswerButton = mView.findViewById(R.id.uploadAnswerButton);
    }

    private void setupClickListeners() {
        generateFileButton.setOnClickListener(v -> pickFile("application/pdf", PICK_PDF_REQUEST));
        uploadAnswerButton.setOnClickListener(v -> pickFile("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", PICK_EXCEL_REQUEST));
        createTestButton.setOnClickListener(v -> validateAndCreateTest());
    }

    private void pickFile(String mimeType, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mimeType);
        startActivityForResult(Intent.createChooser(intent, "Select File"), requestCode);
    }

    private void validateAndCreateTest() {
        if (!validateInputs()) return;
        if (!validateFiles()) return;
        createTest();
    }

    private boolean validateInputs() {
        // Validate test name
        String testName = testNameInput.getText().toString().trim();
        if (testName.isEmpty()) {
            showError("Vui lòng nhập tên bài kiểm tra");
            testNameInput.requestFocus();
            return false;
        }

        // Validate time limit
        try {
            int timeLimit = Integer.parseInt(timeLimitInput.getText().toString().trim());
            if (timeLimit <= 0) {
                showError("Thời gian làm bài phải lớn hơn 0");
                timeLimitInput.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Vui lòng nhập thời gian hợp lệ");
            timeLimitInput.requestFocus();
            return false;
        }

        // Validate question count
        try {
            int questionCount = Integer.parseInt(numberQuestionInput.getText().toString().trim());
            if (questionCount <= 0) {
                showError("Số lượng câu hỏi phải lớn hơn 0");
                numberQuestionInput.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Vui lòng nhập số lượng câu hỏi hợp lệ");
            numberQuestionInput.requestFocus();
            return false;
        }

        // Validate deadline
        if (selectedDeadline == null || selectedDeadline.before(Calendar.getInstance())) {
            showError("Vui lòng chọn thời hạn nộp bài hợp lệ");
            return false;
        }

        // Validate class selection
        if (selectedClassInfo == null) {
            showError("Vui lòng chọn lớp học");
            return false;
        }

        return true;
    }

    private boolean validateFiles() {
        if (pdfUri == null) {
            showError("Vui lòng chọn tệp PDF");
            return false;
        }
        if (excelUri == null) {
            showError("Vui lòng chọn tệp đáp án");
            return false;
        }
        return true;
    }

    private void showError(String message) {
        if (isAdded()) {
            new AlertDialog.Builder(requireContext())
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void setupClassPicker() {
        DatabaseReference classesRef = FirebaseDatabase.getInstance().getReference("Classes");

        classesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                classList.clear();
                for (DataSnapshot classSnapshot : dataSnapshot.getChildren()) {
                    String teacherIdInClass = classSnapshot.child("teacherId").getValue(String.class);
                    if (teacherIdInClass != null && teacherIdInClass.equals(teacherId)) {
                        String className = classSnapshot.child("className").getValue(String.class);
                        String classId = classSnapshot.getKey();
                        if (className != null && classId != null) {
                            classList.add(new ClassInfo(classId, className));
                        }
                    }
                }

                ArrayAdapter<ClassInfo> adapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, classList);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                classPicker.setAdapter(adapter);
                classPicker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        selectedClassInfo = (ClassInfo) parent.getItemAtPosition(position);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        selectedClassInfo = null;
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load classes: " + databaseError.getMessage());
                Toast.makeText(requireContext(), "Failed to load classes. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupDeadlinePicker() {
        deadlinePicker.setOnClickListener(v -> {
            Calendar currentDate = Calendar.getInstance();
            int year = currentDate.get(Calendar.YEAR);
            int month = currentDate.get(Calendar.MONTH);
            int day = currentDate.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        int hour = currentDate.get(Calendar.HOUR_OF_DAY);
                        int minute = currentDate.get(Calendar.MINUTE);

                        TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(),
                                (timeView, selectedHour, selectedMinute) -> {
                                    selectedDeadline = Calendar.getInstance();
                                    selectedDeadline.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute);
                                    @SuppressLint("DefaultLocale") String formattedDeadline = String.format("%02d/%02d/%04d %02d:%02d",
                                            selectedDay, selectedMonth + 1, selectedYear, selectedHour, selectedMinute);
                                    deadlinePicker.setText(formattedDeadline);
                                }, hour, minute, true);

                        timePickerDialog.show();
                    }, year, month, day);

            datePickerDialog.show();
        });
    }

    private void createTest() {
        String testName = testNameInput.getText().toString().trim();
        String timeLimit = timeLimitInput.getText().toString().trim();
        String numberQuestion = numberQuestionInput.getText().toString().trim();

        if (testName.isEmpty()) {
            showAlertDialog("Vui lòng nhập tên bài kiểm tra");
            testNameInput.requestFocus();
            return;
        }
        if (timeLimit.isEmpty()) {
            showAlertDialog("Vui lòng nhập thời gian làm bài");
            timeLimitInput.requestFocus();
            return;
        }
        if (numberQuestion.isEmpty()) {
            showAlertDialog("Vui lòng nhập số lượng câu hỏi");
            numberQuestionInput.requestFocus();
            return;
        }
        if (selectedDeadline == null) {
            showAlertDialog("Vui lòng đặt thời hạn nộp bài");
            return;
        }
        if (selectedClassInfo == null) {
            showAlertDialog("Vui lòng chọn lớp học");
            return;
        }
        if (pdfUri == null) {
            showAlertDialog("Vui lòng chọn tệp PDF");
            return;
        }
        if (excelUri == null) {
            showAlertDialog("Vui lòng chọn tệp đáp án");
            return;
        }

        uploadFilesToFirebase(testName);
    }

    private void showAlertDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }


    private void uploadFilesToFirebase(String testName) {
        showProgressDialog("Đang tải lên tệp...");

        String pdfFileName = generateSecureFileName("pdf");
        String excelFileName = generateSecureFileName("xlsx");

        StorageReference pdfRef = storageReference.child(PDF_STORAGE_PATH + pdfFileName);
        StorageReference excelRef = storageReference.child(EXCEL_STORAGE_PATH + excelFileName);

        UploadErrorHandler errorHandler = (fileType, e) -> {
            hideProgressDialog();
            Log.e(TAG, fileType + " upload failed", e);
            showError(fileType + " tải lên thất bại: " + e.getMessage());
        };

        // Upload PDF
        uploadFile(pdfUri, pdfRef)
                .addOnSuccessListener(pdfUrl -> {
                    // Upload Excel after PDF success
                    uploadFile(excelUri, excelRef)
                            .addOnSuccessListener(excelUrl -> {
                                hideProgressDialog();
                                fetchStudentsAndSaveTest(testName, pdfUrl, excelUrl);
                            })
                            .addOnFailureListener(e -> errorHandler.onError("Excel", e));
                })
                .addOnFailureListener(e -> errorHandler.onError("PDF", e));
    }

    private Task<String> uploadFile(Uri fileUri, StorageReference ref) {
        return ref.putFile(fileUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return task.getResult().toString();
                });
    }

    private String generateSecureFileName(String extension) {
        return UUID.randomUUID().toString() + "." + extension;
    }

    private void fetchStudentsAndSaveTest(String testName, String pdfUrl, String answerUri) {
        DatabaseReference studentsRef = FirebaseDatabase.getInstance()
                .getReference("Classes")
                .child(selectedClassInfo.getId())
                .child("students");

        studentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> studentIds = new ArrayList<>();
                for (DataSnapshot student : dataSnapshot.getChildren()) {
                    studentIds.add(student.getKey());
                }
                saveTestToDatabase(testName, pdfUrl, studentIds, answerUri);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getContext(), "Failed to fetch students: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveTestToDatabase(String testName, String pdfUrl, List<String> studentIds, String answerUri) {
        String examId = generateExamId(selectedClassInfo.getName(), testName);
        Map<String, Object> examData = createExamData(examId, testName, pdfUrl, answerUri, studentIds);

        firestore.collection("exams")
                .document(examId)
                .set(examData)
                .addOnSuccessListener(aVoid ->
                        databaseReference.child(examId).setValue(examData)
                                .addOnSuccessListener(unused -> {
                                    Log.d(TAG, "Exam saved successfully!");
                                    showSuccess("Bài kiểm tra đã được tạo thành công!");
                                    clearForm();
                                    if (getFragmentManager() != null) {
                                        getFragmentManager().popBackStack();
                                    }
                                })
                                .addOnFailureListener(e -> handleDatabaseError("Realtime Database", e)))
                .addOnFailureListener(e -> handleDatabaseError("Firestore", e));
    }

    private String generateExamId(String className, String testName) {
        String baseId = className + "-" + testName;
        return baseId.replaceAll("[^a-zA-Z0-9-_]", "_");
    }

    private Map<String, Object> createExamData(String examId, String testName,
                                               String pdfUrl, String answerUri,
                                               List<String> studentIds) {
        Map<String, Object> examData = new HashMap<>();
        examData.put("id", examId);
        examData.put("name", testName);
        examData.put("className", selectedClassInfo.getName());
        examData.put("classId", selectedClassInfo.getId());
        examData.put("teacherId", teacherId);
        examData.put("numberOfQuestions", Integer.parseInt(numberQuestionInput.getText().toString().trim()));
        examData.put("timeLimit", Integer.parseInt(timeLimitInput.getText().toString().trim()));
        examData.put("deadline", selectedDeadline.getTimeInMillis());
        examData.put("createdAt", System.currentTimeMillis());
        examData.put("status", "pending");
        examData.put("pdfUrl", pdfUrl);
        examData.put("answerUrl", answerUri);
        examData.put("studentIds", studentIds);
        return examData;
    }

    private void handleDatabaseError(String databaseType, Exception e) {
        Log.e(TAG, databaseType + " save failed", e);
        showError("Lưu bài kiểm tra thất bại: " + e.getMessage());
    }

    private void showSuccess(String message) {
        if (isAdded()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void showProgressDialog(String message) {
        if (isAdded()) {
            progressDialog = new ProgressDialog(requireContext());
            progressDialog.setMessage(message);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideProgressDialog();
    }

    private void clearForm() {
        testNameInput.setText("");
        timeLimitInput.setText("");
        numberQuestionInput.setText("");
        deadlinePicker.setText("Set deadline");
        selectedDeadline = null;
        classPicker.setSelection(0);
        pdfUri = null;
        excelUri = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == PICK_PDF_REQUEST) {
                Uri selectedUri = data.getData();
                String fileType = requireContext().getContentResolver().getType(selectedUri);

                if (fileType != null && fileType.equals("application/pdf")) {
                    pdfUri = selectedUri;
                    Toast.makeText(getContext(), "Selected PDF: " + getFileName(pdfUri), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Please select a valid PDF file.", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == PICK_EXCEL_REQUEST) {
                Uri selectedUri = data.getData();
                String fileType = requireContext().getContentResolver().getType(selectedUri);

                if (fileType != null && fileType.contains("spreadsheet")) {
                    excelUri = selectedUri;
                    Toast.makeText(getContext(), "Selected Excel: " + getFileName(excelUri), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Please select a valid Excel file.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}