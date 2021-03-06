package org.commcare.activities;

import android.content.Context;
import android.content.Intent;
import android.text.Spannable;
import android.view.View;

import org.commcare.adapters.HomeCardDisplayData;
import org.commcare.adapters.SquareButtonViewHolder;
import org.commcare.dalvik.R;
import org.commcare.google.services.analytics.GoogleAnalyticsFields;
import org.commcare.google.services.analytics.GoogleAnalyticsUtils;
import org.commcare.utils.SessionUnavailableException;
import org.commcare.utils.StorageUtils;
import org.commcare.utils.SyncDetailCalculations;
import org.javarosa.core.services.locale.Localization;

import java.util.Vector;

/**
 * Build objects that contain all info needed to draw home screen buttons
 *
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class HomeButtons {
    private final static String[] buttonNames =
            new String[]{"start", "saved", "incomplete", "sync", "report", "logout"};

    public static HomeCardDisplayData[] buildButtonData(StandardHomeActivity activity,
                                                        Vector<String> buttonsToHide,
                                                        boolean isDemoUser) {
        String syncKey, homeMessageKey, logoutMessageKey;
        if (!isDemoUser) {
            homeMessageKey = "home.start";
            syncKey = "home.sync";
            logoutMessageKey = "home.logout";
        } else {
            syncKey = "home.sync.demo";
            homeMessageKey = "home.start.demo";
            logoutMessageKey = "home.logout.demo";
        }

        HomeCardDisplayData[] allButtons = new HomeCardDisplayData[]{
                HomeCardDisplayData.homeCardDataWithStaticText(Localization.get(homeMessageKey),
                        R.color.white,
                        R.drawable.home_start,
                        R.color.cc_attention_positive_color,
                        getStartButtonListener(activity)),
                HomeCardDisplayData.homeCardDataWithStaticText(Localization.get("home.forms.saved"),
                        R.color.white,
                        R.drawable.home_saved,
                        R.color.cc_light_cool_accent_color,
                        getViewOldFormsListener(activity)),
                HomeCardDisplayData.homeCardDataWithDynamicText(Localization.get("home.forms.incomplete"), R.color.white,
                        R.drawable.home_incomplete,
                        R.color.solid_dark_orange,
                        getIncompleteButtonListener(activity),
                        getIncompleteButtonTextSetter(activity)),
                HomeCardDisplayData.homeCardDataWithNotification(Localization.get(syncKey), R.color.white,
                        R.color.white,
                        R.drawable.home_sync,
                        R.color.cc_brand_color,
                        R.color.cc_brand_text,
                        getSyncButtonListener(activity),
                        getSyncButtonTextSetter(activity)),
                HomeCardDisplayData.homeCardDataWithStaticText(Localization.get("home.report"), R.color.white,
                        R.drawable.home_report, R.color.cc_attention_negative_color,
                        getReportButtonListener(activity)),
                HomeCardDisplayData.homeCardDataWithNotification(Localization.get(logoutMessageKey), R.color.white,
                        R.color.white,
                        R.drawable.home_logout, R.color.cc_neutral_color, R.color.cc_neutral_text,
                        getLogoutButtonListener(activity),
                        getLogoutButtonTextSetter(activity)),
        };

        return getVisibleButtons(allButtons, buttonsToHide);
    }

    private static HomeCardDisplayData[] getVisibleButtons(HomeCardDisplayData[] allButtons,
                                                           Vector<String> buttonsToHide) {
        int visibleButtonCount = buttonNames.length - buttonsToHide.size();
        HomeCardDisplayData[] buttons = new HomeCardDisplayData[visibleButtonCount];
        int visibleIndex = 0;
        for (int i = 0; i < buttonNames.length; i++) {
            if (!buttonsToHide.contains(buttonNames[i])) {
                buttons[visibleIndex] = allButtons[i];
                visibleIndex++;
            }
        }
        return buttons;
    }

    private static View.OnClickListener getViewOldFormsListener(final StandardHomeActivity activity) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reportButtonClick(GoogleAnalyticsFields.LABEL_SAVED_FORMS_BUTTON);
                activity.goToFormArchive(false);
            }
        };
    }

    private static View.OnClickListener getSyncButtonListener(final StandardHomeActivity activity) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reportButtonClick(GoogleAnalyticsFields.LABEL_SYNC_BUTTON);
                activity.syncButtonPressed();
            }
        };
    }

    private static TextSetter getSyncButtonTextSetter(final StandardHomeActivity activity) {
        return new TextSetter() {
            @Override
            public void update(HomeCardDisplayData cardDisplayData,
                               SquareButtonViewHolder squareButtonViewHolder,
                               Context context,
                               String notificationText) {
                if (notificationText != null) {
                    squareButtonViewHolder.subTextView.setText(notificationText);
                    squareButtonViewHolder.subTextView.setTextColor(activity.getResources().getColor(cardDisplayData.subTextColor));
                } else {
                    SyncDetailCalculations.updateSubText(activity, squareButtonViewHolder, cardDisplayData);
                }
                squareButtonViewHolder.subTextView.setBackgroundColor(activity.getResources().getColor(cardDisplayData.subTextBgColor));
                squareButtonViewHolder.textView.setTextColor(context.getResources().getColor(cardDisplayData.textColor));
                squareButtonViewHolder.textView.setText(cardDisplayData.text);
            }
        };
    }

    private static View.OnClickListener getStartButtonListener(final StandardHomeActivity activity) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reportButtonClick(GoogleAnalyticsFields.LABEL_START_BUTTON);
                activity.enterRootModule();
            }
        };
    }

    private static View.OnClickListener getIncompleteButtonListener(final StandardHomeActivity activity) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reportButtonClick(GoogleAnalyticsFields.LABEL_INCOMPLETE_FORMS_BUTTON);
                activity.goToFormArchive(true);
            }
        };
    }

    private static TextSetter getIncompleteButtonTextSetter(final StandardHomeActivity activity) {
        return new TextSetter() {
            @Override
            public void update(HomeCardDisplayData cardDisplayData,
                               SquareButtonViewHolder squareButtonViewHolder,
                               Context context,
                               String notificationText) {
                int numIncompleteForms;
                try {
                    numIncompleteForms = StorageUtils.getNumIncompleteForms();
                } catch (SessionUnavailableException e) {
                    // stop button setup, since redirection to login is imminent
                    return;
                }

                if (numIncompleteForms > 0) {
                    Spannable incompleteIndicator =
                            (activity.localize("home.forms.incomplete.indicator",
                                    new String[]{String.valueOf(numIncompleteForms),
                                            Localization.get("home.forms.incomplete")}));
                    squareButtonViewHolder.textView.setText(incompleteIndicator);
                } else {
                    squareButtonViewHolder.textView.setText(activity.localize("home.forms.incomplete"));
                }
                squareButtonViewHolder.textView.setTextColor(context.getResources()
                        .getColor(cardDisplayData.textColor));
                squareButtonViewHolder.subTextView.setVisibility(View.GONE);
            }
        };
    }

    private static View.OnClickListener getLogoutButtonListener(final StandardHomeActivity activity) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reportButtonClick(GoogleAnalyticsFields.LABEL_LOGOUT_BUTTON);
                activity.userTriggeredLogout();
            }
        };
    }

    private static TextSetter getLogoutButtonTextSetter(final StandardHomeActivity activity) {
        return new TextSetter() {
            @Override
            public void update(HomeCardDisplayData cardDisplayData,
                               SquareButtonViewHolder squareButtonViewHolder,
                               Context context,
                               String notificationText) {
                squareButtonViewHolder.textView.setText(cardDisplayData.text);
                squareButtonViewHolder.textView.setTextColor(context.getResources().getColor(cardDisplayData.textColor));
                squareButtonViewHolder.subTextView.setText(activity.getActivityTitle());
                squareButtonViewHolder.subTextView.setTextColor(context.getResources().getColor(cardDisplayData.subTextColor));
                squareButtonViewHolder.subTextView.setBackgroundColor(activity.getResources().getColor(cardDisplayData.subTextBgColor));
            }
        };
    }

    private static View.OnClickListener getReportButtonListener(final StandardHomeActivity activity) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reportButtonClick(GoogleAnalyticsFields.LABEL_REPORT_BUTTON);
                Intent i = new Intent(activity, ReportProblemActivity.class);
                activity.startActivity(i);
            }
        };
    }

    private static void reportButtonClick(String buttonLabel) {
        GoogleAnalyticsUtils.reportHomeButtonClick(buttonLabel);
    }

    public interface TextSetter {
        /**
         * Set view holder's text and subtext either from provided display
         * data, notification text argument, or auxiliary computations
         *
         * @param notificationText Optional text which will always be used when provided
         */
        void update(HomeCardDisplayData cardDisplayData,
                    SquareButtonViewHolder squareButtonViewHolder,
                    Context context,
                    String notificationText);
    }
}
