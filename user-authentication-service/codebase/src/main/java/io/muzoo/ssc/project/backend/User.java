package io.muzoo.ssc.project.backend;

import lombok.Data;
import jakarta.persistence.*;

@Entity
@Data
@Table(name = "tbl_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true)
    private String username;

    private String password;

    private String role;
}
