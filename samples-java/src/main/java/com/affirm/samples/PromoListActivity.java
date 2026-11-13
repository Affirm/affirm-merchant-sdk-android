package com.affirm.samples;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.affirm.android.Affirm;
import com.affirm.android.AffirmPromotionButton;
import com.affirm.android.model.PromoPageType;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * AffirmPromotionButton inside recycled rows: one button per row view, configured again with
 * each row's amount when the view is rebound.
 */
public class PromoListActivity extends AppCompatActivity {

    private static final int ROW_COUNT = 100;

    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
    private RecyclerView list;
    private TextView stats;
    private int createdCount;
    private int reboundCount;
    private int configureCount;
    private final ViewTreeObserver.OnPreDrawListener statusUpdater = () -> {
        // A promo changes without a callback, e.g. its HTML becomes visible when ready.
        for (int i = 0; i < list.getChildCount(); i++) {
            ((PromoHolder) list.getChildViewHolder(list.getChildAt(i))).showStatus();
        }
        return true;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promo_list);
        currencyFormat.setMaximumFractionDigits(0);
        stats = findViewById(R.id.stats);
        list = findViewById(R.id.promo_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(new PromoListAdapter());
        list.getViewTreeObserver().addOnPreDrawListener(statusUpdater);
        updateStats();
    }

    @Override
    protected void onDestroy() {
        list.getViewTreeObserver().removeOnPreDrawListener(statusUpdater);
        super.onDestroy();
    }

    private static BigDecimal amount(int position) {
        return BigDecimal.valueOf(100 + position * 45);
    }

    private void updateStats() {
        stats.setText("Views created: " + createdCount + "   Rebound: " + reboundCount
                + "   Configure calls: " + configureCount);
    }

    private final class PromoListAdapter extends RecyclerView.Adapter<PromoHolder> {

        @NonNull
        @Override
        public PromoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_promo_list, parent, false);
            createdCount++;
            return new PromoHolder(view, createdCount);
        }

        @Override
        public void onBindViewHolder(@NonNull PromoHolder holder, int position) {
            if (holder.bindCount > 0) {
                reboundCount++;
            }
            final BigDecimal amount = amount(position);
            holder.title.setText("Row " + position + " · " + currencyFormat.format(amount));
            holder.holderLabel.setText(holder.bindCount == 0
                    ? "view #" + holder.serial + " · new"
                    : "view #" + holder.serial + " · rebound " + holder.bindCount + "×");
            holder.bindCount++;

            configureCount++;
            Affirm.configureWithAmount(holder.promo, PromoPageType.PRODUCT, amount, true);
            updateStats();
        }

        @Override
        public int getItemCount() {
            return ROW_COUNT;
        }
    }

    private static final class PromoHolder extends RecyclerView.ViewHolder {
        final int serial;
        int bindCount;
        final TextView title;
        final TextView holderLabel;
        final AffirmPromotionButton promo;
        final TextView status;

        PromoHolder(View itemView, int serial) {
            super(itemView);
            this.serial = serial;
            title = itemView.findViewById(R.id.item_title);
            holderLabel = itemView.findViewById(R.id.item_holder);
            promo = itemView.findViewById(R.id.item_promo);
            status = itemView.findViewById(R.id.item_status);
        }

        void showStatus() {
            final String text = "Shown as " + PromoViewState.shownAs(promo);
            // Setting the same text again would lay out and draw again, every frame.
            if (!TextUtils.equals(text, status.getText())) {
                status.setText(text);
            }
        }
    }
}
