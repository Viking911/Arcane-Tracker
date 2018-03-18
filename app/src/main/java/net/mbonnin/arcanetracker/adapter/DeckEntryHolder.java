package net.mbonnin.arcanetracker.adapter;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import net.mbonnin.arcanetracker.R;
import net.mbonnin.arcanetracker.Utils;
import net.mbonnin.arcanetracker.ViewManager;
import net.mbonnin.hsmodel.Card;
import net.mbonnin.hsmodel.Rarity;

import timber.log.Timber;

import static android.view.View.GONE;

class DeckEntryHolder extends RecyclerView.ViewHolder implements View.OnTouchListener {
    private final Handler mHandler;
    ImageView gift;
    ImageView background;
    TextView cost;
    TextView name;
    TextView count;
    View overlay;

    Card card;
    private DetailsView detailsView;

    private Bitmap bitmap;
    private boolean longPress;

    private Runnable mLongPressRunnable = new Runnable() {
        @Override
        public void run() {
            longPress = true;
            displayImageViewIfNeeded();
        }

    };
    private float downY;
    private float downX;
    private DeckEntryItem deckEntry;

    private void displayImageViewIfNeeded() {
        if (longPress &&
                ("?".equals(card.id) || bitmap != null)
                && deckEntry != null) {
            if (detailsView != null) {
                Timber.d("too many imageViews");
                return;
            }

            detailsView = new DetailsView(itemView.getContext());

            /*
             * bitmap might be null if the card comes from the Hand
             */
            detailsView.configure(bitmap, deckEntry, (int) (ViewManager.Companion.get().getHeight()/1.5f));

            ViewManager.Params params = new ViewManager.Params();

            int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            detailsView.measure(measureSpec, measureSpec);
            if (detailsView.getMeasuredHeight() >= ViewManager.Companion.get().getHeight()) {
                detailsView.setTopMargin(0);
                detailsView.measure(measureSpec, measureSpec);
            }
            params.setW(detailsView.getMeasuredWidth());
            params.setH(detailsView.getMeasuredHeight());

            params.setX((int) (downX + Utils.INSTANCE.dpToPx(40)));
            params.setY((int) (downY - params.getH() / 2));
            if (params.getY() < 0) {
                params.setY(0);
            } else if (params.getY() + params.getH() > ViewManager.Companion.get().getHeight()) {
                params.setY(ViewManager.Companion.get().getHeight() - params.getH());
            }
            ViewManager.Companion.get().addModalView(detailsView, params);
        }

    }
    public DeckEntryHolder(View itemView) {
        super(itemView);
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Utils.INSTANCE.dpToPx(30));
        itemView.setLayoutParams(params);
        background = itemView.findViewById(R.id.background);
        cost = itemView.findViewById(R.id.cost);
        name = itemView.findViewById(R.id.name);
        count = itemView.findViewById(R.id.count);
        overlay = itemView.findViewById(R.id.overlay);
        gift = itemView.findViewById(R.id.gift);

        mHandler = new Handler();
        itemView.setOnTouchListener(this);
    }


    public void bind(DeckEntryItem entry) {
        this.card = entry.getCard();
        int c = entry.getCount();

        Picasso.with(itemView.getContext())
                .load("bar://" + card.id)
                .placeholder(R.drawable.hero_10)
                .into(background);

        int costInt = Utils.INSTANCE.valueOf(card.cost);
        if (costInt >= 0) {
            cost.setText(costInt + "");
            cost.setVisibility(View.VISIBLE);
        } else {
            cost.setVisibility(View.GONE);
        }
        name.setText(card.name);
        count.setVisibility(GONE);

        resetImageView();

        if (c > 0) {
            overlay.setBackgroundColor(Color.TRANSPARENT);
        } else {
            overlay.setBackgroundColor(Color.argb(150, 0, 0, 0));
        }

        if (entry.getGift()) {
            gift.setVisibility(View.VISIBLE);
        } else {
            gift.setVisibility(GONE);
        }

        if (c > 1){
            count.setVisibility(View.VISIBLE);
            count.setText(c + "");
        } else if (c == 1 && Rarity.LEGENDARY.equals(card.rarity)) {
            count.setVisibility(View.VISIBLE);
            count.setText("\u2605");
        } else {
            count.setVisibility(GONE);
        }

        deckEntry = entry;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            downX = event.getRawX();
            downY = event.getRawY();
            if (!"?".equals(card.id)) {
                Picasso.with(v.getContext()).load(Utils.INSTANCE.getCardUrl(card.id)).into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        DeckEntryHolder.this.bitmap = bitmap;
                        displayImageViewIfNeeded();
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {

                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                    }
                });
            }
            mHandler.postDelayed(mLongPressRunnable, ViewConfiguration.getLongPressTimeout());
        } else if (event.getActionMasked() == MotionEvent.ACTION_CANCEL || event.getActionMasked() == MotionEvent.ACTION_UP) {
            resetImageView();
        }

        return true;
    }

    private void resetImageView() {
        if (detailsView != null) {
            ViewManager.Companion.get().removeView(detailsView);
            detailsView = null;
        }
        mHandler.removeCallbacksAndMessages(null);
        longPress = false;
        bitmap = null;
    }
}
