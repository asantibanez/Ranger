package com.andressantibanez.ranger;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.LocalDateTime;

public class Ranger extends HorizontalScrollView implements View.OnClickListener {

    public final static String TAG = Ranger.class.getSimpleName();


    /**
     * Constants
     */
    //Layouts
    private final int WIDGET_LAYOUT_RES_ID = R.layout.ranger_layout;
    private final int DAY_VIEW_LAYOUT_RES_ID = R.layout.day_layout;
    //Resource ids
    public static final int DAYS_CONTAINER_RES_ID = R.id.days_container;
    public static final int DAY_OF_WEEK_RES_ID = R.id.day_of_week;
    public static final int DAY_NUMBER_RES_ID = R.id.day_number;
    public static final int MONTH_NAME_RES_ID = R.id.month_short_name;


    /**
     * Variables
     */
    //State
    Context mContext;
    LocalDateTime mStartDate;
    LocalDateTime mEndDate;
    int mSelectedDay;

    //Colors
    int mDayTextColor;
    int mSelectedDayTextColor;
    int mDaysContainerBackgroundColor;
    int mSelectedDayBackgroundColor;

    //Titles
    boolean mAlwaysDisplayMonth;
    boolean mDisplayDayOfWeek;

    //Listener
    DayViewOnClickListener mListener;
    public void setDayViewOnClickListener(DayViewOnClickListener listener) {
        mListener = listener;
    }
    public interface DayViewOnClickListener {
        public void onDaySelected(int day);
    }

    //Day View
    DayView mSelectedDayView;

    /**
     * Controls
     */
    Space mLeftSpace;
    LinearLayout mDaysContainer;
    Space mRightSpace;


    /**
     * Constructors
     */
    public Ranger(Context context) {
        super(context);
        init(context, null);
    }

    public Ranger(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public Ranger(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }


    /**
     * Initialization
     */
    public void init(Context context, AttributeSet attributeSet) {
        mContext = context;

        //Init JodaTime
        JodaTimeAndroid.init(context);

        //Init Start and End date with current month
        final LocalDateTime currentDateTime = new LocalDateTime();
        setStartDateWithParts(currentDateTime.getYear(), currentDateTime.getMonthOfYear(), currentDateTime.dayOfMonth().withMinimumValue().getDayOfMonth());
        setEndDateWithParts(currentDateTime.getYear(), currentDateTime.getMonthOfYear(), currentDateTime.dayOfMonth().withMaximumValue().getDayOfMonth());

        //Inflate view
        View view = LayoutInflater.from(mContext).inflate(WIDGET_LAYOUT_RES_ID, this, true);

        //Get controls
        mDaysContainer = (LinearLayout) view.findViewById(DAYS_CONTAINER_RES_ID);

        //Get custom attributes
        mDisplayDayOfWeek = true;
        if(attributeSet != null) {
            TypedArray a = mContext.getTheme().obtainStyledAttributes(attributeSet, R.styleable.Ranger, 0, 0);

            try {

                //Colors
                mDayTextColor = a.getColor(R.styleable.Ranger_dayTextColor, getColor(R.color.default_day_text_color));
                mSelectedDayTextColor = a.getColor(R.styleable.Ranger_selectedDayTextColor, getColor(R.color.default_selected_day_text_color));

                mDaysContainerBackgroundColor = a.getColor(R.styleable.Ranger_daysContainerBackgroundColor, getColor(R.color.default_days_container_background_color));
                mSelectedDayBackgroundColor = a.getColor(R.styleable.Ranger_selectedDayBackgroundColor, getColor(R.color.default_selected_day_background_color));

                //Labels
                mAlwaysDisplayMonth = a.getBoolean(R.styleable.Ranger_alwaysDisplayMonth, false);
                mDisplayDayOfWeek = a.getBoolean(R.styleable.Ranger_displayDayOfWeek, true);

            } finally {
                a.recycle();
            }
        }

        //Setup styling
        //Days Container
        mDaysContainer.setBackgroundColor(mDaysContainerBackgroundColor);

        //Render control
        render();

        //Set Selection. Default is today. Must post delayed
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setSelectedDay(currentDateTime.getDayOfMonth());
            }
        }, 300);
    }


    /***
     * State modification
     */
    public void setStartAndEndDateWithParts(int startYear, int startMonth, int startDay, int endYear, int endMonth, int endDay) {
        setStartDateWithParts(startYear, startMonth, startDay);
        setEndDateWithParts(endYear, endMonth, endDay);

        render();
    }

    private void setStartDateWithParts(int year, int month, int day) {
        mStartDate = new LocalDateTime(year, month, day, 0, 0, 0);
    }

    private void setEndDateWithParts(int year, int month, int day) {
        mEndDate = new LocalDateTime(year, month, day, 0, 0, 0);
    }

    public void setSelectedDay(int day) {
        //Deselect day selected
        if(mSelectedDay > 0)
            unSelectDay(mSelectedDay);

        //Set selected day
        mSelectedDay = day;
        selectDay(mSelectedDay);

        //Scroll to DayView
        scrollToDayView(mSelectedDayView);

        //Call listener
        if(mListener != null)
            mListener.onDaySelected(mSelectedDay);
    }

    /**
     * Ui
     */
    private int getColor(int colorResId) {
        return getResources().getColor(colorResId);
    }

    private void render() {
        //Get inflater for view
        LayoutInflater inflater = LayoutInflater.from(mContext);

        //Add left padding
        mLeftSpace = new Space(mContext);
        mDaysContainer.addView(mLeftSpace);

        //Cycle from start day
        LocalDateTime startDate = mStartDate;
        LocalDateTime endDate = mEndDate;

        while (startDate.isBefore(endDate.plusDays(1))) {

            //Inflate view
            LinearLayout view = (LinearLayout) inflater.inflate(DAY_VIEW_LAYOUT_RES_ID, mDaysContainer, false);

            //new DayView
            DayView dayView = new DayView(view);

            //Set texts and listener
            dayView.setDayOfWeek(startDate.dayOfWeek().getAsShortText().substring(0,3));
            if(!mDisplayDayOfWeek)
                dayView.hideDayOfWeek();

            dayView.setDay(startDate.getDayOfMonth());
            dayView.setMonthShortName(startDate.monthOfYear().getAsShortText().substring(0,3));

            //Hide month if range in same month
            if (!mAlwaysDisplayMonth && startDate.getMonthOfYear() == endDate.getMonthOfYear())
                dayView.hideMonthShortName();

            //Set style
            dayView.setTextColor(mDayTextColor);

            //Set listener
            dayView.setOnClickListener(this);

            //Add to container
            mDaysContainer.addView(dayView.getView());

            //Next day
            startDate = startDate.plusDays(1);
        }

        //Add right padding
        mRightSpace = new Space(mContext);
        mDaysContainer.addView(mRightSpace);
    }

    public void unSelectDay(int day) {
        for (int i = 1; i < mDaysContainer.getChildCount() - 1; i++) {
            DayView dayView = new DayView(mDaysContainer.getChildAt(i));
            if(dayView.getDay() == day) {
                dayView.setTextColor(mDayTextColor);
                dayView.setBackgroundColor(0);
                return;
            }
        }
    }

    public void selectDay(int day) {
        for (int i = 1; i < mDaysContainer.getChildCount() - 1; i++) {
            DayView dayView = new DayView(mDaysContainer.getChildAt(i));
            if(dayView.getDay() == day) {

                dayView.setTextColor(mSelectedDayTextColor);
                dayView.setBackgroundColor(mSelectedDayBackgroundColor);

                mSelectedDayView = dayView;

                return;
            }
        }
    }

    public void scrollToDayView(DayView dayView) {
        int x = dayView.getView().getLeft();
        int y = dayView.getView().getTop();
        smoothScrollTo(x - mLeftSpace.getLayoutParams().width, y);
    }


    /**
     * On DayView click listener
     */
    @Override
    public void onClick(View view) {
        //Get day view
        DayView dayView = new DayView(view);

        //Get selected day and set selection
        int selectedDay = dayView.getDay();
        setSelectedDay(selectedDay);
    }


    /**
     * Custom implementation for left and right spaces
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if(changed) {
            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40, getResources().getDisplayMetrics());

            LinearLayout.LayoutParams leftParams = (LinearLayout.LayoutParams) mLeftSpace.getLayoutParams();
            leftParams.width = getWidth() / 2 - padding;
            mLeftSpace.setLayoutParams(leftParams);

            LinearLayout.LayoutParams rightParams = (LinearLayout.LayoutParams) mRightSpace.getLayoutParams();
            rightParams.width = getWidth() / 2 - padding;
            mRightSpace.setLayoutParams(rightParams);
        }
    }


    /**
     * Configuration change handling
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();

        SavedState savedState = new SavedState(superState);
        savedState.setSelectedDay(mSelectedDay);
        savedState.setStartDateMillis(mStartDate.getMillisOfDay());
        savedState.setEndDateMillis(mStartDate.getMillisOfDay());

        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        mSelectedDay = savedState.getSelectedDay();
    }

    protected static class SavedState extends BaseSavedState {
        int mSelectedDay;
        int mStartDateMillisOfDay;
        int mEndDateMillisOfDay;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel in) {
            super(in);
            mSelectedDay = in.readInt();
            mStartDateMillisOfDay = in.readInt();
            mEndDateMillisOfDay = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mSelectedDay);
            out.writeInt(mStartDateMillisOfDay);
            out.writeInt(mEndDateMillisOfDay);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        public void setSelectedDay(int selectedDay) {
            mSelectedDay = selectedDay;
        }

        public void setStartDateMillis(int startDateMillis) {
            mStartDateMillisOfDay = startDateMillis;
        }

        public void setEndDateMillis(int endDateMillis) {
            mEndDateMillisOfDay = endDateMillis;
        }

        public int getSelectedDay() {
            return mSelectedDay;
        }
    }


    /**
     * DateView class
     */
    public static class DayView {

        int mDay;

        LinearLayout mView;
        TextView mDayOfWeek;
        TextView mDayNumber;
        TextView mMonthShortName;

        public DayView(View view) {
            mView = (LinearLayout) view;

            mDayOfWeek = (TextView) mView.findViewById(DAY_OF_WEEK_RES_ID);
            mDayNumber = (TextView) mView.findViewById(DAY_NUMBER_RES_ID);
            mMonthShortName = (TextView) mView.findViewById(MONTH_NAME_RES_ID);
        }

        public int getDay() {
            return Integer.parseInt(mDayNumber.getText().toString());
        }

        public void setDay(int day) {
            mDay = day;
            setDayNumber(String.format("%02d", day));
        }

        public void setDayOfWeek(String dayOfWeek) {
            mDayOfWeek.setText(dayOfWeek);
        }

        public void setDayNumber(String dayNumber) {
            mDayNumber.setText(dayNumber);
        }

        public void setMonthShortName(String monthShortName) {
            mMonthShortName.setText(monthShortName);
        }

        public void setBackgroundColor(int color) {
            mView.setBackgroundColor(color);
        }

        public void setTextColor(int color) {
            mDayOfWeek.setTextColor(color);
            mDayNumber.setTextColor(color);
            mMonthShortName.setTextColor(color);
        }

        public void setOnClickListener(OnClickListener listener) {
            mView.setOnClickListener(listener);
        }

        public View getView() {
            return mView;
        }

        public void hideDayOfWeek() {
            mDayOfWeek.setVisibility(View.GONE);
        }

        public void hideMonthShortName() {
            mMonthShortName.setVisibility(View.GONE);
        }

    }
}
