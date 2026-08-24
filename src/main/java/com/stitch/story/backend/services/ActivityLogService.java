package com.stitch.story.backend.services;

import com.stitch.story.backend.dtos.ActivityLogDTO;
import com.stitch.story.backend.entities.enums.ActivityAction;
import com.stitch.story.backend.entities.enums.ActivityEntityType;

import java.util.List;

public interface ActivityLogService {
    void record(ActivityAction action, ActivityEntityType entityType, Long entityId, String description);

    List<ActivityLogDTO> getAll();
}
