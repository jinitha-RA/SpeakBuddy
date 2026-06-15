package lk.tmjnr.speakbuddy.data.remote;

import lk.tmjnr.speakbuddy.data.remote.dto.ChatRequest;
import lk.tmjnr.speakbuddy.data.remote.dto.ChatResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Retrofit API interface for the SpeakBuddy backend.
 */
public interface ApiService {

    @POST("default/SpeakBuddyAPI")
    Call<ChatResponse> sendMessage(@Body ChatRequest request);
}
