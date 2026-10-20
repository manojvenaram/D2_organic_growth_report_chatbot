package com.example.excelviz.controller;

import com.example.excelviz.service.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides metadata for populating the UI dropdowns.
 * Sequence: Owning Site → Creation Profile → GMP Group → GMP Subgroup → GMP Artifact
 */
@RestController
@RequestMapping("/api/meta")
public class MetadataController {

    @Autowired
    private MetadataService metadataService;

    /** Step 1 — all owning sites in the loaded Excel */
    @GetMapping("/owning-sites")
    public Set<String> getOwningSites() {
        return metadataService.getOwningSites();
    }

    /** Step 2 — creation profiles for a given site */
    @GetMapping("/creation-profiles")
    public Set<String> getCreationProfiles(@RequestParam String owningSite) {
        return metadataService.getCreationProfilesBySite(owningSite);
    }

    /** Step 3 — GMP groups for a given site + profile */
    @GetMapping("/gmp-groups")
    public Set<String> getGmpGroups(
            @RequestParam String owningSite,
            @RequestParam String creationProfile) {
        return metadataService.getGmpGroupsBySiteAndProfile(owningSite, creationProfile);
    }

    /** Step 4 — GMP subgroups grouped by GMP Group name */
    @GetMapping("/gmp-subgroups")
    public Map<String, Set<String>> getGmpSubgroups(
            @RequestParam String owningSite,
            @RequestParam(required = false) List<String> creationProfiles,
            @RequestParam(required = false) List<String> gmpGroups) {
        return metadataService.getGmpSubgroupsGroupedByGmpGroup(owningSite, creationProfiles, gmpGroups);
    }

    /** Step 5 — GMP artifacts grouped by GMP Subgroup or GMP Group name */
    @GetMapping("/gmp-artifacts-grouped")
    public Map<String, Set<String>> getGmpArtifactsGrouped(
            @RequestParam String owningSite,
            @RequestParam(required = false) List<String> creationProfiles,
            @RequestParam(required = false) List<String> gmpGroups,
            @RequestParam(required = false) List<String> gmpSubgroups) {
        return metadataService.getGmpArtifactsGrouped(owningSite, creationProfiles, gmpGroups, gmpSubgroups);
    }

    /** All available years across the loaded data */
    @GetMapping("/years")
    public Set<Integer> getYears() {
        return metadataService.getAvailableYears();
    }
}
