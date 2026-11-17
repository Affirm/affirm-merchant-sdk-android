package com.affirm.samples;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.affirm.android.Affirm;
import com.affirm.android.AffirmPromotionButton;
import com.affirm.android.model.PromoPageType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PromoListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promo_list);
        RecyclerView list = findViewById(R.id.promo_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new PromoListAdapter(buildRows()));
    }

    private static List<PromoRow> buildRows() {
        BigDecimal[] prices = {
                BigDecimal.valueOf(50),
                BigDecimal.valueOf(110),
                BigDecimal.valueOf(250),
                BigDecimal.valueOf(500),
                BigDecimal.valueOf(1100),
                BigDecimal.valueOf(2500),
                BigDecimal.valueOf(80),
                BigDecimal.valueOf(999),
                BigDecimal.valueOf(75),
                BigDecimal.valueOf(320)
        };
        List<PromoRow> rows = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            BigDecimal price = prices[i % prices.length];
            rows.add(new PromoRow("Item " + (i + 1), price));
        }
        return rows;
    }

    private static final class PromoRow {
        final String name;
        final BigDecimal price;

        PromoRow(String name, BigDecimal price) {
            this.name = name;
            this.price = price;
        }
    }

    private static final class PromoListAdapter extends RecyclerView.Adapter<PromoHolder> {

        private final List<PromoRow> rows;

        PromoListAdapter(List<PromoRow> rows) {
            this.rows = rows;
        }

        @NonNull
        @Override
        public PromoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_promo_list, parent, false);
            return new PromoHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PromoHolder holder, int position) {
            PromoRow row = rows.get(position);
            holder.name.setText(row.name);
            holder.price.setText("$" + row.price);
            Affirm.configureWithAmount(holder.promo, PromoPageType.PRODUCT, row.price, true);
        }

        @Override
        public int getItemCount() {
            return rows.size();
        }
    }

    private static final class PromoHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView price;
        final AffirmPromotionButton promo;

        PromoHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.item_name);
            price = itemView.findViewById(R.id.item_price);
            promo = itemView.findViewById(R.id.item_promo);
        }
    }
}
