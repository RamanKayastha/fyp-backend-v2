package com.stitch.story.backend.services;

import com.stitch.story.backend.dtos.VendorApplicationDTO;
import com.stitch.story.backend.dtos.VendorApplyRequest;
import com.stitch.story.backend.dtos.VendorReviewRequest;

import java.util.List;

public interface VendorService {
    VendorApplicationDTO apply(VendorApplyRequest request);

    VendorApplicationDTO myApplication();

    List<VendorApplicationDTO> listApplications();

    VendorApplicationDTO review(Long id, VendorReviewRequest request);
}
