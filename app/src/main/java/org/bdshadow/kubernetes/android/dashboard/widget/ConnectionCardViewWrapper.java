package org.bdshadow.kubernetes.android.dashboard.widget;

import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.CardView;
import android.text.Layout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import org.bdshadow.kubernetes.android.dashboard.R;
import org.bdshadow.kubernetes.android.dashboard.utils.Utils;

public class ConnectionCardViewWrapper {
    private CardView cardView;
    private FragmentActivity context;

    public ConnectionCardViewWrapper(FragmentActivity context) {
        this.context = context;
        this.cardView = new CardView(context);

        cardView.setId(View.generateViewId());
        CardView.MarginLayoutParams layoutParams = new CardView.MarginLayoutParams(CardView.MarginLayoutParams.MATCH_PARENT, CardView.MarginLayoutParams.WRAP_CONTENT);
        //TODO set nice margins
//        layoutParams.setMarginStart(Utils.dpToPx(16, context));
//        layoutParams.setMarginEnd(Utils.dpToPx(16, context));
        cardView.setLayoutParams(layoutParams);
        cardView.setUseCompatPadding(true);
    }

    public void fillWithJsonObject(JSONObject connectionJson) {
        TextView textView = new TextView(this.context);
        textView.setAllCaps(false);
        textView.setLayoutParams(new CardView.LayoutParams(CardView.LayoutParams.MATCH_PARENT, CardView.LayoutParams.MATCH_PARENT));
        textView.setText(connectionJson.optString("url"));
        this.cardView.addView(textView);
    }

    public void setOnClickListener(View.OnClickListener listener) {
        this.cardView.setOnClickListener(listener);
    }

    public void addToLayout(ViewGroup layout) {
        layout.addView(this.cardView);
    }
}
