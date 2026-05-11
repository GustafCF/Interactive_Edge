package com.br.elohostel.config;

import java.math.BigDecimal;
import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.br.elohostel.model.Bed;
import com.br.elohostel.model.Guest;
import com.br.elohostel.model.Role;
import com.br.elohostel.model.Room;
import com.br.elohostel.model.Tenant;
import com.br.elohostel.model.User;
import com.br.elohostel.model.enums.BedStatus;
import com.br.elohostel.model.enums.RoleStatus;
import com.br.elohostel.model.enums.RoomStatus;
import com.br.elohostel.model.enums.RoomType;
import com.br.elohostel.repository.BedRepository;
import com.br.elohostel.repository.GuestRepository;
import com.br.elohostel.repository.ReserveRepository;
import com.br.elohostel.repository.RoleRepository;
import com.br.elohostel.repository.RoomRepository;
import com.br.elohostel.repository.TenantRepository;
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
    private final TenantRepository tenantRepo;
    
    public TestConfig(GuestRepository idRepo, RoomRepository roomRepo, UserRepository userRepo, BCryptPasswordEncoder passwordEncode, ReserveRepository reserveRepo, BedRepository bedRepo, RoleRepository roleRepo, TenantRepository tenantRepo) {
        this.idRepo = idRepo;
        this.roomRepo = roomRepo;
        this.userRepo = userRepo;
        this.passwordEncode = passwordEncode;
        this.reserveRepo = reserveRepo;
        this.bedRepo = bedRepo;
        this.roleRepo = roleRepo;
        this.tenantRepo = tenantRepo;
    }

    @Override
    public void run(String... args) throws Exception {

        // roleRepo.deleteAll();
        // userRepo.deleteAll();

        // Tenant t1 = new Tenant("elo_key", "elohostel");
        // Tenant t2 = new Tenant("gu_key", "gu_hostel");

        // tenantRepo.saveAll(Arrays.asList(t1, t2));

        // Role rl1 = new Role(RoleStatus.ADMIN.name(), "Administrador da API" , RoleStatus.ADMIN);
        // Role rl2 = new Role(RoleStatus.BASIC.name(), "Basic user", RoleStatus.BASIC);

        // User u1 = new User("gcf", "g.cesarfranco7@gmail.com", passwordEncode.encode("123456"), "12345678");
        // User u2 = new User("veve", "veve@gmail.com", passwordEncode.encode("123"), "12345678");
        // u1.getRoles().add(rl1);
        // u2.getRoles().add(rl2);

        // u1.setTenant(t1);
        // u2.setTenant(t2);

        // userRepo.saveAll(Arrays.asList(u1, u2));
        
        // Guest i1 = new Guest("Gustavo", "04695595192", "61-9999-9999", "gus@email.com");
        // Guest i2 = new Guest("Verônica", "12345678900", "61-9999-9999", "veve@email.com");
        // Guest i3 = new Guest("Eloina", "33344455511", "61-9999-9999", "eloina@email.com");
        // Guest i4 = new Guest("Deusimar", "33344455511", "61-9999-9999", "masim@email.com");
        // Guest i5 = new Guest("Eloita", "33344455511", "61-9999-9999", "eloita@email.com");
        // i1.setTenant(t1);
        // i2.setTenant(t1);
        // i3.setTenant(t1);
        // i4.setTenant(t2);
        // i5.setTenant(t2);

        // idRepo.saveAll(Arrays.asList(i1, i2, i3, i4, i5));


       
        // Room r1 = new Room( 1, RoomStatus.VAGUE, RoomType.SUITE, BigDecimal.valueOf(150.0));
        // Room r2 = new Room( 2, RoomStatus.VAGUE, RoomType.ROOM_SHARED_BATHROOM, BigDecimal.valueOf(99.0));
        // Room r3 = new Room( 3, RoomStatus.VAGUE, RoomType.ROOM_SHARED_BATHROOM, BigDecimal.valueOf(99.0));
        // Room r4 = new Room( 4, RoomStatus.VAGUE, RoomType.SUITE, BigDecimal.valueOf(150.0)); 
        // Room r5 = new Room( 5, RoomStatus.VAGUE, RoomType.SHARED, BigDecimal.valueOf(59.0));
        // Room r6 = new Room(6, RoomStatus.VAGUE, RoomType.SHARED, BigDecimal.valueOf(180.0));  
        // Room r7 = new Room(7, RoomStatus.VAGUE, RoomType.SUITE, BigDecimal.valueOf(110.0));
        // Room r8 = new Room(8, RoomStatus.VAGUE, RoomType.ROOM_SHARED_BATHROOM, BigDecimal.valueOf(110.0));
        // Room r9 = new Room(9, RoomStatus.VAGUE, RoomType.ROOM_SHARED_BATHROOM, BigDecimal.valueOf(110.0));
        // Room r10 = new Room(10, RoomStatus.VAGUE, RoomType.ROOM_SHARED_BATHROOM, BigDecimal.valueOf(110.0));
        // Room r11 = new Room(11, RoomStatus.VAGUE, RoomType.ROOM_SHARED_BATHROOM, BigDecimal.valueOf(79.0));
        // Room r12 = new Room(12, RoomStatus.VAGUE, RoomType.ROOM_SHARED_BATHROOM, BigDecimal.valueOf(89.0));
        // Room r13 = new Room(13, RoomStatus.VAGUE, RoomType.STUDIO, BigDecimal.valueOf(160));
        // Room r14 = new Room(14, RoomStatus.VAGUE, RoomType.STUDIO, BigDecimal.valueOf(160));
        // r1.setTenant(t1);
        // r2.setTenant(t1);
        // r3.setTenant(t1);
        // r4.setTenant(t1);
        // r5.setTenant(t1);
        // r6.setTenant(t2);
        // r7.setTenant(t2);
        // r8.setTenant(t2);
        // r9.setTenant(t2);
        // r10.setTenant(t2);
        // r11.setTenant(t2);
        // r12.setTenant(t2);
        // r13.setTenant(t2);
        // r14.setTenant(t2);

        // roomRepo.saveAll(Arrays.asList(r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11, r12, r13, r14));


        // Bed b1 = new Bed(BedStatus.VAGUE, 1, r1);
        // Bed b2 = new Bed(BedStatus.VAGUE, 2, r2);
        // Bed b3 = new Bed(BedStatus.VAGUE, 3, r2);
        // Bed b4 = new Bed(BedStatus.VAGUE, 4, r3);
        // Bed b5 = new Bed(BedStatus.VAGUE, 5, r4);
        // Bed b6 = new Bed(BedStatus.VAGUE, 6, r5);
        // Bed b7 = new Bed(BedStatus.VAGUE, 7, r6);
        // Bed b8 = new Bed(BedStatus.VAGUE, 8, r6);
        // Bed b9 = new Bed(BedStatus.VAGUE, 9, r7);
        // Bed b10 = new Bed(BedStatus.VAGUE, 10, r7);
        // Bed b11 = new Bed(BedStatus.VAGUE, 11, r8);
        // Bed b12 = new Bed(BedStatus.VAGUE, 12, r8);

        // b1.setTenant(t1);
        // b2.setTenant(t1);
        // b3.setTenant(t1);
        // b4.setTenant(t1);
        // b5.setTenant(t1);
        // b6.setTenant(t2);
        // b7.setTenant(t2);
        // b8.setTenant(t2);
        // b9.setTenant(t2);
        // b10.setTenant(t2);
        // b11.setTenant(t2);
        // b12.setTenant(t2);

        // bedRepo.saveAll(Arrays.asList(b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11, b12));

        // r1.getBeds().add(b1);
        // r2.getBeds().addAll(Arrays.asList(b2, b3));
        // r3.getBeds().add(b4);
        // r4.getBeds().add(b5);
        // r5.getBeds().add(b6);
        // r6.getBeds().addAll(Arrays.asList(b7, b8));
        // r7.getBeds().addAll(Arrays.asList(b9, b10));
        // r8.getBeds().addAll(Arrays.asList(b11, b12));
        // roomRepo.saveAll(Arrays.asList(r1, r2, r3, r4, r5, r6, r7, r8));

        // Plan planoTeste = new Plan();
        // planoTeste.setName("Plano Teste Mensal");
        // planoTeste.setDescription("Plano de assinatura para testes");
        // planoTeste.setPrice(BigDecimal.valueOf(99.90));
        // planoTeste.setCurrency("BRL");
        // planoTeste.setFrequency(1);
        // planoTeste.setFrequencyType("months");
        // planoTeste.setMpPlanId(null); 
        // planoTeste.setTenant(t1);
        // planRepo.save(planoTeste);

    }
}