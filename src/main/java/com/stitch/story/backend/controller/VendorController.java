package com.stitch.story.backend.controller;

import com.stitch.story.backend.dtos.VendorApplicationDTO;
import com.stitch.story.backend.dtos.VendorApplyRequest;
import com.stitch.story.backend.dtos.VendorReviewRequest;
import com.stitch.story.backend.services.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @PostMapping("/apply")
    public ResponseEntity<VendorApplicationDTO> apply(@RequestBody VendorApplyRequest request) {
        return new ResponseEntity<>(vendorService.apply(request), HttpStatus.CREATED);
    }

    @GetMapping("/me")
    public ResponseEntity<VendorApplicationDTO> myApplication() {
        return ResponseEntity.ok(vendorService.myApplication());
    }

    @GetMapping("/applications")
    public ResponseEntity<List<VendorApplicationDTO>> listApplications() {
        return ResponseEntity.ok(vendorService.listApplications());
    }

    @PutMapping("/applications/{id}/review")
    public ResponseEntity<VendorApplicationDTO> review(
            @PathVariable Long id,
            @RequestBody VendorReviewRequest request
    ) {
        return ResponseEntity.ok(vendorService.review(id, request));
    }
}
