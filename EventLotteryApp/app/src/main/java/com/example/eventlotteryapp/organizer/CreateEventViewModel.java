package com.example.eventlotteryapp.organizer;

import android.net.Uri;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.Date;

/**
 * ViewModel for the event creation process.
 * Stores all event data entered by the organizer across multiple steps using LiveData.
 * This allows data to persist across configuration changes and be shared between fragments.
 * 
 * @author Droids Team
 */
public class CreateEventViewModel extends ViewModel {
    public final MutableLiveData<String> title = new MutableLiveData<>();
    public final MutableLiveData<String> description = new MutableLiveData<>();
    public final MutableLiveData<String> location = new MutableLiveData<>();
    public final MutableLiveData<Double> price = new MutableLiveData<>(0.0);
    public final MutableLiveData<Date> eventStartDate = new MutableLiveData<>();
    public final MutableLiveData<Date> eventEndDate = new MutableLiveData<>();
    public final MutableLiveData<Date> registrationOpenDate = new MutableLiveData<>();
    public final MutableLiveData<Date> registrationCloseDate = new MutableLiveData<>();
    public final MutableLiveData<Integer> maxParticipants = new MutableLiveData<>(0);
    public final MutableLiveData<Integer> waitingListSize = new MutableLiveData<>(0);
    public final MutableLiveData<Boolean> requireGeolocation = new MutableLiveData<>(false);
    public final MutableLiveData<Boolean> limitWaitingList = new MutableLiveData<>(false);
    public final MutableLiveData<Uri> posterImageUri = new MutableLiveData<>();
    public final MutableLiveData<String> posterImageBase64 = new MutableLiveData<>();
}
