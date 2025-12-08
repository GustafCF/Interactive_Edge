package com.br.elohostel.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.br.elohostel.model.Bed;
import com.br.elohostel.model.Guest;
import com.br.elohostel.model.Reserve;
import com.br.elohostel.model.Role;
import com.br.elohostel.model.Room;
import com.br.elohostel.model.User;
import com.br.elohostel.model.enums.BedStatus;
import com.br.elohostel.model.enums.ReserveStatus;
import com.br.elohostel.model.enums.RoleStatus;
import com.br.elohostel.model.enums.RoomStatus;
import com.br.elohostel.model.enums.RoomType;
import com.br.elohostel.repository.BedRepository;
import com.br.elohostel.repository.GuestRepository;
import com.br.elohostel.repository.ReserveRepository;
import com.br.elohostel.repository.RoleRepository;
import com.br.elohostel.repository.RoomRepository;
import com.br.elohostel.repository.UserRepository;

@Configuration
@Profile("test")
public class TestConfig implements CommandLineRunner {

    private final GuestRepository idRepo;
    private final RoomRepository roomRepo;
    private final UserRepository userRepo;
    private final BCryptPasswordEncoder passwordEncode;
    private final ReserveRepository reserveRepo;
    private final BedRepository bedRepo;
    private final RoleRepository roleRepo;
    
    public TestConfig(GuestRepository idRepo, RoomRepository roomRepo, UserRepository userRepo, BCryptPasswordEncoder passwordEncode, ReserveRepository reserveRepo, BedRepository bedRepo, RoleRepository roleRepo) {
        this.idRepo = idRepo;
        this.roomRepo = roomRepo;
        this.userRepo = userRepo;
        this.passwordEncode = passwordEncode;
        this.reserveRepo = reserveRepo;
        this.bedRepo = bedRepo;
        this.roleRepo = roleRepo;
    }

    @Override
    public void run(String... args) throws Exception {

        // roleRepo.deleteAll();
        // userRepo.deleteAll();

        Role rl1 = new Role(RoleStatus.ADMIN.name(), "Administrador da API" , RoleStatus.ADMIN);

        User u1 = new User("gcf", "gcf", "gcf@gmail.com", passwordEncode.encode("12345"), "12345678");
        u1.getRoles().add(rl1);
        userRepo.save(u1);
        
        Guest i1 = new Guest("Gustavo", "04695595192", "61-9999-9999", "gus@email.com");
        Guest i2 = new Guest("Verônica", "12345678900", "61-9999-9999", "veve@email.com");
        Guest i3 = new Guest("Eloina", "33344455511", "61-9999-9999", "eloina@email.com");
        Guest i4 = new Guest("Deusimar", "33344455511", "61-9999-9999", "masim@email.com");
        Guest i5 = new Guest("Eloita", "33344455511", "61-9999-9999", "eloita@email.com");
        idRepo.saveAll(Arrays.asList(i1, i2, i3, i4, i5));

       
        Room r1 = new Room( 1, RoomStatus.VAGUE, RoomType.SUITE, BigDecimal.valueOf(150.0));
        Room r2 = new Room( 2, RoomStatus.VAGUE, RoomType.ROOM_SHARED_BATHROOM, BigDecimal.valueOf(99.0));
        Room r3 = new Room( 3, RoomStatus.VAGUE, RoomType.ROOM_SHARED_BATHROOM, BigDecimal.valueOf(99.0));
        Room r4 = new Room( 4, RoomStatus.VAGUE, RoomType.SUITE, BigDecimal.valueOf(150.0)); 
        Room r5 = new Room( 5, RoomStatus.VAGUE, RoomType.SHARED, BigDecimal.valueOf(59.0));
        Room r6 = new Room(6, RoomStatus.VAGUE, RoomType.SHARED, BigDecimal.valueOf(180.0));  
        Room r7 = new Room(7, RoomStatus.VAGUE, RoomType.SUITE, BigDecimal.valueOf(110.0));
        Room r8 = new Room(8, RoomStatus.VAGUE, RoomType.ROOM_SHARED_BATHROOM, BigDecimal.valueOf(110.0));
        Room r9 = new Room(9, RoomStatus.VAGUE, RoomType.ROOM_SHARED_BATHROOM, BigDecimal.valueOf(110.0));
        Room r10 = new Room(10, RoomStatus.VAGUE, RoomType.ROOM_SHARED_BATHROOM, BigDecimal.valueOf(110.0));
        Room r11 = new Room(11, RoomStatus.VAGUE, RoomType.ROOM_SHARED_BATHROOM, BigDecimal.valueOf(79.0));
        Room r12 = new Room(12, RoomStatus.VAGUE, RoomType.ROOM_SHARED_BATHROOM, BigDecimal.valueOf(89.0));
        Room r13 = new Room(13, RoomStatus.VAGUE, RoomType.STUDIO, BigDecimal.valueOf(160));
        Room r14 = new Room(14, RoomStatus.VAGUE, RoomType.STUDIO, BigDecimal.valueOf(160));
        roomRepo.saveAll(Arrays.asList(r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11, r12, r13, r14));


        Bed b1 = new Bed(BedStatus.VAGUE, r1);
        Bed b2 = new Bed(BedStatus.VAGUE, r2);
        Bed b3 = new Bed(BedStatus.VAGUE, r2);
        Bed b4 = new Bed(BedStatus.VAGUE, r3);
        Bed b5 = new Bed(BedStatus.VAGUE, r4);
        Bed b6 = new Bed(BedStatus.VAGUE, r5);
        Bed b7 = new Bed(BedStatus.VAGUE, r6);
        Bed b8 = new Bed(BedStatus.VAGUE, r6);
        Bed b9 = new Bed(BedStatus.VAGUE, r7);
        Bed b10 = new Bed(BedStatus.VAGUE, r7);
        Bed b11 = new Bed(BedStatus.VAGUE, r8);
        Bed b12 = new Bed(BedStatus.VAGUE, r8);
        bedRepo.saveAll(Arrays.asList(b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12));

        r1.getBeds().add(b1);
        r2.getBeds().addAll(Arrays.asList(b2, b3));
        r3.getBeds().add(b4);
        r4.getBeds().add(b5);
        r5.getBeds().add(b6);
        r6.getBeds().addAll(Arrays.asList(b7, b8));
        r7.getBeds().addAll(Arrays.asList(b9, b10));
        r8.getBeds().addAll(Arrays.asList(b11, b12));
        roomRepo.saveAll(Arrays.asList(r1, r2, r3, r4, r5, r6, r7, r8));

        // Reserve re1 = new Reserve();
        // re1.getGuest().add(i1);
        // re1.getReservedDays().add(LocalDate.now().plusDays(6));
        // re1.getReservedDays().add(LocalDate.now().plusDays(7));
        // re1.getCheckIn().add(LocalDateTime.now());
        // re1.getRooms().add(r1);
        // re1.setReserveStatus(ReserveStatus.CONFIRMED);
        // re1.setInitialValue(r1.getPrice());
        // reserveRepo.save(re1);

        // i1.getReservation().add(re1);
        // idRepo.save(i1);

        // r1.setRoomStatus(RoomStatus.OCCUPIED);
        // roomRepo.save(r1);
    }
}