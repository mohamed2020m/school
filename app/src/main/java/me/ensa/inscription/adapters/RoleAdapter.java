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
import me.ensa.inscription.classes.Role;

public class RoleAdapter extends RecyclerView.Adapter<RoleAdapter.RoleViewHolder>{
    private List<Role> roles;
    private Context context;

    public RoleAdapter(Context context) {
        this.context = context;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void addRole(Role Role) {
        // Check if the dataset exists
        if (roles == null) {
            roles = new ArrayList<>();
        }

        // Add the new Role to the dataset
        roles.add(Role);

        // Notify the adapter that the dataset has changed
        notifyDataSetChanged();
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
        notifyDataSetChanged();
    }

    public List<Role> getData(){
        return roles;
    }

    public void removeItem(int position) {
        roles.remove(position);
        notifyItemRemoved(position);
    }

    public void restoreItem(Role item, int position) {
        roles.add(position, item);
        notifyItemInserted(position);
    }

    @NonNull
    @Override
    public RoleAdapter.RoleViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(this.context).inflate(R.layout.roles_list, viewGroup, false);
        final RoleAdapter.RoleViewHolder holder = new RoleAdapter.RoleViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RoleAdapter.RoleViewHolder RoleViewHolder, int i) {
        RoleViewHolder.name.setText(roles.get(i).getName());
    }

    @Override
    public int getItemCount() {
        if(roles == null){
            return 0;
        }
        return roles.size();
    }

    public class RoleViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        CardView parent;
        public RoleViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            parent = itemView.findViewById(R.id.parent);
        }
    }

}