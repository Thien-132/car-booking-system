package com.thuexe.thuexetulai.controller;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.thuexe.thuexetulai.dto.BookingHistoryView;
import com.thuexe.thuexetulai.model.Booking;
import com.thuexe.thuexetulai.model.User;
import com.thuexe.thuexetulai.repository.BookingRepository;
import com.thuexe.thuexetulai.repository.CarRepository;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.*;
import com.thuexe.thuexetulai.config.VNPayConfig;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

@Controller
public class BookingController {

    private static final List<String> ACTIVE_BOOKING_STATUSES = List.of("PENDING", "APPROVED", "PAID");

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CarRepository carRepository;

    @GetMapping("/booking-success")
    public String success(){
        return "booking-success";
    }

    @GetMapping("/booking/{carId}")
    public String bookCar(
            @PathVariable Long carId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Integer depositPercent,
            @RequestParam(required = false) String depositMethod,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        if (bookingRepository.existsByCarIdAndStatusInAndEndDateGreaterThanEqual(
                carId, ACTIVE_BOOKING_STATUSES, LocalDate.now())) {
            return "redirect:/car/" + carId;
        }

        Booking booking = new Booking();
        booking.setCarId(carId);
        booking.setUserId(user.getId());

        if (startDate != null && !startDate.isBlank()) {
            try { booking.setStartDate(LocalDate.parse(startDate)); } catch (Exception ignored) {}
        }
        if (endDate != null && !endDate.isBlank()) {
            try { booking.setEndDate(LocalDate.parse(endDate)); } catch (Exception ignored) {}
        }

        if (booking.getStartDate() == null) booking.setStartDate(LocalDate.now());
        if (booking.getEndDate() == null) booking.setEndDate(booking.getStartDate().plusDays(1));

        booking.setStatus("PENDING");

        String paymentCode = "COC-" + System.currentTimeMillis();
        booking.setPaymentCode(paymentCode);

        if (depositPercent != null) {
            booking.setDepositPercent(depositPercent);
        }
        if (depositMethod != null) {
            booking.setDepositMethod(depositMethod.toUpperCase());
        }

        bookingRepository.save(booking);

        return "redirect:/booking-success";
    }

    @GetMapping("/payment/{id}")
    public String payment(@PathVariable Long id){
        Booking b = bookingRepository.findById(id).orElse(null);
        if(b != null){
            b.setStatus("PAID");
            bookingRepository.save(b);
        }
        return "redirect:/booking/history";
    }

    @GetMapping("/complete-booking/{id}")
    public String completeBooking(@PathVariable Long id, HttpSession session){

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        Booking b = bookingRepository.findById(id).orElse(null);

        if(b != null && b.getUserId().equals(user.getId())){
            b.setStatus("COMPLETED");
            bookingRepository.save(b);
        }

        return "redirect:/booking/history";
    }

    @GetMapping("/booking/history")
    public String history(HttpSession session, Model model) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<BookingHistoryView> rows = bookingRepository.findAll().stream()
                .filter(b -> b.getUserId().equals(user.getId()))
                .sorted(Comparator.comparing(Booking::getId).reversed())
                .map(b -> new BookingHistoryView(b, carRepository.findById(b.getCarId()).orElse(null)))
                .toList();

        model.addAttribute("rows", rows);

        return "history";
    }

    // ❌ ĐÃ XÓA /profile Ở ĐÂY

    @PostMapping("/return-car/{id}")
    public String returnCar(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile[] files,
            @RequestParam("note") String note
    ) throws IOException {

        Booking booking = bookingRepository.findById(id).orElseThrow();

        String uploadDir = System.getProperty("user.dir") + "/uploads/";
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        List<String> fileNames = new ArrayList<>();

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                file.transferTo(new File(uploadDir + fileName));
                fileNames.add(fileName);
            }
        }

        booking.setReturnImage(String.join(";", fileNames));
        booking.setReturnNote(note);

        bookingRepository.save(booking);

        return "redirect:/booking/history";
    }

    @GetMapping("/pay-damage/{id}")
    public String payDamage(@PathVariable Long id) {

        Booking b = bookingRepository.findById(id).orElseThrow();
        b.setDamagePaid(true);
        bookingRepository.save(b);

        return "redirect:/booking/history";
    }
}