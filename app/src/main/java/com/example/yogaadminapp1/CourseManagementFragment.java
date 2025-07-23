package com.example.yogaadminapp1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.Arrays;
import java.util.List;

public class CourseManagementFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_course_management, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerCourses);
        FloatingActionButton fabAddCourse = view.findViewById(R.id.fabAddCourse);

        // Dummy data for courses
        List<String> courses = Arrays.asList("Yoga Basics", "Advanced Yoga", "Meditation 101");
        CourseListAdapter adapter = new CourseListAdapter(courses);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Add course button click (to be implemented)
        fabAddCourse.setOnClickListener(v -> {
            // TODO: Show add course dialog or screen
        });

        return view;
    }

    // Simple RecyclerView Adapter for course names
    private static class CourseListAdapter extends RecyclerView.Adapter<CourseViewHolder> {
        private final List<String> courses;
        CourseListAdapter(List<String> courses) { this.courses = courses; }
        @NonNull
        @Override
        public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new CourseViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
            holder.bind(courses.get(position));
        }
        @Override
        public int getItemCount() { return courses.size(); }
    }

    private static class CourseViewHolder extends RecyclerView.ViewHolder {
        public CourseViewHolder(@NonNull View itemView) { super(itemView); }
        public void bind(String courseName) {
            ((android.widget.TextView) itemView).setText(courseName);
        }
    }
} 