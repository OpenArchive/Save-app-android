package net.opendasharchive.openarchive.features.settings;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import net.opendasharchive.openarchive.R;

public class OpenOrbotPreference extends Preference {
    public OpenOrbotPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.prefs_open_orbot);
        this.setSelectable(false);
        this.setOnPreferenceClickListener(null);
    }

    private View.OnClickListener onClickListener = null;

    public void setOnOpenOrbotListener(View.OnClickListener listener) {
        this.onClickListener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        Button button = (Button) holder.findViewById(R.id.open_orbot_button);
        button.setOnClickListener(v -> {
            if (onClickListener != null) {
                onClickListener.onClick(v);
            }
        });
    }
}