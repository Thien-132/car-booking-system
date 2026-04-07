package com.thuexe.thuexetulai.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.thuexe.thuexetulai.dto.CompletedBookingView;
import com.thuexe.thuexetulai.model.Booking;
import com.thuexe.thuexetulai.model.Car;
import com.thuexe.thuexetulai.model.User;
import com.thuexe.thuexetulai.repository.BookingRepository;
import com.thuexe.thuexetulai.repository.CarRepository;
import com.thuexe.thuexetulai.repository.UserRepository;
import com.thuexe.thuexetulai.service.BookingService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AdminController {



    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private UserRepository userRepository;

    private static final List<String> ACTIVE_BOOKING_STATUSES = List.of("PENDING", "APPROVED", "PAID");

    // 👉 CHECK ADMIN (tái sử dụng)
    private boolean isAdmin(HttpSession session) {
        User user = (User) session.getAttribute("user");
        return user != null && "ADMIN".equals(user.getRole());
    }


    // 👉 TRANG ADMIN
    @GetMapping("/admin")
    public String adminPage(HttpSession session){

        if(!isAdmin(session)){
            return "redirect:/login";
        }

        return "admin";
    }

    // 👉 DANH SÁCH BOOKING
    @GetMapping("/admin/bookings")
    public String bookings(HttpSession session, Model model){

        User user = (User) session.getAttribute("user");

        if(user == null){
            return "redirect:/login";
        }

        if(!"ADMIN".equals(user.getRole())){
            return "redirect:/";
        }

        model.addAttribute("bookings", bookingRepository.findAll());

        return "admin/admin-bookings";
    }

    // 👉 DUYỆT ĐƠN
    @GetMapping("/admin/bookings/approve/{id}")
    public String approve(@PathVariable Long id, HttpSession session){

        if(!isAdmin(session)){
            return "redirect:/login";
        }

        Booking b = bookingRepository.findById(id).orElse(null);

        if(b != null){
            b.setStatus("APPROVED");
            bookingRepository.save(b);
        }

        return "redirect:/admin/bookings";
    }

    // 👉 (OPTIONAL) TỪ CHỐI
    @GetMapping("/admin/bookings/reject/{id}")
    public String reject(@PathVariable Long id, HttpSession session){

        if(!isAdmin(session)){
            return "redirect:/login";
        }

        Booking b = bookingRepository.findById(id).orElse(null);

        if(b != null){
            b.setStatus("REJECTED");
            bookingRepository.save(b);
        }

        return "redirect:/admin/bookings";
    }

    // 👉 QUẢN LÝ XE ĐANG THUÊ
    @GetMapping("/admin/car-rentals")
    public String carRentals(HttpSession session, Model model){

        if(!isAdmin(session)){
            return "redirect:/login";
        }

        LocalDate today = LocalDate.now();
        
        // Lấy danh sách xe đang được thuê (có booking active)
        List<Long> bookedCarIds = bookingRepository.findDistinctCarIdsWithActiveBooking(ACTIVE_BOOKING_STATUSES, today);
        
        List<Car> rentedCars = carRepository.findAll().stream()
                .filter(c -> bookedCarIds.contains(c.getId()))
                .toList();
        
        // Lấy danh sách xe đang rảnh
        List<Car> availableCars = carRepository.findAll().stream()
                .filter(c -> !bookedCarIds.contains(c.getId()))
                .toList();
        
        // Lấy danh sách booking đã hoàn thành (chờ admin duyệt)
        List<CompletedBookingView> completedBookings = bookingRepository.findAll().stream()
                .filter(b -> "COMPLETED".equals(b.getStatus()))
                .map(b -> new CompletedBookingView(
                        b,
                        carRepository.findById(b.getCarId()).orElse(null),
                        userRepository.findById(b.getUserId()).orElse(null)
                ))
                .toList();
        
        model.addAttribute("rentedCars", rentedCars);
        model.addAttribute("availableCars", availableCars);
        model.addAttribute("completedBookings", completedBookings);

        return "admin/admin-car-rentals";
    }

    // 👉 PHÊ DUYỆT HOÀN THÀNH XE
    @GetMapping("/admin/verify-car-return/{bookingId}")
    public String verifyCarReturn(@PathVariable Long bookingId, HttpSession session){

        if(!isAdmin(session)){
            return "redirect:/login";
        }

        Booking booking = bookingRepository.findById(bookingId).orElse(null);

        if(booking != null && "COMPLETED".equals(booking.getStatus())){
            // Xóa booking để hoàn toàn quay lại available
            bookingRepository.deleteById(bookingId);
        }

        return "redirect:/admin/car-rentals";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {

        model.addAttribute("totalRevenue", bookingService.getTotalRevenue());
        model.addAttribute("totalBookings", bookingService.getTotalCompletedBookings());
        model.addAttribute("revenueByDate", bookingService.getRevenueByDate());
        model.addAttribute("topCars", bookingService.getTopCars(carRepository));
        return "admin/dashboard";
    }

    // 👉 TEST: TẠO BOOKING COMPLETED MẪU (để test admin panel)
    @GetMapping("/admin/test-create-completed-booking")
    public String testCreateCompletedBooking(HttpSession session){

        if(!isAdmin(session)){
            return "redirect:/login";
        }

        // Tìm booking PAID đầu tiên để convert thành COMPLETED
        Booking booking = bookingRepository.findAll().stream()
                .filter(b -> "PAID".equals(b.getStatus()))
                .findFirst()
                .orElse(null);

        if(booking != null){
            booking.setStatus("COMPLETED");
            bookingRepository.save(booking);
        }

        return "redirect:/admin/car-rentals";
    }

    @GetMapping("/admin/check-ok/{id}")
    public String checkOk(@PathVariable Long id) {
        Booking b = bookingRepository.findById(id).orElseThrow();
        b.setDamageStatus("OK");
        b.setDamageFee(0.0);
        bookingRepository.save(b);
        return "redirect:/admin/bookings";
    }

    @GetMapping("/admin/check-damage/{id}")
    public String checkDamage(@PathVariable Long id) {
        Booking b = bookingRepository.findById(id).orElseThrow();
        b.setDamageStatus("DAMAGED");
        b.setDamageFee(500000.0); // tiền đền
        bookingRepository.save(b);
        return "redirect:/admin/bookings";
    }
}