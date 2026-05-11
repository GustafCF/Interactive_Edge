package com.br.elohostel.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.br.elohostel.model.dtos.LoginRequestDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "TB_USERS")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(unique = true)
    private String email;
    private String password;
    private String phone;
    
    @Column(length = 100)
    private String establishmentName;
    
    @Column(length = 255)
    private String establishmentAddress;
    
    @Column(length = 20)
    private String establishmentPhone;
    
    @Column(length = 100)
    private String establishmentResponsible;
    
    @Column(length = 500)
    private String establishmentLogo;
    
    @Column(length = 500)
    private String establishmentWelcomeMessage;

    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(
        name = "TB_USERS_ROLES",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @JsonIgnore
    @OneToOne(mappedBy = "user")
    private PasswordResetToken passwordResetToken;
    
    public User() {}

    public User(String name, String email, String password, String phone) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
    }

    // Getters e Setters existentes...
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public PasswordResetToken getPasswordResetToken() {
        return passwordResetToken;
    }

    public void setPasswordResetToken(PasswordResetToken passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }
    
    // NOVOS GETTERS E SETTERS
    public String getEstablishmentName() {
        return establishmentName;
    }

    public void setEstablishmentName(String establishmentName) {
        this.establishmentName = establishmentName;
    }

    public String getEstablishmentAddress() {
        return establishmentAddress;
    }

    public void setEstablishmentAddress(String establishmentAddress) {
        this.establishmentAddress = establishmentAddress;
    }

    public String getEstablishmentPhone() {
        return establishmentPhone;
    }

    public void setEstablishmentPhone(String establishmentPhone) {
        this.establishmentPhone = establishmentPhone;
    }

    public String getEstablishmentResponsible() {
        return establishmentResponsible;
    }

    public void setEstablishmentResponsible(String establishmentResponsible) {
        this.establishmentResponsible = establishmentResponsible;
    }

    public String getEstablishmentLogo() {
        return establishmentLogo;
    }

    public void setEstablishmentLogo(String establishmentLogo) {
        this.establishmentLogo = establishmentLogo;
    }

    public String getEstablishmentWelcomeMessage() {
        return establishmentWelcomeMessage;
    }

    public void setEstablishmentWelcomeMessage(String establishmentWelcomeMessage) {
        this.establishmentWelcomeMessage = establishmentWelcomeMessage;
    }

    public boolean loginValidation(LoginRequestDTO loginRequestDTO, BCryptPasswordEncoder bCryptPasswordEncoder) {
        return bCryptPasswordEncoder.matches(loginRequestDTO.password(), this.password);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        User other = (User) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    } 
}