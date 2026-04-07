package com.thuexe.thuexetulai.controller;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
import java.util.*;
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
        // ====== THÊM QR PAYMENT ======
        String paymentCode = "COC-" + System.currentTimeMillis();
        booking.setPaymentCode(paymentCode);


        if (depositPercent != null && (depositPercent == 20 || depositPercent == 50 || depositPercent == 70)) {
            booking.setDepositPercent(depositPercent);
        }
        if (depositMethod != null && ("CASH".equalsIgnoreCase(depositMethod) || "TRANSFER".equalsIgnoreCase(depositMethod))) {
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

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        List<BookingHistoryView> bookings = bookingRepository.findAll().stream()
                .filter(b -> b.getUserId().equals(user.getId()))
                .sorted(Comparator.comparing(Booking::getId).reversed())
                .map(b -> new BookingHistoryView(b, carRepository.findById(b.getCarId()).orElse(null)))
                .toList();

        model.addAttribute("bookings", bookings);
        return "profile";
    }
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
                String cleanName = file.getOriginalFilename()
                        .replaceAll("[^a-zA-Z0-9\\.\\-]", "_");

                String fileName = System.currentTimeMillis() + "_" + cleanName;

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

    @GetMapping("/pay-damage-vnpay/{id}")
    public String payVNPay(@PathVariable Long id) throws Exception {

        Booking b = bookingRepository.findById(id).orElseThrow();

        long amount = b.getDamageFee().longValue() * 100;

        String vnp_TxnRef = String.valueOf(System.currentTimeMillis());

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");

        String vnp_CreateDate = formatter.format(cal.getTime());

        cal.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cal.getTime());

        Map<String, String> vnp_Params = new HashMap<>();

        vnp_Params.put("vnp_Version", "2.1.0");
        vnp_Params.put("vnp_Command", "pay");
        vnp_Params.put("vnp_TmnCode", VNPayConfig.vnp_TmnCode);

        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");

        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan den bu booking " + id);
        vnp_Params.put("vnp_OrderType", "other");

        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", VNPayConfig.vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", "127.0.0.1");

        // 🔥 QUAN TRỌNG
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (String field : fieldNames) {
            String value = URLEncoder.encode(vnp_Params.get(field), StandardCharsets.US_ASCII.toString());

            hashData.append(field).append("=").append(value).append("&");
            query.append(field).append("=").append(value).append("&");
        }

        hashData.deleteCharAt(hashData.length() - 1);
        query.deleteCharAt(query.length() - 1);

        String secureHash = VNPayConfig.hmacSHA512(VNPayConfig.vnp_HashSecret, hashData.toString());

        query.append("&vnp_SecureHash=").append(secureHash);

        return "redirect:" + VNPayConfig.vnp_Url + "?" + query.toString();
    }

    @GetMapping("/vnpay-return")
    public String vnpayReturn(HttpServletRequest request) {

        String status = request.getParameter("vnp_ResponseCode");
        String orderInfo = request.getParameter("vnp_OrderInfo");

        if ("00".equals(status)) {

            // lấy id từ chuỗi "Thanh toan den bu booking 30"
            String[] arr = orderInfo.split(" ");
            Long bookingId = Long.parseLong(arr[arr.length - 1]);

            Booking b = bookingRepository.findById(bookingId).orElseThrow();
            b.setDamagePaid(true);

            bookingRepository.save(b);
        }

        return "redirect:/booking/history";
    }

    @GetMapping("/confirm-paid/{id}")
    public String confirmPaid(@PathVariable Long id) {

        Booking b = bookingRepository.findById(id).orElseThrow();

        // ✅ đánh dấu đã thanh toán
        b.setDamagePaid(true);

        // ✅ (QUAN TRỌNG) update luôn trạng thái
        b.setStatus("PAID");

        bookingRepository.save(b);

        return "redirect:/booking/history";
    }

}