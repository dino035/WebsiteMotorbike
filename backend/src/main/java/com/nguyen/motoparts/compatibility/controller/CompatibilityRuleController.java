package com.nguyen.motoparts.compatibility.controller;

import com.nguyen.motoparts.compatibility.dto.CreateRulesRequest;
import com.nguyen.motoparts.compatibility.dto.CreateRulesResult;
import com.nguyen.motoparts.compatibility.dto.RuleRow;
import com.nguyen.motoparts.compatibility.service.CompatibilityRuleService;
import com.nguyen.motoparts.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/admin/compatibility-rules")
@RequiredArgsConstructor
public class CompatibilityRuleController {

    private final CompatibilityRuleService ruleService;

    @PostMapping
    public CreateRulesResult create(@Valid @RequestBody CreateRulesRequest req,
                                    @AuthenticationPrincipal AuthUser admin) {
        return ruleService.createRules(req, admin.accountId());
    }

    @GetMapping
    public List<RuleRow> list(@RequestParam String sparePartId) {
        return ruleService.listRules(sparePartId);
    }

    @DeleteMapping("/{ruleId}")
    public void delete(@PathVariable String ruleId) {
        ruleService.deleteRule(ruleId);
    }
}