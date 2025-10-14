package com.intranet.controller;

import com.intranet.service.WeekInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;


@CrossOrigin(origins = "*", allowedHeaders = "*")
@RestController
@RequestMapping("/api/weekinfo")
@RequiredArgsConstructor
public class WeekInfoController {

    private final WeekInfoService weekInfoService;

    @PostMapping("/generate/{year}/{month}")
    public String generateForGivenMonth(@PathVariable int year, @PathVariable int month) {
        weekInfoService.generateWeeksForMonth(year, month);
        return "WeekInfo generated for " + month + "/" + year;
    }
}
