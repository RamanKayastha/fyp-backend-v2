package com.stitch.story.backend.controller;

import com.stitch.story.backend.dtos.ActivityLogDTO;
import com.stitch.story.backend.services.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/activity-logs")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @GetMapping
    public ResponseEntity<List<ActivityLogDTO>> getAll() {
        return ResponseEntity.ok(activityLogService.getAll());
    }
}
