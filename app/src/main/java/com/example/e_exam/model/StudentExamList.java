package com.example.e_exam.model;

import java.io.Serializable;  // Thêm import Serializable
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class StudentExamList implements Serializable {  // Thêm implements Serializable
    private String id;
    private String className;
    private String name;
    private String status;
    private long dueDate;
    private String pdfUrl;
    private String answerUrl;
    private int score;
    private long submittedAt;

    // Constructor
    public StudentExamList(String className, String name, String status, long dueDate, String id) {
        this.id = id;
        this.className = className;
        this.name = name;
        this.status = status;
        this.dueDate = dueDate;
    }

    // Getter và Setter
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getDueDate() {
        return dueDate;
    }

    public void setDueDate(long dueDate) {
        this.dueDate = dueDate;
    }

    public String getPdfUrl() {
        return pdfUrl;
    }

    public void setPdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
    }

    public String getAnswerUrl() {
        return answerUrl;
    }

    public void setAnswerUrl(String answerUrl) {
        this.answerUrl = answerUrl;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public long getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(long submittedAt) {
        this.submittedAt = submittedAt;
    }

    // Phương thức để hiển thị ngày tháng
    public String getFormattedDate() {
        // Kiểm tra dueDate có giá trị hợp lệ
        if (dueDate <= 0) {
            return "Deadline: N/A";
        }

        try {
            // Tạo đối tượng SimpleDateFormat với pattern cụ thể
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());

            // Thiết lập múi giờ Việt Nam
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

            // Chuyển đổi timestamp sang milliseconds nếu cần
            long timestampInMillis = dueDate;
            if (String.valueOf(dueDate).length() <= 10) {
                timestampInMillis = dueDate * 1000; // Chuyển từ seconds sang milliseconds
            }

            // Format ngày tháng
            return "Deadline: " + sdf.format(new Date(timestampInMillis));
        } catch (Exception e) {
            // Xử lý ngoại lệ nếu có lỗi khi format
            return "Deadline: Error";
        }
    }
}
