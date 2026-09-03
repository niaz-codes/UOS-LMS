package com.example.uos_lms.core.data.remote.api;

import com.example.uos_lms.core.data.remote.api.dto.ConversationEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.ConversationsEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.FindOrCreateConversationRequestDto;
import com.example.uos_lms.core.data.remote.api.dto.MessageEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.MessagesEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.MyMessagesEnvelopeDto;
import com.example.uos_lms.core.data.remote.api.dto.SendMessageRequestDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MessagingApi {

    @POST("conversations")
    Call<ConversationEnvelopeDto> findOrCreate(@Body FindOrCreateConversationRequestDto request);

    @GET("conversations")
    Call<ConversationsEnvelopeDto> listConversations();

    @POST("conversations/{id}/messages")
    Call<MessageEnvelopeDto> sendMessage(@Path("id") String conversationId, @Body SendMessageRequestDto request);

    /** since is optional (null omits the query param) - poll-for-new-messages while the
     * thread screen is open, see messagingController.listMessages. */
    @GET("conversations/{id}/messages")
    Call<MessagesEnvelopeDto> listMessages(@Path("id") String conversationId, @Query("since") String sinceIso);

    @POST("conversations/{id}/read")
    Call<ConversationEnvelopeDto> markRead(@Path("id") String conversationId);

    /** Every message ever addressed to the caller, newest first, capped at 50 server-side -
     * consumed by AppNotificationCenter (see messagingController.listMyMessages). */
    @GET("messages/mine")
    Call<MyMessagesEnvelopeDto> listMyMessages(@Query("since") String sinceIso);
}
