package com.stitch.story.backend.controller;

import com.stitch.story.backend.dtos.VendorApplicationDTO;
import com.stitch.story.backend.dtos.VendorApplyRequest;
import com.stitch.story.backend.dtos.VendorReviewRequest;
import com.stitch.story.backend.services.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @PostMapping(value = "/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VendorApplicationDTO> apply(
            @ModelAttribute VendorApplyRequest request,
            @RequestParam("proof") MultipartFile proof
    ) {
        return new ResponseEntity<>(vendorService.apply(request, proof), HttpStatus.CREATED);
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

    @GetMapping("/applications/{id}/document")
    public ResponseEntity<Resource> document(@PathVariable Long id) {
        VendorService.ProofDocument document = vendorService.loadProofDocument(id);
        String contentType = document.contentType() == null || document.contentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : document.contentType();
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(document.originalName() == null ? "document" : document.originalName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType(contentType))
                .body(document.resource());
    }
}
