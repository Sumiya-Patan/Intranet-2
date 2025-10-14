package com.intranet.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.intranet.service.TimeSheetService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/timesheet")
public class TimeSheetController {

    @Autowired
    private TimeSheetService timeSheetService;


    @PostMapping("/submit")
    public String postMethodName(@RequestBody Timesheet timesheet) {
        return timeSheetService.submitTimeSheet(timeSheet);
        
      
    }
    

}
