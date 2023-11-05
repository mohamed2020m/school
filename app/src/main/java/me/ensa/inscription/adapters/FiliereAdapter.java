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
import me.ensa.inscription.classes.Filiere;

public class FiliereAdapter extends RecyclerView.Adapter<FiliereAdapter.FiliereViewHolder>{
    private List<Filiere> filieres;
    private Context context;

    public FiliereAdapter(Context context) {
        this.context = context;
    }

    public List<Filiere> getFilieres() {
        return filieres;
    }

    public void addFiliere(Filiere Filiere) {
        // Check if the dataset exists
        if (filieres == null) {
            filieres = new ArrayList<>();
        }

        // Add the new Filiere to the dataset
        filieres.add(Filiere);

        // Notify the adapter that the dataset has changed
        notifyDataSetChanged();
    }

    public void setFilieres(List<Filiere> filieres) {
        this.filieres = filieres;
        notifyDataSetChanged();
    }

    public List<Filiere> getData(){
        return filieres;
    }

    public void removeItem(int position) {
        filieres.remove(position);
        notifyItemRemoved(position);
    }

    public void restoreItem(Filiere item, int position) {
        filieres.add(position, item);
        notifyItemInserted(position);
    }

    @NonNull
    @Override
    public FiliereAdapter.FiliereViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(this.context).inflate(R.layout.filieres_list, viewGroup, false);
        final FiliereAdapter.FiliereViewHolder holder = new FiliereAdapter.FiliereViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull FiliereAdapter.FiliereViewHolder FiliereViewHolder, int i) {
        FiliereViewHolder.name.setText(filieres.get(i).getLibelle());
        FiliereViewHolder.code.setText(filieres.get(i).getCode());
    }

    @Override
    public int getItemCount() {
        if(filieres == null){
            return 0;
        }
        return filieres.size();
    }

    public class FiliereViewHolder extends RecyclerView.ViewHolder {
        TextView name, code;
        CardView parent;
        public FiliereViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name_filiere);
            code = itemView.findViewById(R.id.code_filiere);
            parent = itemView.findViewById(R.id.parent);
        }
    }

}