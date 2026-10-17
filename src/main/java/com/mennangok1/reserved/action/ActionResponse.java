package com.mennangok1.reserved.action;

public record ActionResponse(
        Long id,
        String name,
        String description
) {

    static ActionResponse from(Action action) {
        return new ActionResponse(action.getId(), action.getName(), action.getDescription());
    }

}
