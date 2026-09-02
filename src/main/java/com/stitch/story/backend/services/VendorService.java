package com.stitch.story.backend.services;

import com.stitch.story.backend.dtos.VendorApplicationDTO;
import com.stitch.story.backend.dtos.VendorApplyRequest;
import com.stitch.story.backend.dtos.VendorReviewRequest;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VendorService {
    VendorApplicationDTO apply(VendorApplyRequest request, MultipartFile proof);

    VendorApplicationDTO myApplication();

    List<VendorApplicationDTO> listApplications();

    VendorApplicationDTO review(Long id, VendorReviewRequest request);

    ProofDocument loadProofDocument(Long id);

    record ProofDocument(Resource resource, String originalName, String contentType) {
    }
}
