package com.eris.servicehub.services.ordernote;

import com.eris.servicehub.dtos.ordernote.OrderNoteRequest;
import com.eris.servicehub.dtos.ordernote.OrderNoteResponse;
import java.util.List;
import java.util.UUID;

public interface OrderNoteService {
    List<OrderNoteResponse> getNotesForOrder(UUID orderId);
    OrderNoteResponse createNoteForOrder(UUID orderId, OrderNoteRequest request);
}