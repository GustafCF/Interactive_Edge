// package com.br.elohostel.controller;

// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import com.br.elohostel.service.ICalExportService;

// @RestController
// @RequestMapping("/api/debug")
// public class DebugController {

//     private final ICalExportService icalExportService;

//     public DebugController(ICalExportService icalExportService) {
//         this.icalExportService = icalExportService;
//     }

//     @GetMapping("/ical/{propertyId}")
//     public ResponseEntity<String> debugICal(@PathVariable String propertyId) {
//         try {
//             icalExportService.debugICalContent(propertyId);
//             return ResponseEntity.ok("Debug realizado - verifique os logs do servidor");
//         } catch (Exception e) {
//             return ResponseEntity.badRequest().body("Erro no debug: " + e.getMessage());
//         }
//     }

//     @GetMapping("/ical-content/{propertyId}")
//     public ResponseEntity<String> getICalContent(@PathVariable String propertyId) {
//         try {
//             String icalContent = icalExportService.generateICalContent(propertyId);
//             return ResponseEntity.ok()
//                     .header("Content-Type", "text/plain")
//                     .body(icalContent);
//         } catch (Exception e) {
//             return ResponseEntity.badRequest().body("Erro: " + e.getMessage());
//         }
//     }
// }