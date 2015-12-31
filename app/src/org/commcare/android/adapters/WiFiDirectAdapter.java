package org.commcare.android.adapters;

import android.view.View;

import org.commcare.dalvik.R;
import org.commcare.dalvik.activities.CommCareWiFiDirectActivity;

import java.util.ArrayList;

/**
 * Created by willpride on 12/29/15.
 */
public class WiFiDirectAdapter extends SquareButtonAdapter {

    private final CommCareWiFiDirectActivity wiFiDirectActivity;

    public WiFiDirectAdapter(CommCareWiFiDirectActivity activity) {
        super(activity);

        this.wiFiDirectActivity = activity;
    }

    private static View.OnClickListener getSendButtonListener(final CommCareWiFiDirectActivity activity) {
        return new View.OnClickListener() {
            public void onClick(View v) {
                activity.prepareFileTransfer();
            }
        };
    }

    private static View.OnClickListener getDiscoverButtonListener(final CommCareWiFiDirectActivity activity) {
        return new View.OnClickListener() {
            public void onClick(View v) {
                activity.discoverPeers();
            }
        };
    }

    private static View.OnClickListener getSubmitButtonListener(final CommCareWiFiDirectActivity activity) {
        return new View.OnClickListener() {
            public void onClick(View v) {
                activity.submitFiles();
            }
        };
    }

    private static View.OnClickListener getChangeModeButtonListener(final CommCareWiFiDirectActivity activity) {
        return new View.OnClickListener() {
            public void onClick(View v) {
                activity.changeState();
            }
        };
    }

    @Override
    protected HomeCardDisplayData getItem(int position) {
        return getButtonDisplayData().get(position);
    }

    private ArrayList<HomeCardDisplayData> getButtonDisplayData() {
        ArrayList<HomeCardDisplayData> buttonData = new ArrayList<HomeCardDisplayData>();

        HomeCardDisplayData sendButton = HomeCardDisplayData.homeCardDataWithStaticText("Send",
                R.color.white,
                R.drawable.wifi_direct_transfer,
                R.color.cc_attention_positive_color,
                getSendButtonListener(wiFiDirectActivity));
        HomeCardDisplayData discoverButton = HomeCardDisplayData.homeCardDataWithStaticText("Discover",
                R.color.white,
                R.drawable.wifi_direct_discover,
                R.color.cc_light_cool_accent_color,
                getDiscoverButtonListener(wiFiDirectActivity));
        HomeCardDisplayData submitButton = HomeCardDisplayData.homeCardDataWithStaticText("Submit",
                R.color.white,
                R.drawable.wifi_direct_submit,
                R.color.solid_dark_orange,
                getSubmitButtonListener(wiFiDirectActivity));
        HomeCardDisplayData changeModeButton = HomeCardDisplayData.homeCardDataWithStaticText("Change Mode",
                R.color.white,
                R.drawable.wifi_direct_change_mode,
                R.color.cc_brand_text,
                getChangeModeButtonListener(wiFiDirectActivity));

        buttonData.add(changeModeButton);

        switch (wiFiDirectActivity.mState) {
            case send:
                buttonData.add(discoverButton);
                buttonData.add(sendButton);
                break;
            case submit:
                buttonData.add(submitButton);
                break;
        }

        return buttonData;
    }

    @Override
    public int getItemCount() {
        switch (wiFiDirectActivity.mState) {
            case send:
                return 3;
            case submit:
                return 2;
        }
        return 1;
    }
}
