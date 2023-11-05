package me.ensa.inscription.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import me.ensa.inscription.R;
import me.ensa.inscription.classes.Student;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.StudentViewHolder>{
    private List<Student> students;
    private Context context;
    
    public StudentAdapter(Context context) {
        this.context = context;
    }

    public List<Student> getStudents() {
        return students;
    }

    public void addStudent(Student student) {
        // Check if the dataset exists
        if (students == null) {
            students = new ArrayList<>();
        }

        // Add the new student to the dataset
        students.add(student);

        // Notify the adapter that the dataset has changed
        notifyDataSetChanged();
    }

    public void setStudents(List<Student> students) {
        this.students = students;
        notifyDataSetChanged();
    }

    public List<Student> getData(){
        return students;
    }

    public void removeItem(int position) {
        students.remove(position);
        notifyItemRemoved(position);
    }

    public void restoreItem(Student item, int position) {
        students.add(position, item);
        notifyItemInserted(position);
    }

    @NonNull
    @Override
    public StudentViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(this.context).inflate(R.layout.students_list, viewGroup, false);
        final StudentViewHolder holder = new StudentViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull StudentViewHolder StudentViewHolder, int i) {
        StudentViewHolder.name.setText(students.get(i).getName());
        StudentViewHolder.email.setText(students.get(i).getEmail());
        StudentViewHolder.phone.setText(students.get(i).getPhone());
    }

    @Override
    public int getItemCount() {
        if(students == null){
            return 0;
        }
        return students.size();
    }

    public class StudentViewHolder extends RecyclerView.ViewHolder {
        TextView name, email, phone;
        CardView parent;
        public StudentViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            email = itemView.findViewById(R.id.email);
            phone = itemView.findViewById(R.id.phone);

            parent = itemView.findViewById(R.id.parent);
        }
    }

}
